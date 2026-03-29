package com.watch.music163;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MoreActivity extends AppCompatActivity {

    private EditText etSearch;
    private ListView lvSongs;
    private TextView tabSearch;
    private TextView tabFavorites;
    private ArrayAdapter<Song> adapter;
    private final List<Song> displayList = new ArrayList<>();
    private boolean showingFavorites = false;
    private FavoritesManager favoritesManager;
    private MusicPlayerManager playerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);

        etSearch = findViewById(R.id.et_search);
        lvSongs = findViewById(R.id.lv_songs);
        tabSearch = findViewById(R.id.tab_search);
        tabFavorites = findViewById(R.id.tab_favorites);
        TextView btnSearch = findViewById(R.id.btn_search);
        TextView tabSettings = findViewById(R.id.tab_settings);

        favoritesManager = new FavoritesManager(this);
        playerManager = MusicPlayerManager.getInstance();

        adapter = new ArrayAdapter<Song>(this, R.layout.item_song, R.id.tv_item_name, displayList) {
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
        lvSongs.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> doSearch());

        tabSearch.setOnClickListener(v -> switchToSearch());
        tabFavorites.setOnClickListener(v -> switchToFavorites());
        tabSettings.setOnClickListener(v ->
                startActivity(new Intent(MoreActivity.this, SettingsActivity.class)));

        lvSongs.setOnItemClickListener((parent, view, position, id) -> {
            Song song = displayList.get(position);
            List<Song> playlist = new ArrayList<>(displayList);
            playerManager.setPlaylist(playlist, position);
            playerManager.playCurrent();
            finish();
        });
    }

    private void doSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) return;

        switchToSearch();
        String cookie = playerManager.getCookie();
        MusicApiHelper.searchSongs(keyword, cookie, new MusicApiHelper.SearchCallback() {
            @Override
            public void onResult(List<Song> songs) {
                displayList.clear();
                displayList.addAll(songs);
                adapter.notifyDataSetChanged();
                if (songs.isEmpty()) {
                    Toast.makeText(MoreActivity.this, R.string.no_song, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MoreActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchToSearch() {
        showingFavorites = false;
        tabSearch.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        tabSearch.setTextColor(getResources().getColor(R.color.white));
        tabFavorites.setBackgroundColor(getResources().getColor(R.color.bg_dark));
        tabFavorites.setTextColor(getResources().getColor(R.color.gray_text));
    }

    private void switchToFavorites() {
        showingFavorites = true;
        tabFavorites.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        tabFavorites.setTextColor(getResources().getColor(R.color.white));
        tabSearch.setBackgroundColor(getResources().getColor(R.color.bg_dark));
        tabSearch.setTextColor(getResources().getColor(R.color.gray_text));

        displayList.clear();
        displayList.addAll(favoritesManager.getFavorites());
        adapter.notifyDataSetChanged();
    }
}
