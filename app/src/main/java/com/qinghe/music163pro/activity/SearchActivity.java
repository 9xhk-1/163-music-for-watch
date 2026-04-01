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
import com.qinghe.music163pro.model.Song;
import com.qinghe.music163pro.player.MusicPlayerManager;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Search activity - search songs and play from results.
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
    private ListView lvHistory;
    private ArrayAdapter<Song> adapter;
    private ArrayAdapter<String> historyAdapter;
    private final List<Song> displayList = new ArrayList<>();
    private final List<String> historyList = new ArrayList<>();
    private MusicPlayerManager playerManager;
    private SharedPreferences prefs;

    private String currentKeyword = "";
    private int currentOffset = 0;
    private boolean isLoadingMore = false;
    private boolean hasMoreResults = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Apply keep screen on setting
        SharedPreferences screenPrefs = getSharedPreferences("music163_settings", MODE_PRIVATE);
        if (screenPrefs.getBoolean("keep_screen_on", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        etSearch = findViewById(R.id.et_search);
        lvSongs = findViewById(R.id.lv_songs);
        lvHistory = findViewById(R.id.lv_history);
        TextView btnSearch = findViewById(R.id.btn_search);

        playerManager = MusicPlayerManager.getInstance();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

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

        // Set up search history
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
            showConfirmDialog("确认删除", "确定删除搜索记录「" + keyword + "」？", () -> {
                historyList.remove(position);
                saveSearchHistory();
                historyAdapter.notifyDataSetChanged();
                updateHistoryVisibility();
            });
            return true;
        });

        btnSearch.setOnClickListener(v -> doSearch());

        lvSongs.setOnItemClickListener((parent, view, position, id) -> {
            Song song = displayList.get(position);
            List<Song> playlist = new ArrayList<>(displayList);
            playerManager.setPlaylist(playlist, position);
            playerManager.playCurrent();
            // Navigate back to MainActivity (player screen)
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Infinite scroll: load more when reaching bottom
        lvSongs.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {}

            @Override
            public void onScroll(android.widget.AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount
                        && !isLoadingMore && hasMoreResults) {
                    loadMore();
                }
            }
        });
    }

    private void doSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) return;

        currentKeyword = keyword;
        currentOffset = 0;
        hasMoreResults = true;

        addToSearchHistory(keyword);
        String cookie = playerManager.getCookie();
        MusicApiHelper.searchSongs(keyword, 0, cookie, new MusicApiHelper.SearchCallback() {
            @Override
            public void onResult(List<Song> songs) {
                displayList.clear();
                displayList.addAll(songs);
                adapter.notifyDataSetChanged();
                lvSongs.setVisibility(View.VISIBLE);
                lvHistory.setVisibility(View.GONE);
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

    private void loadMore() {
        if (currentKeyword.isEmpty() || isLoadingMore || !hasMoreResults) return;
        isLoadingMore = true;

        String cookie = playerManager.getCookie();
        MusicApiHelper.searchSongs(currentKeyword, currentOffset, cookie, new MusicApiHelper.SearchCallback() {
            @Override
            public void onResult(List<Song> songs) {
                isLoadingMore = false;
                if (songs.isEmpty()) {
                    hasMoreResults = false;
                    return;
                }
                displayList.addAll(songs);
                adapter.notifyDataSetChanged();
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
        if (displayList.isEmpty() && !historyList.isEmpty()) {
            lvHistory.setVisibility(View.VISIBLE);
            lvSongs.setVisibility(View.GONE);
        } else {
            lvHistory.setVisibility(View.GONE);
            lvSongs.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Show a confirmation dialog adapted for watch (360x320 px screen).
     * Uses fixed pixel values for consistent sizing on watch displays.
     */
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

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(18));
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, px(6));
        dialog.addView(tvTitle);

        // Message
        TextView tvMessage = new TextView(this);
        tvMessage.setText(message);
        tvMessage.setTextColor(0xFFCCCCCC);
        tvMessage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(15));
        tvMessage.setGravity(Gravity.CENTER);
        tvMessage.setPadding(0, 0, 0, px(12));
        dialog.addView(tvMessage);

        // Buttons row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);
        dialog.addView(btnRow);

        // Cancel button
        TextView btnCancel = new TextView(this);
        btnCancel.setText("取消");
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

        // Confirm button
        TextView btnConfirm = new TextView(this);
        btnConfirm.setText("确定");
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

    /**
     * Convert a value scaled for a 320px-wide watch screen to actual pixels.
     * Base reference: 320px width. Values are proportionally scaled.
     */
    private int px(int baseValue) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return (int) (baseValue * screenWidth / 320f + 0.5f);
    }
}
