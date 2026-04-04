package com.qinghe.music163pro.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.qinghe.music163pro.R;
import com.qinghe.music163pro.api.MusicApiHelper;
import com.qinghe.music163pro.model.PlaylistInfo;
import com.qinghe.music163pro.model.Song;
import com.qinghe.music163pro.player.MusicPlayerManager;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Search activity - search songs and playlists.
 * Supports tab switching between 单曲 (songs) and 歌单 (playlists).
 * Long press search history to delete with confirmation dialog.
 * Supports infinite scrolling: loads next 20 results when reaching bottom.
 */
public class SearchActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "music163_settings";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    private static final int MAX_HISTORY = 20;
    private static final int PAGE_SIZE = 20;

    private EditText etSearch;
    private ListView lvSongs;
    private ListView lvPlaylists;
    private ListView lvHistory;
    private LinearLayout llSearchTabs;
    private TextView tabSongs;
    private TextView tabPlaylists;
    private ArrayAdapter<Song> songAdapter;
    private ArrayAdapter<PlaylistInfo> playlistAdapter;
    private ArrayAdapter<String> historyAdapter;
    private final List<Song> songList = new ArrayList<>();
    private final List<PlaylistInfo> playlistList = new ArrayList<>();
    private final List<String> historyList = new ArrayList<>();
    private MusicPlayerManager playerManager;
    private SharedPreferences prefs;

    private String currentKeyword = "";
    private int currentOffset = 0;
    private boolean isLoadingMore = false;
    private boolean hasMoreResults = true;

    private int playlistOffset = 0;
    private boolean isLoadingMorePlaylists = false;
    private boolean hasMorePlaylists = true;

    private boolean isSongTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        SharedPreferences screenPrefs = getSharedPreferences("music163_settings", MODE_PRIVATE);
        if (screenPrefs.getBoolean("keep_screen_on", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        etSearch = findViewById(R.id.et_search);
        lvSongs = findViewById(R.id.lv_songs);
        lvPlaylists = findViewById(R.id.lv_playlists);
        lvHistory = findViewById(R.id.lv_history);
        llSearchTabs = findViewById(R.id.ll_search_tabs);
        tabSongs = findViewById(R.id.tab_songs);
        tabPlaylists = findViewById(R.id.tab_playlists);
        TextView btnSearch = findViewById(R.id.btn_search);

        playerManager = MusicPlayerManager.getInstance();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        songAdapter = new ArrayAdapter<Song>(this, R.layout.item_song, R.id.tv_item_name, songList) {
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
        lvSongs.setAdapter(songAdapter);

        playlistAdapter = new ArrayAdapter<PlaylistInfo>(this, R.layout.item_playlist, R.id.tv_playlist_name, playlistList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                PlaylistInfo pl = getItem(position);
                if (pl != null) {
                    TextView tvName = view.findViewById(R.id.tv_playlist_name);
                    TextView tvInfo = view.findViewById(R.id.tv_playlist_info);
                    tvName.setText(pl.getName());
                    String info = pl.getTrackCount() + "\u9996";
                    if (pl.getCreator() != null && !pl.getCreator().isEmpty()) {
                        info += " \u00b7 " + pl.getCreator();
                    }
                    tvInfo.setText(info);
                }
                return view;
            }
        };
        lvPlaylists.setAdapter(playlistAdapter);

        loadSearchHistory();
        historyAdapter = new ArrayAdapter<>(this, R.layout.item_history, R.id.tv_history_text, historyList);
        lvHistory.setAdapter(historyAdapter);
        updateHistoryVisibility();

        lvHistory.setOnItemClickListener((parent, view, position, id) -> {
            String keyword = historyList.get(position);
            etSearch.setText(keyword);
            etSearch.setSelection(keyword.length());
            doSearch();
        });

        lvHistory.setOnItemLongClickListener((parent, view, position, id) -> {
            String keyword = historyList.get(position);
            showConfirmDialog("\u786e\u8ba4\u5220\u9664", "\u786e\u5b9a\u5220\u9664\u641c\u7d22\u8bb0\u5f55\u300c" + keyword + "\u300d\uff1f", () -> {
                historyList.remove(position);
                saveSearchHistory();
                historyAdapter.notifyDataSetChanged();
                updateHistoryVisibility();
            });
            return true;
        });

        btnSearch.setOnClickListener(v -> doSearch());
        tabSongs.setOnClickListener(v -> switchToSongTab());
        tabPlaylists.setOnClickListener(v -> switchToPlaylistTab());

        lvSongs.setOnItemClickListener((parent, view, position, id) -> {
            Song song = songList.get(position);
            List<Song> playlist = new ArrayList<>(songList);
            playerManager.setPlaylist(playlist, position);
            playerManager.playCurrent();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        lvPlaylists.setOnItemClickListener((parent, view, position, id) -> {
            PlaylistInfo pl = playlistList.get(position);
            Intent intent = new Intent(this, PlaylistDetailActivity.class);
            intent.putExtra("playlist_id", pl.getId());
            intent.putExtra("playlist_name", pl.getName());
            intent.putExtra("track_count", pl.getTrackCount());
            intent.putExtra("creator", pl.getCreator());
            intent.putExtra("creator_user_id", pl.getUserId());
            intent.putExtra("is_liked_playlist", pl.isLikedPlaylist());
            startActivity(intent);
        });

        lvSongs.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {}
            @Override
            public void onScroll(android.widget.AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount
                        && !isLoadingMore && hasMoreResults && isSongTab) {
                    loadMoreSongs();
                }
            }
        });

        lvPlaylists.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {}
            @Override
            public void onScroll(android.widget.AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount
                        && !isLoadingMorePlaylists && hasMorePlaylists && !isSongTab) {
                    loadMorePlaylists();
                }
            }
        });
    }

    private void switchToSongTab() {
        isSongTab = true;
        tabSongs.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        tabSongs.setTextColor(0xFFFFFFFF);
        tabPlaylists.setBackgroundColor(0xFF424242);
        tabPlaylists.setTextColor(0xFFAAAAAA);
        lvSongs.setVisibility(songList.isEmpty() ? View.GONE : View.VISIBLE);
        lvPlaylists.setVisibility(View.GONE);
        if (!currentKeyword.isEmpty() && songList.isEmpty()) {
            doSearchSongs();
        }
    }

    private void switchToPlaylistTab() {
        isSongTab = false;
        tabPlaylists.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        tabPlaylists.setTextColor(0xFFFFFFFF);
        tabSongs.setBackgroundColor(0xFF424242);
        tabSongs.setTextColor(0xFFAAAAAA);
        lvSongs.setVisibility(View.GONE);
        lvPlaylists.setVisibility(playlistList.isEmpty() ? View.GONE : View.VISIBLE);
        if (!currentKeyword.isEmpty() && playlistList.isEmpty()) {
            doSearchPlaylists();
        }
    }

    private void doSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) return;

        currentKeyword = keyword;
        addToSearchHistory(keyword);

        llSearchTabs.setVisibility(View.VISIBLE);
        lvHistory.setVisibility(View.GONE);

        songList.clear();
        songAdapter.notifyDataSetChanged();
        playlistList.clear();
        playlistAdapter.notifyDataSetChanged();

        currentOffset = 0;
        hasMoreResults = true;
        playlistOffset = 0;
        hasMorePlaylists = true;

        if (isSongTab) {
            doSearchSongs();
        } else {
            doSearchPlaylists();
        }
    }

    private void doSearchSongs() {
        currentOffset = 0;
        hasMoreResults = true;
        String cookie = playerManager.getCookie();
        MusicApiHelper.searchSongs(currentKeyword, 0, cookie, new MusicApiHelper.SearchCallback() {
            @Override
            public void onResult(List<Song> songs) {
                songList.clear();
                songList.addAll(songs);
                songAdapter.notifyDataSetChanged();
                lvSongs.setVisibility(View.VISIBLE);
                lvPlaylists.setVisibility(View.GONE);
                currentOffset = songs.size();
                hasMoreResults = songs.size() >= PAGE_SIZE;
                if (songs.isEmpty()) {
                    Toast.makeText(SearchActivity.this, R.string.no_song, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onError(String message) {
                Toast.makeText(SearchActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doSearchPlaylists() {
        playlistOffset = 0;
        hasMorePlaylists = true;
        String cookie = playerManager.getCookie();
        MusicApiHelper.searchPlaylists(currentKeyword, 0, cookie, new MusicApiHelper.SearchPlaylistCallback() {
            @Override
            public void onResult(List<PlaylistInfo> playlists) {
                playlistList.clear();
                playlistList.addAll(playlists);
                playlistAdapter.notifyDataSetChanged();
                lvPlaylists.setVisibility(View.VISIBLE);
                lvSongs.setVisibility(View.GONE);
                playlistOffset = playlists.size();
                hasMorePlaylists = playlists.size() >= PAGE_SIZE;
                if (playlists.isEmpty()) {
                    Toast.makeText(SearchActivity.this, "\u6682\u65e0\u6b4c\u5355", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onError(String message) {
                Toast.makeText(SearchActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMoreSongs() {
        if (currentKeyword.isEmpty() || isLoadingMore || !hasMoreResults) return;
        isLoadingMore = true;
        String cookie = playerManager.getCookie();
        MusicApiHelper.searchSongs(currentKeyword, currentOffset, cookie, new MusicApiHelper.SearchCallback() {
            @Override
            public void onResult(List<Song> songs) {
                isLoadingMore = false;
                if (songs.isEmpty()) { hasMoreResults = false; return; }
                songList.addAll(songs);
                songAdapter.notifyDataSetChanged();
                currentOffset += songs.size();
                hasMoreResults = songs.size() >= PAGE_SIZE;
            }
            @Override
            public void onError(String message) {
                isLoadingMore = false;
                Toast.makeText(SearchActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMorePlaylists() {
        if (currentKeyword.isEmpty() || isLoadingMorePlaylists || !hasMorePlaylists) return;
        isLoadingMorePlaylists = true;
        String cookie = playerManager.getCookie();
        MusicApiHelper.searchPlaylists(currentKeyword, playlistOffset, cookie, new MusicApiHelper.SearchPlaylistCallback() {
            @Override
            public void onResult(List<PlaylistInfo> playlists) {
                isLoadingMorePlaylists = false;
                if (playlists.isEmpty()) { hasMorePlaylists = false; return; }
                playlistList.addAll(playlists);
                playlistAdapter.notifyDataSetChanged();
                playlistOffset += playlists.size();
                hasMorePlaylists = playlists.size() >= PAGE_SIZE;
            }
            @Override
            public void onError(String message) {
                isLoadingMorePlaylists = false;
                Toast.makeText(SearchActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSearchHistory() {
        historyList.clear();
        String json = prefs.getString(KEY_SEARCH_HISTORY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                historyList.add(arr.getString(i));
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void saveSearchHistory() {
        JSONArray arr = new JSONArray();
        for (String s : historyList) {
            arr.put(s);
        }
        prefs.edit().putString(KEY_SEARCH_HISTORY, arr.toString()).apply();
    }

    private void addToSearchHistory(String keyword) {
        historyList.remove(keyword);
        historyList.add(0, keyword);
        if (historyList.size() > MAX_HISTORY) {
            historyList.remove(historyList.size() - 1);
        }
        saveSearchHistory();
        historyAdapter.notifyDataSetChanged();
    }

    private void updateHistoryVisibility() {
        boolean noSearchResults = songList.isEmpty() && playlistList.isEmpty();
        if (noSearchResults && !historyList.isEmpty() && currentKeyword.isEmpty()) {
            lvHistory.setVisibility(View.VISIBLE);
            lvSongs.setVisibility(View.GONE);
            lvPlaylists.setVisibility(View.GONE);
            llSearchTabs.setVisibility(View.GONE);
        } else {
            lvHistory.setVisibility(View.GONE);
        }
    }

    private void showConfirmDialog(String title, String message, Runnable onConfirm) {
        FrameLayout rootView = findViewById(android.R.id.content);

        FrameLayout overlay = new FrameLayout(this);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(0xCC333333);

        LinearLayout dialog = new LinearLayout(this);
        dialog.setOrientation(LinearLayout.VERTICAL);
        dialog.setBackgroundColor(0xFF424242);
        dialog.setPadding(px(16), px(12), px(16), px(12));
        FrameLayout.LayoutParams dlgParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        dlgParams.gravity = Gravity.CENTER;
        dlgParams.leftMargin = px(16);
        dlgParams.rightMargin = px(16);
        dialog.setLayoutParams(dlgParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(18));
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, px(6));
        dialog.addView(tvTitle);

        TextView tvMessage = new TextView(this);
        tvMessage.setText(message);
        tvMessage.setTextColor(0xFFCCCCCC);
        tvMessage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(15));
        tvMessage.setGravity(Gravity.CENTER);
        tvMessage.setPadding(0, 0, 0, px(12));
        dialog.addView(tvMessage);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);
        dialog.addView(btnRow);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("\u53d6\u6d88");
        btnCancel.setTextColor(0xFFFFFFFF);
        btnCancel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(16));
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setPadding(px(12), px(8), px(12), px(8));
        btnCancel.setBackgroundColor(0xFF616161);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        cancelParams.rightMargin = px(4);
        btnCancel.setLayoutParams(cancelParams);
        btnCancel.setClickable(true);
        btnCancel.setFocusable(true);
        btnCancel.setOnClickListener(v -> rootView.removeView(overlay));
        btnRow.addView(btnCancel);

        TextView btnConfirm = new TextView(this);
        btnConfirm.setText("\u786e\u5b9a");
        btnConfirm.setTextColor(0xFFFFFFFF);
        btnConfirm.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(16));
        btnConfirm.setGravity(Gravity.CENTER);
        btnConfirm.setPadding(px(12), px(8), px(12), px(8));
        btnConfirm.setBackgroundColor(0xFFD32F2F);
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        confirmParams.leftMargin = px(4);
        btnConfirm.setLayoutParams(confirmParams);
        btnConfirm.setClickable(true);
        btnConfirm.setFocusable(true);
        btnConfirm.setOnClickListener(v -> {
            rootView.removeView(overlay);
            onConfirm.run();
        });
        btnRow.addView(btnConfirm);

        overlay.addView(dialog);
        overlay.setOnClickListener(v -> rootView.removeView(overlay));
        dialog.setOnClickListener(v -> { /* consume click */ });
        rootView.addView(overlay);
    }

    private int px(int baseValue) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return (int) (baseValue * screenWidth / 320f + 0.5f);
    }
}
