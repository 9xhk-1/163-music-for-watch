package com.qinghe.music163pro.activity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.qinghe.music163pro.R;
import com.qinghe.music163pro.manager.DownloadManager;
import com.qinghe.music163pro.player.MusicPlayerManager;
import com.qinghe.music163pro.model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Download list activity - shows all downloaded songs from /sdcard/163Music/Download/
 */
public class DownloadListActivity extends AppCompatActivity {

    private final List<File> downloadedFiles = new ArrayList<>();
    private ArrayAdapter<File> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);

        ListView lvDownloads = findViewById(R.id.lv_downloads);
        TextView tvEmpty = findViewById(R.id.tv_empty);

        adapter = new ArrayAdapter<File>(this, R.layout.item_song, R.id.tv_item_name, downloadedFiles) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                File file = getItem(position);
                if (file != null) {
                    TextView tvName = view.findViewById(R.id.tv_item_name);
                    TextView tvArtist = view.findViewById(R.id.tv_item_artist);
                    String name = file.getName();
                    // Remove .mp3 extension
                    if (name.endsWith(".mp3")) {
                        name = name.substring(0, name.length() - 4);
                    }
                    // Split by " - " to get song name and artist
                    int dashIdx = name.indexOf(" - ");
                    if (dashIdx > 0) {
                        tvName.setText(name.substring(0, dashIdx));
                        tvArtist.setText(name.substring(dashIdx + 3));
                    } else {
                        tvName.setText(name);
                        tvArtist.setText("");
                    }
                }
                return view;
            }
        };
        lvDownloads.setAdapter(adapter);

        lvDownloads.setOnItemClickListener((parent, view, position, id) -> {
            File file = downloadedFiles.get(position);
            playLocalFile(file);
        });

        loadDownloads();

        if (downloadedFiles.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvDownloads.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvDownloads.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDownloads();
    }

    private void loadDownloads() {
        downloadedFiles.clear();
        downloadedFiles.addAll(DownloadManager.getDownloadedFiles());
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void playLocalFile(File file) {
        try {
            String name = file.getName();
            if (name.endsWith(".mp3")) {
                name = name.substring(0, name.length() - 4);
            }
            String songName = name;
            String artist = "";
            int dashIdx = name.indexOf(" - ");
            if (dashIdx > 0) {
                songName = name.substring(0, dashIdx);
                artist = name.substring(dashIdx + 3);
            }

            Song song = new Song(0, songName, artist, "");
            song.setUrl(file.getAbsolutePath());

            List<Song> playlist = new ArrayList<>();
            playlist.add(song);

            MusicPlayerManager playerManager = MusicPlayerManager.getInstance();
            playerManager.setPlaylist(playlist, 0);
            playerManager.playCurrent();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "播放失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
