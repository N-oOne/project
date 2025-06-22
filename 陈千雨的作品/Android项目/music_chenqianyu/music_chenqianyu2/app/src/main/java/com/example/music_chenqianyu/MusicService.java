package com.example.music_chenqianyu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.example.music_chenqianyu.model.MusicInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {
    public static final String ACTION_PLAYLIST_UPDATED = "com.example.music_chenqianyu.PLAYLIST_UPDATED";


    private List<MusicInfo> currentPlaylist = new ArrayList<>();
    private static final String TAG = "MusicService";
    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "music_channel";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREV = "action_prev";
    public static final String ACTION_STOP = "action_stop";
    public static final String ACTION_PLAYBACK_STATE_CHANGED = "com.example.music_chenqianyu.PLAYBACK_STATE_CHANGED";
    public static final String ACTION_MUSIC_CHANGED = "com.example.music_chenqianyu.MUSIC_CHANGED";

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new MusicBinder();
    private MusicInfo currentMusic;
    private MediaSessionCompat mediaSession;
    private OnCompletionListener completionListener;
    private OnPlaybackStateChangedListener playbackStateListener;

    public interface OnPlaybackStateChangedListener {
        void onPlaybackStateChanged(boolean isPlaying);
    }

    public interface OnCompletionListener {
        void onCompletion();
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaSession = new MediaSessionCompat(this, "MusicService");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra("playList")) {
                currentPlaylist = intent.getParcelableArrayListExtra("playList");
            }

            if (intent.getAction() != null) {
                handleAction(intent.getAction());
            }
        }
        return START_STICKY;
    }

    private void handleAction(String action) {
        switch (action) {
            case ACTION_PLAY:
                if (currentPlaylist != null && !currentPlaylist.isEmpty() && currentMusic == null) {
                    playMusic(currentPlaylist.get(0));
                } else {
                    resumeMusic();
                }
                break;
            case ACTION_PAUSE:
                pauseMusic();
                break;
            case ACTION_NEXT:
                if (completionListener != null) {
                    completionListener.onCompletion();
                }
                break;
            case ACTION_PREV:
                if (completionListener != null) {
                    completionListener.onCompletion();
                }
                break;
            case ACTION_STOP:
                stopSelf();
                break;
        }
    }

    public List<MusicInfo> getCurrentPlaylist() {
        return currentPlaylist;
    }

    public void setCompletionListener(OnCompletionListener listener) {
        this.completionListener = listener;
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> {
                if (completionListener != null) {
                    completionListener.onCompletion();
                }
            });
        }
    }

    public void removeFromPlaylist(int position) {
        if (position >= 0 && position < currentPlaylist.size()) {
            currentPlaylist.remove(position);
            sendPlaylistUpdateBroadcast();

            // 如果删除的是当前播放歌曲
            if (position == currentPlaylist.indexOf(currentMusic)) {
                if (!currentPlaylist.isEmpty()) {
                    int newPosition = Math.max(0, position - 1);
                    playMusic(currentPlaylist.get(newPosition));
                } else {
                    stopMusic();
                }
            }
        }
    }

    public void setPlaybackStateListener(OnPlaybackStateChangedListener listener) {
        this.playbackStateListener = listener;
    }

    public void setCurrentMusic(MusicInfo music) {
        this.currentMusic = music;
        updateNotification();
    }

    public void setCurrentPlaylist(List<MusicInfo> playlist) {
        if (playlist != null) {
            this.currentPlaylist = new ArrayList<>(playlist);
            sendPlaylistUpdateBroadcast();
        }
    }
    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private void sendPlaylistUpdateBroadcast() {
        Intent intent = new Intent(ACTION_PLAYLIST_UPDATED);
        intent.putParcelableArrayListExtra("playList", new ArrayList<>(currentPlaylist));
        sendBroadcast(intent);
    }
    public void addToPlaylist(MusicInfo music) {
        if (music == null) return;

        boolean alreadyExists = false;
        for (MusicInfo item : currentPlaylist) {
            if (item.getId() == music.getId()) {
                alreadyExists = true;
                break;
            }
        }

        if (!currentPlaylist.contains(music)) {
            currentPlaylist.add(music);
            sendPlaylistUpdateBroadcast(); // 通知所有界面更新
        }
    }

    public void playMusic(MusicInfo music) {
        if (music == null) return;

        if (!currentPlaylist.contains(music)) {
            currentPlaylist.add(music);
        }

        setCurrentPlaylist(currentPlaylist);

        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(music.getMusicUrl());
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(mp -> {
                    setCurrentMusic(music);
                    mediaPlayer.start();
                    updateNotification();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, buildNotification(),
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                    } else {
                        startForeground(NOTIFICATION_ID, buildNotification());
                    }

                    if (playbackStateListener != null) {
                        playbackStateListener.onPlaybackStateChanged(true);
                    }

                    sendMusicChangedBroadcast(music, true);
                });

                mediaPlayer.setOnCompletionListener(mp -> {
                    if (completionListener != null) {
                        completionListener.onCompletion();
                    }
                });

                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "播放错误: " + what + ", " + extra);
                    return true;
                });

            } catch (IOException e) {
                Log.e(TAG, "播放失败", e);
            }
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateNotification();
            sendPlaybackStateBroadcast(false);

            if (playbackStateListener != null) {
                playbackStateListener.onPlaybackStateChanged(false);
            }
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            updateNotification();
            sendPlaybackStateBroadcast(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, buildNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID, buildNotification());
            }

            if (playbackStateListener != null) {
                playbackStateListener.onPlaybackStateChanged(true);
            }
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
        stopForeground(true);
        sendMusicChangedBroadcast(null, false);

        if (playbackStateListener != null) {
            playbackStateListener.onPlaybackStateChanged(false);
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public MusicInfo getCurrentMusic() {
        return currentMusic;
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putParcelableArrayListExtra("playList", new ArrayList<>(currentPlaylist));
        intent.putExtra("currentMusic", currentMusic);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                        PendingIntent.FLAG_UPDATE_CURRENT);

        MediaStyle style = new MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentMusic != null ? currentMusic.getMusicName() : "正在播放")
                .setContentText(currentMusic != null ? currentMusic.getAuthor() : "未知艺术家")
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(currentMusic != null ?
                        BitmapFactory.decodeResource(getResources(), R.drawable.app_icon) : null)
                .setContentIntent(pendingIntent)
                .setStyle(style)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(true);

        builder.addAction(new NotificationCompat.Action(
                R.drawable.ic_skip_previous, "上一首",
                getPendingIntent(ACTION_PREV)));

        if (isPlaying()) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_pause, "暂停",
                    getPendingIntent(ACTION_PAUSE)));
        } else {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_play, "播放",
                    getPendingIntent(ACTION_PLAY)));
        }

        builder.addAction(new NotificationCompat.Action(
                R.drawable.ic_skip_next, "下一首",
                getPendingIntent(ACTION_NEXT)));

        return builder.build();
    }

    private PendingIntent getPendingIntent(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                        PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "音乐播放",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("音乐播放控制");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void sendPlaybackStateBroadcast(boolean isPlaying) {
        Intent intent = new Intent(ACTION_PLAYBACK_STATE_CHANGED);
        intent.putExtra("isPlaying", isPlaying);
        sendBroadcast(intent);
    }

    private void sendMusicChangedBroadcast(MusicInfo music, boolean isPlaying) {
        Intent intent = new Intent(ACTION_MUSIC_CHANGED);
        intent.putExtra("music", music);
        intent.putExtra("isPlaying", isPlaying);
        intent.putParcelableArrayListExtra("playList", new ArrayList<>(currentPlaylist));
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
    }
}