package com.qinghe.music163pro.manager;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.qinghe.music163pro.api.MusicApiHelper;
import com.qinghe.music163pro.model.Song;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages downloading songs to /sdcard/163Music/Download/
 */
public class DownloadManager {

    private static final String TAG = "DownloadManager";
    private static final String DOWNLOAD_DIR = "163Music/Download";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DownloadCallback {
        void onSuccess(String filePath);
        void onError(String message);
    }

    /**
     * Download a song to /sdcard/163Music/Download/
     */
    public static void downloadSong(Song song, String cookie, DownloadCallback callback) {
        executor.execute(() -> {
            try {
                // First get a fresh URL
                MusicApiHelper.getSongUrl(song.getId(), cookie, new MusicApiHelper.UrlCallback() {
                    @Override
                    public void onResult(String url) {
                        executor.execute(() -> doDownload(song, url, callback));
                    }

                    @Override
                    public void onError(String message) {
                        mainHandler.post(() -> callback.onError("获取下载链接失败: " + message));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("下载失败: " + e.getMessage()));
            }
        });
    }

    private static void doDownload(Song song, String urlStr, DownloadCallback callback) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), DOWNLOAD_DIR);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    mainHandler.post(() -> callback.onError("无法创建下载目录"));
                    return;
                }
            }

            // Sanitize filename
            String safeName = song.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
            String safeArtist = song.getArtist().replaceAll("[\\\\/:*?\"<>|]", "_");
            String fileName = safeName + " - " + safeArtist + ".mp3";
            File outputFile = new File(dir, fileName);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            try {
                InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(outputFile);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                fos.close();
                is.close();

                String filePath = outputFile.getAbsolutePath();
                mainHandler.post(() -> callback.onSuccess(filePath));
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            Log.w(TAG, "Download error", e);
            mainHandler.post(() -> callback.onError("下载失败: " + e.getMessage()));
        }
    }

    /**
     * Get list of downloaded song files from /sdcard/163Music/Download/
     */
    public static List<File> getDownloadedFiles() {
        List<File> files = new ArrayList<>();
        File dir = new File(Environment.getExternalStorageDirectory(), DOWNLOAD_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] listing = dir.listFiles();
            if (listing != null) {
                for (File f : listing) {
                    if (f.isFile() && f.getName().endsWith(".mp3")) {
                        files.add(f);
                    }
                }
            }
        }
        return files;
    }

    /**
     * Get the download directory path
     */
    public static String getDownloadDirPath() {
        return new File(Environment.getExternalStorageDirectory(), DOWNLOAD_DIR).getAbsolutePath();
    }

    /**
     * Check if a song is already downloaded
     */
    public static boolean isDownloaded(Song song) {
        String safeName = song.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
        String safeArtist = song.getArtist().replaceAll("[\\\\/:*?\"<>|]", "_");
        String fileName = safeName + " - " + safeArtist + ".mp3";
        File dir = new File(Environment.getExternalStorageDirectory(), DOWNLOAD_DIR);
        File file = new File(dir, fileName);
        return file.exists();
    }
}
