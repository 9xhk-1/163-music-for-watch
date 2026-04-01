package com.qinghe.music163pro.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import com.qinghe.music163pro.activity.MainActivity;
import com.qinghe.music163pro.player.MusicPlayerManager;

/**
 * Foreground service to keep the music player alive during background
 * playback and when the screen is off. Shows a persistent notification
 * with media controls (previous, play/pause, next).
 */
public class MusicPlaybackService extends Service {

    private static final String CHANNEL_ID = "music163_playback";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_PREVIOUS = "com.qinghe.music163pro.ACTION_PREVIOUS";
    public static final String ACTION_PLAY_PAUSE = "com.qinghe.music163pro.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.qinghe.music163pro.ACTION_NEXT";

    private PowerManager.WakeLock wakeLock;
    private String currentSongName = "";
    private String currentArtist = "";
    private boolean currentIsPlaying = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_PREVIOUS.equals(action)) {
                MusicPlayerManager.getInstance().previous();
                return START_STICKY;
            } else if (ACTION_PLAY_PAUSE.equals(action)) {
                MusicPlayerManager player = MusicPlayerManager.getInstance();
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.resume();
                }
                return START_STICKY;
            } else if (ACTION_NEXT.equals(action)) {
                MusicPlayerManager.getInstance().next();
                return START_STICKY;
            }

            // Normal start/update: extract song info and play state
            String songName = intent.getStringExtra("song_name");
            String artist = intent.getStringExtra("artist");
            boolean isPlaying = intent.getBooleanExtra("is_playing", false);

            if (songName != null) currentSongName = songName;
            if (artist != null) currentArtist = artist;
            currentIsPlaying = isPlaying;
        }

        startForeground(NOTIFICATION_ID, buildNotification(currentSongName, currentArtist, currentIsPlaying));
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "音乐播放",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("音乐播放中");
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String songName, String artist, boolean isPlaying) {
        // Content intent: tap notification to open player
        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action intents
        PendingIntent prevPendingIntent = createActionPendingIntent(ACTION_PREVIOUS, 1);
        PendingIntent playPausePendingIntent = createActionPendingIntent(ACTION_PLAY_PAUSE, 2);
        PendingIntent nextPendingIntent = createActionPendingIntent(ACTION_NEXT, 3);

        String title = (songName != null && !songName.isEmpty()) ? songName : "163音乐";
        String text = (artist != null && !artist.isEmpty()) ? artist : "音乐播放中";

        int playPauseIcon = isPlaying
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play;
        String playPauseLabel = isPlaying ? "暂停" : "播放";

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play)
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .addAction(android.R.drawable.ic_media_previous, "上一曲", prevPendingIntent)
                .addAction(playPauseIcon, playPauseLabel, playPausePendingIntent)
                .addAction(android.R.drawable.ic_media_next, "下一曲", nextPendingIntent);

        // Use MediaStyle if available (API 21+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setStyle(new Notification.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2));
        }

        return builder.build();
    }

    private PendingIntent createActionPendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, MusicPlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "music163:playback");
            wakeLock.acquire(12 * 60 * 60 * 1000L); // 12 hours max to prevent indefinite hold
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
