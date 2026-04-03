package com.qinghe.music163pro.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.qinghe.music163pro.R;
import com.qinghe.music163pro.api.MusicApiHelper;
import com.qinghe.music163pro.manager.PlaylistManager;
import com.qinghe.music163pro.model.PlaylistInfo;
import com.qinghe.music163pro.model.Song;
import com.qinghe.music163pro.player.MusicPlayerManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Playlist detail activity - shows songs in a playlist and allows playing them.
 * Supports cloud mode (subscribe/unsubscribe via API) and local mode (local save).
 * Rules:
 * - Cloud mode: "我喜欢的音乐" → no unsub/delete; my created → no unsub, can delete;
 *   others' → can unsub, no delete.
 * - Local mode: all can unsub (local remove), no delete.
 * Designed for watch screen (320x360 dpi).
 */
public class PlaylistDetailActivity extends AppCompatActivity {

    private ListView lvSongs;
    private final List<Song> displayList = new ArrayList<>();
    private ArrayAdapter<Song> adapter;
    private MusicPlayerManager playerManager;
    private PlaylistManager playlistManager;
    private TextView tvStatus;
    private TextView tvTitle;
    private TextView btnFav;
    private TextView btnDelete;
    private long playlistId;
    private String playlistName;
    private int trackCount;
    private String creator;
    private long creatorUserId;
    private boolean isLikedPlaylist;
    private long currentUserId = -1;
    private boolean isCloudMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("music163_settings", MODE_PRIVATE);
        if (prefs.getBoolean("keep_screen_on", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        isCloudMode = prefs.getBoolean("fav_mode_cloud", false);

        playlistId = getIntent().getLongExtra("playlist_id", 0);
        playlistName = getIntent().getStringExtra("playlist_name");
        trackCount = getIntent().getIntExtra("track_count", 0);
        creator = getIntent().getStringExtra("creator");
        creatorUserId = getIntent().getLongExtra("creator_user_id", 0);
        isLikedPlaylist = getIntent().getBooleanExtra("is_liked_playlist", false);

        playerManager = MusicPlayerManager.getInstance();
        playlistManager = new PlaylistManager();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF212121);
        root.setPadding(px(6), px(6), px(6), px(6));

        // Title row with heart + delete buttons on the right
        android.widget.RelativeLayout titleRow = new android.widget.RelativeLayout(this);
        titleRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Title
        tvTitle = new TextView(this);
        updateTitleText();
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(14));
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(px(28), 0, px(56), px(4));
        tvTitle.setSingleLine(true);
        tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        android.widget.RelativeLayout.LayoutParams titleParams = new android.widget.RelativeLayout.LayoutParams(
                android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
        titleParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        tvTitle.setLayoutParams(titleParams);
        titleRow.addView(tvTitle);

        // Delete button (🗑) - top right
        btnDelete = new TextView(this);
        btnDelete.setText("🗑");
        btnDelete.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(16));
        btnDelete.setGravity(Gravity.CENTER);
        btnDelete.setPadding(px(4), 0, px(2), 0);
        btnDelete.setClickable(true);
        btnDelete.setFocusable(true);
        btnDelete.setId(View.generateViewId());
        android.widget.RelativeLayout.LayoutParams delParams = new android.widget.RelativeLayout.LayoutParams(
                px(26), px(28));
        delParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
        delParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        btnDelete.setLayoutParams(delParams);
        btnDelete.setVisibility(View.GONE);
        btnDelete.setOnClickListener(v -> onDeletePlaylist());
        titleRow.addView(btnDelete);

        // Heart button (♡/♥) - left of delete
        btnFav = new TextView(this);
        btnFav.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(18));
        btnFav.setGravity(Gravity.CENTER);
        btnFav.setPadding(px(2), 0, px(4), 0);
        btnFav.setClickable(true);
        btnFav.setFocusable(true);
        android.widget.RelativeLayout.LayoutParams favParams = new android.widget.RelativeLayout.LayoutParams(
                px(26), px(28));
        favParams.addRule(android.widget.RelativeLayout.LEFT_OF, btnDelete.getId());
        favParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        btnFav.setLayoutParams(favParams);
        btnFav.setOnClickListener(v -> togglePlaylistFav());
        titleRow.addView(btnFav);

        root.addView(titleRow);

        // Creator label
        if (creator != null && !creator.isEmpty()) {
            TextView tvCreator = new TextView(this);
            tvCreator.setText("创建者: " + creator);
            tvCreator.setTextColor(0xFF888888);
            tvCreator.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(11));
            tvCreator.setGravity(Gravity.CENTER);
            tvCreator.setPadding(0, 0, 0, px(2));
            root.addView(tvCreator);
        }

        // Status text
        tvStatus = new TextView(this);
        tvStatus.setText("正在加载...");
        tvStatus.setTextColor(0xFFAAAAAA);
        tvStatus.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px(13));
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, px(4), 0, px(4));
        root.addView(tvStatus);

        lvSongs = new ListView(this);
        lvSongs.setDividerHeight(1);
        lvSongs.setDivider(getResources().getDrawable(android.R.color.transparent));
        LinearLayout.LayoutParams lvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        lvSongs.setLayoutParams(lvParams);
        lvSongs.setVisibility(View.GONE);
        root.addView(lvSongs);

        setContentView(root);

        adapter = new ArrayAdapter<Song>(this, R.layout.item_song, R.id.tv_item_name, displayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                Song song = getItem(position);
                if (song != null) {
                    TextView tvName = view.findViewById(R.id.tv_item_name);
                    TextView tvArtist = view.findViewById(R.id.tv_item_artist);
                    tvName.setText((position + 1) + ". " + song.getName());
                    tvArtist.setText(song.getArtist());
                }
                return view;
            }
        };
        lvSongs.setAdapter(adapter);

        lvSongs.setOnItemClickListener((parent, view, position, id) -> {
            List<Song> playlist = new ArrayList<>(displayList);
            playerManager.setPlaylistFromSource(playlist, position,
                    playlistId, playlistName, trackCount, creator,
                    creatorUserId, isLikedPlaylist);
            playerManager.playCurrent();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Long-press to delete song from user's created playlist (not liked playlist)
        lvSongs.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!isCloudMode || isLikedPlaylist) return false;
            if (creatorUserId <= 0 || creatorUserId != currentUserId) return false;

            Song song = displayList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("删除歌曲")
                    .setMessage("确定要从歌单中删除「" + song.getName() + "」吗？")
                    .setPositiveButton("删除", (d, w) -> {
                        String cookie = playerManager.getCookie();
                        if (cookie == null || cookie.isEmpty()) {
                            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(this, "正在删除...", Toast.LENGTH_SHORT).show();
                        MusicApiHelper.playlistTracks("del", playlistId,
                                new long[]{song.getId()}, cookie,
                                new MusicApiHelper.PlaylistActionCallback() {
                                    @Override
                                    public void onResult(boolean success) {
                                        if (success) {
                                            displayList.remove(position);
                                            adapter.notifyDataSetChanged();
                                            trackCount = displayList.size();
                                            updateTitleText();
                                            tvStatus.setText(trackCount + " 首歌曲");
                                            Toast.makeText(PlaylistDetailActivity.this,
                                                    "已删除", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(PlaylistDetailActivity.this,
                                                    "删除失败", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onError(String message) {
                                        Toast.makeText(PlaylistDetailActivity.this,
                                                "删除失败: " + message, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        // Get current user ID for cloud mode rules
        if (isCloudMode) {
            String cookie = playerManager.getCookie();
            if (cookie != null && !cookie.isEmpty()) {
                MusicApiHelper.getUid(cookie, new MusicApiHelper.AccountCallback() {
                    @Override
                    public void onResult(JSONObject json) {
                        currentUserId = json.optLong("uid", -1);
                        updateActionButtons();
                    }
                    @Override
                    public void onError(String message) {
                        updateActionButtons();
                    }
                });
            }
        }

        updateFavButton();
        updateActionButtons();

        if (playlistId > 0) {
            loadPlaylistDetail(playlistId);
        }
    }

    private void updateTitleText() {
        String titleText = playlistName != null ? playlistName : "歌单";
        if (trackCount > 0) {
            titleText += " (" + trackCount + "首)";
        }
        tvTitle.setText(titleText);
    }

    private void loadPlaylistDetail(long playlistId) {
        String cookie = playerManager.getCookie();
        MusicApiHelper.getPlaylistDetail(playlistId, cookie, new MusicApiHelper.PlaylistDetailCallback() {
            @Override
            public void onResult(List<Song> songs) {
                displayList.clear();
                displayList.addAll(songs);
                adapter.notifyDataSetChanged();
                // Update trackCount from actual loaded data
                trackCount = songs.size();
                updateTitleText();
                if (songs.isEmpty()) {
                    tvStatus.setText("暂无歌曲");
                } else {
                    tvStatus.setText(songs.size() + " 首歌曲");
                    lvSongs.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                tvStatus.setText("加载失败: " + message);
            }
        });
    }

    /**
     * Update button visibility based on mode and ownership rules:
     * Cloud mode:
     *   - "我喜欢的音乐" (specialType=5) → no unsub, no delete
     *   - My created playlist → no unsub, can delete
     *   - Others' subscribed playlist → can unsub, no delete
     * Local mode:
     *   - All playlists → can unsub (local remove), no delete
     */
    private void updateActionButtons() {
        if (isCloudMode) {
            if (isLikedPlaylist) {
                // "我喜欢的音乐" - cannot unsub or delete
                btnFav.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
            } else if (creatorUserId > 0 && creatorUserId == currentUserId) {
                // My created playlist - cannot unsub but can delete
                btnFav.setVisibility(View.GONE);
                btnDelete.setVisibility(View.VISIBLE);
            } else {
                // Others' playlist - can unsub but cannot delete
                btnFav.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.GONE);
                updateFavButton();
            }
        } else {
            // Local mode: can unsub all, no delete
            btnFav.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.GONE);
            updateFavButton();
        }
    }

    private void updateFavButton() {
        if (playlistId <= 0) {
            btnFav.setVisibility(View.GONE);
            return;
        }
        if (isCloudMode) {
            // In cloud mode, the playlist is always "subscribed" if user opens it from their list
            // Show unsubscribe button
            btnFav.setText("\u2665");
            btnFav.setTextColor(0xFFFF4444);
        } else {
            boolean saved = playlistManager.isPlaylistSaved(playlistId);
            btnFav.setText(saved ? "\u2665" : "\u2661");
            btnFav.setTextColor(saved ? 0xFFFF4444 : 0xFFAAAAAA);
        }
    }

    private void togglePlaylistFav() {
        if (playlistId <= 0) return;

        if (isCloudMode) {
            // Cloud mode: unsubscribe from cloud
            Toast.makeText(this, "正在取消收藏...", Toast.LENGTH_SHORT).show();
            String cookie = playerManager.getCookie();
            MusicApiHelper.subscribePlaylist(playlistId, false, cookie,
                    new MusicApiHelper.PlaylistActionCallback() {
                @Override
                public void onResult(boolean success) {
                    if (success) {
                        // Also remove from local storage
                        playlistManager.removePlaylist(playlistId);
                        Toast.makeText(PlaylistDetailActivity.this,
                                "已取消云端收藏", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(PlaylistDetailActivity.this,
                                "取消收藏失败", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onError(String message) {
                    Toast.makeText(PlaylistDetailActivity.this,
                            "取消收藏失败: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Local mode: toggle local save
            boolean saved = playlistManager.isPlaylistSaved(playlistId);
            if (saved) {
                playlistManager.removePlaylist(playlistId);
                Toast.makeText(this, "已取消收藏歌单", Toast.LENGTH_SHORT).show();
            } else {
                PlaylistInfo info = new PlaylistInfo(playlistId,
                        playlistName != null ? playlistName : "",
                        trackCount, creator != null ? creator : "",
                        creatorUserId, true, isLikedPlaylist ? "5" : "0");
                playlistManager.addPlaylist(info);
                Toast.makeText(this, "已收藏歌单", Toast.LENGTH_SHORT).show();
            }
            updateFavButton();
        }
    }

    private void onDeletePlaylist() {
        if (playlistId <= 0) return;
        new AlertDialog.Builder(this)
                .setTitle("删除歌单")
                .setMessage("确定要从云端删除歌单「" + (playlistName != null ? playlistName : "") + "」吗？此操作不可恢复。")
                .setPositiveButton("删除", (d, w) -> {
                    Toast.makeText(this, "正在删除...", Toast.LENGTH_SHORT).show();
                    String cookie = playerManager.getCookie();
                    MusicApiHelper.deletePlaylist(playlistId, cookie,
                            new MusicApiHelper.PlaylistActionCallback() {
                        @Override
                        public void onResult(boolean success) {
                            if (success) {
                                playlistManager.removePlaylist(playlistId);
                                Toast.makeText(PlaylistDetailActivity.this,
                                        "已删除歌单", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(PlaylistDetailActivity.this,
                                        "删除失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onError(String message) {
                            Toast.makeText(PlaylistDetailActivity.this,
                                    "删除失败: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private int px(int baseValue) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return (int) (baseValue * screenWidth / 320f + 0.5f);
    }
}
