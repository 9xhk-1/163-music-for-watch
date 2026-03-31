package com.qinghe.music163pro.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.qinghe.music163pro.R;
import com.qinghe.music163pro.api.MusicApiHelper;
import com.qinghe.music163pro.manager.FavoritesManager;
import com.qinghe.music163pro.model.Song;
import com.qinghe.music163pro.player.MusicPlayerManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Favorites list activity - shows all favorited songs.
 * Supports both local and cloud favorites modes.
 */
public class FavoritesListActivity extends AppCompatActivity {

    private final List<Song> favoritesList = new ArrayList<>();
    private ArrayAdapter<Song> adapter;
    private FavoritesManager favoritesManager;
    private MusicPlayerManager playerManager;
    private TextView tvTitle;
    private TextView tvEmpty;
    private ListView lvFavorites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites_list);

        // Apply keep screen on setting
        SharedPreferences prefs = getSharedPreferences("music163_settings", MODE_PRIVATE);
        if (prefs.getBoolean("keep_screen_on", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        lvFavorites = findViewById(R.id.lv_favorites);
        tvEmpty = findViewById(R.id.tv_empty);

        favoritesManager = new FavoritesManager(this);
        playerManager = MusicPlayerManager.getInstance();

        adapter = new ArrayAdapter<Song>(this, R.layout.item_song, R.id.tv_item_name, favoritesList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Song song = getItem(position);
                if (song != null) {
                    TextView tvName = view.findViewById(R.id.tv_item_name);
                    TextView tvArtist = view.findViewById(R.id.tv_item_artist);
                    tvName.setText(song.getName());
                    tvArtist.setText(song.getArtist());
                }
                return view;
            }
        };
        lvFavorites.setAdapter(adapter);

        lvFavorites.setOnItemClickListener((parent, view, position, id) -> {
            Song song = favoritesList.get(position);
            List<Song> playlist = new ArrayList<>(favoritesList);
            playerManager.setPlaylist(playlist, position);
            playerManager.playCurrent();
            // Navigate back to MainActivity (player screen)
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        loadFavorites();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        SharedPreferences prefs = getSharedPreferences("music163_settings", MODE_PRIVATE);
        boolean isCloud = prefs.getBoolean("fav_mode_cloud", false);

        if (isCloud) {
            loadCloudFavorites();
        } else {
            loadLocalFavorites();
        }
    }

    private void loadLocalFavorites() {
        favoritesList.clear();
        favoritesList.addAll(favoritesManager.getFavorites());
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void loadCloudFavorites() {
        favoritesList.clear();
        adapter.notifyDataSetChanged();
        tvEmpty.setText("正在加载云端收藏...");
        tvEmpty.setVisibility(View.VISIBLE);
        lvFavorites.setVisibility(View.GONE);

        String cookie = playerManager.getCookie();
        if (cookie == null || cookie.isEmpty()) {
            tvEmpty.setText("请先登录以使用云端收藏");
            return;
        }

        MusicApiHelper.getCloudFavorites(cookie, new MusicApiHelper.CloudFavoritesCallback() {
            @Override
            public void onResult(List<Song> songs) {
                favoritesList.clear();
                favoritesList.addAll(songs);
                adapter.notifyDataSetChanged();
                tvEmpty.setText("暂无云端收藏");
                updateEmptyState();
            }

            @Override
            public void onError(String message) {
                tvEmpty.setText("加载失败: " + message);
                tvEmpty.setVisibility(View.VISIBLE);
                lvFavorites.setVisibility(View.GONE);
            }
        });
    }

    private void updateEmptyState() {
        if (favoritesList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvFavorites.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvFavorites.setVisibility(View.VISIBLE);
        }
    }
}
