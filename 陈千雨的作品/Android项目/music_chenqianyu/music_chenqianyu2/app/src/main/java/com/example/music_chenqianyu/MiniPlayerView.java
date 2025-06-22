package com.example.music_chenqianyu;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.music_chenqianyu.adapter.PlaylistAdapter;
import com.example.music_chenqianyu.model.MusicInfo;

import java.util.ArrayList;
import java.util.List;

public class MiniPlayerView extends LinearLayout {

    private List<MusicInfo> currentPlaylist = new ArrayList<>();
    private ImageView coverImage;
    private TextView musicTitle;
    private TextView musicArtist;
    private ImageButton playButton;
    private ImageButton playlistButton;
    private MusicInfo currentMusic;
    private boolean isPlaying = false;
    private PlaylistAdapter playlistAdapter;
    private Dialog playlistDialog;

    public MiniPlayerView(Context context) {
        super(context);
        init(context);
    }

    public MiniPlayerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.mini_player, this, true);
        coverImage = findViewById(R.id.miniCoverImage);
        musicTitle = findViewById(R.id.miniMusicTitle);
        musicArtist = findViewById(R.id.miniMusicArtist);
        playButton = findViewById(R.id.miniPlayButton);
        playlistButton = findViewById(R.id.playlistButton);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.ACTION_PLAYLIST_UPDATED);
        context.registerReceiver(playlistUpdateReceiver, filter);

        // 播放/暂停按钮点击事件
        playButton.setOnClickListener(v -> {
            if (currentMusic != null) {
                Intent serviceIntent = new Intent(getContext(), MusicService.class);
                serviceIntent.setAction(isPlaying ?
                        MusicService.ACTION_PAUSE :
                        MusicService.ACTION_PLAY);
                getContext().startService(serviceIntent);
            }
        });

        // 播放列表按钮点击事件
        playlistButton.setOnClickListener(v -> showPlaylistDialog());
    }

    private final BroadcastReceiver playlistUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(MusicService.ACTION_PLAYLIST_UPDATED)) {
                ArrayList<MusicInfo> playlist = intent.getParcelableArrayListExtra("playList");
                if (playlist != null) {
                    setCurrentPlaylist(playlist);
                }
            }
        }
    };
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(playlistUpdateReceiver);
    }

    private void showPlaylistDialog() {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            return;
        }

        playlistDialog = new Dialog(getContext());
        playlistDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        playlistDialog.setContentView(R.layout.dialog_playlist);

        Window window = playlistDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
            window.getDecorView().setPadding(0, 0, 0, 0);
        }

        // 初始化播放模式显示
        TextView tvCurrentPlayMode = playlistDialog.findViewById(R.id.tvCurrentPlayMode);
        tvCurrentPlayMode.setVisibility(View.GONE); // 迷你播放器不需要显示播放模式

        RecyclerView recyclerView = playlistDialog.findViewById(R.id.playlistRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        playlistAdapter = new PlaylistAdapter(currentPlaylist, new PlaylistAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (position >= 0 && position < currentPlaylist.size()) {
                    // 播放选中的歌曲
                    Intent serviceIntent = new Intent(getContext(), MusicService.class);
                    serviceIntent.putParcelableArrayListExtra("playList", new ArrayList<>(currentPlaylist));
                    serviceIntent.putExtra("currentPosition", position);
                    serviceIntent.setAction(MusicService.ACTION_PLAY);
                    getContext().startService(serviceIntent);

                    // 更新当前播放状态
                    currentMusic = currentPlaylist.get(position);
                    isPlaying = true;
                    updateUI();

                    playlistDialog.dismiss();
                }
            }

            @Override
            public void onDeleteClick(int position) {
                // 可选：实现删除逻辑
                if (position >= 0 && position < currentPlaylist.size()) {
                    currentPlaylist.remove(position);
                    playlistAdapter.notifyItemRemoved(position);

                    // 如果删除的是当前播放的歌曲
                    if (currentMusic != null && position == currentPlaylist.indexOf(currentMusic)) {
                        if (!currentPlaylist.isEmpty()) {
                            int newPosition = Math.max(0, position - 1);
                            currentMusic = currentPlaylist.get(newPosition);
                            isPlaying = true;
                            updateUI();
                        } else {
                            currentMusic = null;
                            isPlaying = false;
                            updateUI();
                        }
                    }
                }
            }
        });

        // 设置当前播放的歌曲
        if (currentMusic != null) {
            playlistAdapter.setCurrentPlayingPosition(currentPlaylist.indexOf(currentMusic));
        }

        recyclerView.setAdapter(playlistAdapter);
        playlistDialog.show();
    }

    public void setCurrentPlaylist(List<MusicInfo> playlist) {
        if (playlist != null) {
            this.currentPlaylist = new ArrayList<>(playlist);
        }
    }

    public void setMusic(MusicInfo music, boolean playing) {
        this.currentMusic = music;
        this.isPlaying = playing;
        updateUI();
    }

    private void updateUI() {
        if (currentMusic != null) {
            musicTitle.setText(currentMusic.getMusicName());
            musicArtist.setText(currentMusic.getAuthor());
            playButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);

            Glide.with(getContext())
                    .load(currentMusic.getCoverUrl())
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_error)
                    .into(coverImage);
        }
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
        playButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }
}