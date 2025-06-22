package com.example.music_chenqianyu;

import static androidx.media.session.MediaButtonReceiver.handleIntent;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.music_chenqianyu.adapter.PlaylistAdapter;
import com.example.music_chenqianyu.model.MusicInfo;
import com.example.music_chenqianyu.view.CircleImageView;
import com.example.music_chenqianyu.adapter.PlaylistAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class PlayerActivity extends AppCompatActivity {

    private enum PlayMode {
        SHUFFLE, SEQUENTIAL, LOOP
    }

    // UI Components
    private CircleImageView coverImage;
    private RelativeLayout rootLayout;
    private ImageButton btnPlay, btnPrev, btnNext, btnMode, btnClose;
    private SeekBar seekBar;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvLyrics;
    private ScrollView lyricsScrollView;

    private PlaylistAdapter playlistAdapter;

    //喜欢按钮
    private ImageButton likeButton;
    private boolean isLiked = false;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "MusicPrefs";
    private static final String KEY_LIKED = "isLiked";

    private ImageButton playlistButton;
    private Dialog playlistDialog;

    // Music Data
    private List<MusicInfo> playList;
    private int currentPosition;
    private boolean isPlaying = true;
    private PlayMode currentMode = PlayMode.SEQUENTIAL;
    private Random random = new Random();

    // Service
    private MusicService musicService;
    private boolean isBound = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            // Set up listeners
            musicService.setCompletionListener(() -> runOnUiThread(() -> handleSongCompletion()));

            musicService.setPlaybackStateListener(new MusicService.OnPlaybackStateChangedListener() {
                @Override
                public void onPlaybackStateChanged(boolean playing) {
                    runOnUiThread(() -> {
                        isPlaying = playing;
                        updatePlayButton(playing);
                        if (playing && !showingLyrics) {
                            startRotationAnimation();
                        } else {
                            pauseRotationAnimation();
                        }
                    });
                }
            });

            if (playList != null && !playList.isEmpty()) {
                musicService.setCurrentPlaylist(playList);
                musicService.playMusic(playList.get(currentPosition));
                updatePlayButton(true);
                startRotationAnimation();
                progressHandler.post(updateProgress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    // Animation
    private ObjectAnimator rotationAnimator;
    private final Handler progressHandler = new Handler();
    private GestureDetectorCompat gestureDetector;
    private boolean showingLyrics = false;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private final OkHttpClient httpClient = new OkHttpClient();
    private String currentLyrics = "正在加载歌词...";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);


        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.ACTION_PLAYLIST_UPDATED);
        registerReceiver(playlistUpdateReceiver, filter);

        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        isLiked = sharedPreferences.getBoolean(KEY_LIKED, false);

        // 初始化视图
        likeButton = findViewById(R.id.likeButton);

        // 设置初始状态
        updateLikeButtonState();

        // 设置点击监听器
        likeButton.setOnClickListener(v -> toggleLike());

        playlistButton = findViewById(R.id.playlistButton);
        playlistButton.setOnClickListener(v -> showPlaylistDialog());

        // 进入动画
        overridePendingTransition(R.anim.slide_in_up, R.anim.stay);

        setDefaultBackground();

        // 初始化服务
        Intent serviceIntent = new Intent(this, MusicService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        initViews();
        initData();
        handleIntent(getIntent());
        setupGestureDetector();
        setupControls();
        loadCoverImage();
        loadLyrics();

    }
    private final BroadcastReceiver playlistUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(MusicService.ACTION_PLAYLIST_UPDATED)) {
                ArrayList<MusicInfo> playlist = intent.getParcelableArrayListExtra("playList");
                if (playlist != null) {
                    playList = playlist;
                    if (playlistAdapter != null) {
                        playlistAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };


    private void toggleLike() {
        isLiked = !isLiked;

        // 保存状态
        sharedPreferences.edit().putBoolean(KEY_LIKED, isLiked).apply();

        // 更新按钮状态
        updateLikeButtonState();

        // 执行动画
        if (isLiked) {
            playLikeAnimation();
        } else {
            playUnlikeAnimation();
        }
    }
    private void updateLikeButtonState() {
        likeButton.setImageResource(isLiked ?
                R.drawable.ic_favorite_white_24dp :
                R.drawable.ic_favorite_border_white_24dp);
    }

    private void playLikeAnimation() {
        // 缩放动画：1.0 -> 1.2 -> 1.0
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(likeButton, "scaleX", 1.0f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(likeButton, "scaleY", 1.0f, 1.2f, 1.0f);

        // 旋转动画：沿Y轴旋转360度
        ObjectAnimator rotationY = ObjectAnimator.ofFloat(likeButton, "rotationY", 0f, 360f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, rotationY);
        animatorSet.setDuration(1000);
        animatorSet.start();
    }

    private void playUnlikeAnimation() {
        // 缩放动画：1.0 -> 0.8 -> 1.0
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(likeButton, "scaleX", 1.0f, 0.8f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(likeButton, "scaleY", 1.0f, 0.8f, 1.0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(1000);
        animatorSet.start();
    }
    private void showPlaylistDialog() {
        if (playList == null || playList.isEmpty()) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }

        playlistDialog = new Dialog(this);
        playlistDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        playlistDialog.setContentView(R.layout.dialog_playlist);

        Window window = playlistDialog.getWindow();
        if (window != null) {
            // 设置透明背景，让圆角生效
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // 设置对话框宽度和位置
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);

            // 移除默认的边距，让圆角完全显示
            window.getDecorView().setPadding(0, 0, 0, 0);
        }

        // 设置当前播放模式和标题
        TextView tvCurrentPlayMode = playlistDialog.findViewById(R.id.tvCurrentPlayMode);
        String playModeText = "";
        switch (currentMode) {
            case SEQUENTIAL:
                playModeText = "顺序播放";
                break;
            case SHUFFLE:
                playModeText = "随机播放";
                break;
            case LOOP:
                playModeText = "单曲循环";
                break;
        }
        tvCurrentPlayMode.setText("播放模式: " + playModeText);
        tvCurrentPlayMode.setVisibility(View.VISIBLE);

        RecyclerView recyclerView = playlistDialog.findViewById(R.id.playlistRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        playlistAdapter = new PlaylistAdapter(playList, new PlaylistAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                currentPosition = position;
                resetPlayer();
                playlistDialog.dismiss();
            }

            @Override
            public void onDeleteClick(int position) {
                if (isBound && musicService != null) {
                    musicService.removeFromPlaylist(position);
                    playList.remove(position);
                    playlistAdapter.notifyItemRemoved(position);

                    if (position == currentPosition) {
                        if (playList.isEmpty()) {
                            finish();
                        } else {
                            currentPosition = Math.max(0, position - 1);
                            resetPlayer();
                        }
                    } else if (position < currentPosition) {
                        currentPosition--;
                    }
                }
            }
        });

        playlistAdapter.setCurrentPlayingPosition(currentPosition);
        recyclerView.setAdapter(playlistAdapter);

        btnClose.setOnClickListener(v -> playlistDialog.dismiss());

        playlistDialog.show();
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initData();
        resetPlayer();
    }

    private void handleIntent(Intent intent) {
        setIntent(intent);

        if (intent.hasExtra("playList")) {
            playList = intent.getParcelableArrayListExtra("playList");
            if (intent.hasExtra("currentPosition")) {
                currentPosition = intent.getIntExtra("currentPosition", 0);
            } else if (intent.hasExtra("currentMusic")) {
                MusicInfo music = intent.getParcelableExtra("currentMusic");
                currentPosition = playList.indexOf(music);
            }
        } else if (intent.hasExtra("currentMusic")) {
            MusicInfo music = intent.getParcelableExtra("currentMusic");
            playList = new ArrayList<>();
            playList.add(music);
            currentPosition = 0;
        }

        isPlaying = intent.getBooleanExtra("isPlaying", true);
        initPlayer();
    }

    private void initPlayer() {
        if (playList == null || playList.isEmpty()) {
            Toast.makeText(this, "No music data available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        MusicInfo current = playList.get(currentPosition);
        tvTitle.setText(current.getMusicName());
        tvArtist.setText(current.getAuthor());

        loadCoverImage();
        loadLyrics();

        if (isBound && musicService != null) {
            musicService.setCurrentPlaylist(playList);
            musicService.playMusic(current);
            updatePlayButton(true);
            startRotationAnimation();
            progressHandler.post(updateProgress);
        }
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
            overridePendingTransition(0, R.anim.slide_out_down);
        });
        btnClose.setFocusable(true);
        btnClose.setFocusableInTouchMode(true);
        btnClose.requestFocus();
        coverImage = findViewById(R.id.coverImage);
        rootLayout = findViewById(R.id.rootLayout);
        btnPlay = findViewById(R.id.playButton);
        btnPrev = findViewById(R.id.prevButton);
        btnNext = findViewById(R.id.nextButton);
        btnMode = findViewById(R.id.modeButton);
        seekBar = findViewById(R.id.seekBar);
        tvTitle = findViewById(R.id.musicTitle);
        tvArtist = findViewById(R.id.musicAuthor);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvLyrics = findViewById(R.id.tvLyrics);
        lyricsScrollView = findViewById(R.id.lyricsScrollView);
    }



    private void initData() {
        playList = getIntent().getParcelableArrayListExtra("playList");
        currentPosition = getIntent().getIntExtra("currentPosition", 0);

        if (playList != null && !playList.isEmpty()) {
            MusicInfo current = playList.get(currentPosition);
            tvTitle.setText(current.getMusicName());
            tvArtist.setText(current.getAuthor());
        }
        isPlaying = true;
    }

    private void resetPlayer() {
        if (playList == null || playList.isEmpty()) return;

        if (isBound && musicService != null) {
            musicService.setCurrentPlaylist(playList);
            musicService.playMusic(playList.get(currentPosition));
            isPlaying = true;
            updatePlayButton(true);
            startRotationAnimation();
            progressHandler.post(updateProgress);

            // 更新播放列表中的当前播放位置
            if (playlistAdapter != null) {
                playlistAdapter.setCurrentPlayingPosition(currentPosition);
            }
        }

        MusicInfo current = playList.get(currentPosition);
        tvTitle.setText(current.getMusicName());
        tvArtist.setText(current.getAuthor());
        loadCoverImage();
        loadLyrics();
    }


    private void handleSongCompletion() {
        if (currentMode == PlayMode.LOOP) {
            if (isBound && musicService != null) {
                musicService.seekTo(0);
                musicService.resumeMusic();
            }
        } else {
            playNext();
        }
    }

    private void playNext() {
        switch (currentMode) {
            case SEQUENTIAL:
                currentPosition = (currentPosition + 1) % playList.size();
                break;
            case SHUFFLE:
                int newPosition;
                do {
                    newPosition = random.nextInt(playList.size());
                } while (newPosition == currentPosition && playList.size() > 1);
                currentPosition = newPosition;
                break;
            case LOOP:
                return;
        }
        resetPlayer();
    }

    private void playPrevious() {
        switch (currentMode) {
            case SEQUENTIAL:
                currentPosition = (currentPosition - 1 + playList.size()) % playList.size();
                break;
            case SHUFFLE:
                int newPosition;
                do {
                    newPosition = random.nextInt(playList.size());
                } while (newPosition == currentPosition && playList.size() > 1);
                currentPosition = newPosition;
                break;
            case LOOP:
                return;
        }
        resetPlayer();
    }

    private void switchPlayMode() {
        switch (currentMode) {
            case SEQUENTIAL:
                currentMode = PlayMode.SHUFFLE;
                btnMode.setImageResource(R.drawable.ic_shuffle);
                Toast.makeText(this, "随机播放模式", Toast.LENGTH_SHORT).show();
                break;
            case SHUFFLE:
                currentMode = PlayMode.LOOP;
                btnMode.setImageResource(R.drawable.ic_loop);
                Toast.makeText(this, "单曲循环模式", Toast.LENGTH_SHORT).show();
                break;
            case LOOP:
                currentMode = PlayMode.SEQUENTIAL;
                btnMode.setImageResource(R.drawable.ic_mode_circle);
                Toast.makeText(this, "顺序播放模式", Toast.LENGTH_SHORT).show();
                break;
        }

        // 更新播放列表对话框中的播放模式显示（如果对话框正在显示）
        if (playlistDialog != null && playlistDialog.isShowing()) {
            TextView tvCurrentPlayMode = playlistDialog.findViewById(R.id.tvCurrentPlayMode);
            String playModeText = "";
            switch (currentMode) {
                case SEQUENTIAL:
                    playModeText = "顺序播放";
                    break;
                case SHUFFLE:
                    playModeText = "随机播放";
                    break;
                case LOOP:
                    playModeText = "单曲循环";
                    break;
            }
            tvCurrentPlayMode.setText("播放模式: " + playModeText);
        }
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX < 0 && !showingLyrics) {
                        toggleLyricsView();
                    } else if (diffX > 0 && showingLyrics) {
                        toggleLyricsView();
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        coverImage.setOnTouchListener((v, event) -> {
            // 检查是否点击了关闭按钮区域
            if (isPointInsideView(event.getRawX(), event.getRawY(), btnClose)) {
                return false; // 让关闭按钮处理点击
            }
            return gestureDetector.onTouchEvent(event);
        });
        lyricsScrollView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }
    private boolean isPointInsideView(float x, float y, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];

        return (x > viewX && x < (viewX + view.getWidth())) &&
                (y > viewY && y < (viewY + view.getHeight()));
    }

    private void toggleLyricsView() {
        showingLyrics = !showingLyrics;
        coverImage.setVisibility(showingLyrics ? View.GONE : View.VISIBLE);
        lyricsScrollView.setVisibility(showingLyrics ? View.VISIBLE : View.GONE);

        if (showingLyrics) {
            pauseRotationAnimation();
            tvLyrics.setText(currentLyrics);
        } else if (isPlaying) {
            startRotationAnimation();
        }
    }

    private void setupControls() {
        btnPlay.setOnClickListener(v -> togglePlayPause());
        btnPrev.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());
        btnMode.setOnClickListener(v -> switchPlayMode());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatTime(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progressHandler.removeCallbacks(updateProgress);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBound && musicService != null) {
                    musicService.seekTo(seekBar.getProgress());
                }
                progressHandler.post(updateProgress);
            }
        });
    }

    private void togglePlayPause() {
        if (isPlaying) pauseMusic(); else playMusic();
    }

    private void playMusic() {
        if (isBound && musicService != null) musicService.resumeMusic();
        progressHandler.post(updateProgress);
    }

    private void pauseMusic() {
        if (isBound && musicService != null) {
            musicService.pauseMusic();
            progressHandler.removeCallbacks(updateProgress);
        }
    }

    private void updatePlayButton(boolean playing) {
        btnPlay.setImageResource(playing ? R.drawable.ic_play_rec : R.drawable.ic_play);
    }

    private void initRotationAnimation() {
        rotationAnimator = ObjectAnimator.ofFloat(coverImage, "rotation", 0f, 360f);
        rotationAnimator.setDuration(20000);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
    }

    private void startRotationAnimation() {
        if (rotationAnimator == null) initRotationAnimation();
        if (!rotationAnimator.isRunning()) {
            if (rotationAnimator.isPaused()) {
                rotationAnimator.resume();
            } else {
                rotationAnimator.start();
            }
        }
    }

    private void pauseRotationAnimation() {
        if (rotationAnimator != null && rotationAnimator.isRunning()) rotationAnimator.pause();
    }

    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (isBound && musicService != null) {
                int currentPos = musicService.getCurrentPosition();
                int duration = musicService.getDuration();

                seekBar.setMax(duration);
                seekBar.setProgress(currentPos);
                tvCurrentTime.setText(formatTime(currentPos));
                tvTotalTime.setText(formatTime(duration));

                if (showingLyrics) updateLyricsScrollPosition(currentPos, duration);
            }
            if (isPlaying) progressHandler.postDelayed(this, 1000);
        }
    };

    private void updateLyricsScrollPosition(int currentPos, int duration) {
        float progressRatio = (float) currentPos / duration;
        int scrollRange = tvLyrics.getHeight() - lyricsScrollView.getHeight();
        if (scrollRange > 0) lyricsScrollView.smoothScrollTo(0, (int) (progressRatio * scrollRange));
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void loadLyrics() {
        if (playList == null || playList.isEmpty()) return;

        MusicInfo currentMusic = playList.get(currentPosition);
        String lyricUrl = currentMusic.getLyricUrl();

        if (TextUtils.isEmpty(lyricUrl)) {
            currentLyrics = "No lyrics available";
            tvLyrics.setText(currentLyrics);
            return;
        }

        Request request = new Request.Builder().url(lyricUrl).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    currentLyrics = "Failed to load lyrics";
                    if (showingLyrics) tvLyrics.setText(currentLyrics);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        currentLyrics = "Failed to load lyrics";
                        if (showingLyrics) tvLyrics.setText(currentLyrics);
                    });
                    return;
                }

                String lyrics = response.body().string();
                runOnUiThread(() -> {
                    currentLyrics = parseLyrics(lyrics);
                    if (showingLyrics) tvLyrics.setText(currentLyrics);
                });
            }
        });
    }

    private String parseLyrics(String rawLyrics) {
        if (rawLyrics == null || rawLyrics.isEmpty()) return "No lyrics available";

        StringBuilder builder = new StringBuilder();
        String[] lines = rawLyrics.split("\n");
        for (String line : lines) {
            String text = line.replaceAll("\\[[0-9]{2}:[0-9]{2}\\.[0-9]{2}\\]", "").trim();
            if (!text.isEmpty()) builder.append(text).append("\n\n");
        }
        return builder.toString().trim();
    }

    private void loadCoverImage() {
        if (playList == null || playList.isEmpty()) return;

        MusicInfo current = playList.get(currentPosition);
        Glide.with(this)
                .load(current.getCoverUrl())
                .error(R.drawable.ic_music_note)
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                        coverImage.setImageDrawable(resource);
                        if (resource instanceof BitmapDrawable) {
                            Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                            if (bitmap != null && !bitmap.isRecycled()) {
                                extractColorsFromCover(bitmap);
                            } else {
                                setDefaultBackground();
                            }
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        setDefaultBackground();
                        coverImage.setImageResource(R.drawable.ic_music_note);
                    }
                });
    }

    private void extractColorsFromCover(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette == null || rootLayout == null) {
                setDefaultBackground();
                return;
            }

            Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
            if (vibrantSwatch != null) {
                updateBackgroundGradient(vibrantSwatch.getRgb(), adjustColorBrightness(vibrantSwatch.getRgb(), 0.7f));
                return;
            }

            Palette.Swatch dominantSwatch = palette.getDominantSwatch();
            if (dominantSwatch != null) {
                updateBackgroundGradient(dominantSwatch.getRgb(), adjustColorBrightness(dominantSwatch.getRgb(), 0.7f));
                return;
            }

            Palette.Swatch mutedSwatch = palette.getMutedSwatch();
            if (mutedSwatch != null) {
                updateBackgroundGradient(mutedSwatch.getRgb(), adjustColorBrightness(mutedSwatch.getRgb(), 0.7f));
                return;
            }

            setDefaultBackground();
        });
    }

    private void updateBackgroundGradient(int primaryColor, int secondaryColor) {
        runOnUiThread(() -> {
            if (rootLayout == null) return;

            final int finalPrimary = primaryColor | 0xFF000000;
            final int finalSecondary = secondaryColor | 0xFF000000;

            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{finalPrimary, finalSecondary}
            );
            gradient.setCornerRadius(0f);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                rootLayout.setBackground(gradient);
            } else {
                rootLayout.setBackgroundDrawable(gradient);
            }
            rootLayout.invalidate();
        });
    }

    private void setDefaultBackground() {
        updateBackgroundGradient(
                Color.parseColor("#3F51B5"),
                Color.parseColor("#5C6BC0")
        );
    }

    private int adjustColorBrightness(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.max(0.1f, Math.min(hsv[2] * factor, 1f));
        return Color.HSVToColor(hsv);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_down);
    }

    @Override
    protected void onPause() {
        super.onPause();
        progressHandler.removeCallbacks(updateProgress);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isPlaying) progressHandler.post(updateProgress);
    }

    @Override
    protected void onDestroy() {
        if (playlistDialog != null && playlistDialog.isShowing()) {
            playlistDialog.dismiss();
        }
        unregisterReceiver(playlistUpdateReceiver);
        super.onDestroy();
        // Send playlist update before destroying
        if (isBound && musicService != null) {
            Intent intent = new Intent(MusicService.ACTION_PLAYLIST_UPDATED);
            intent.putParcelableArrayListExtra("playList", new ArrayList<>(playList));
            sendBroadcast(intent);

            musicService.setPlaybackStateListener(null);
            unbindService(serviceConnection);
        }
        progressHandler.removeCallbacks(updateProgress);
        if (rotationAnimator != null) rotationAnimator.cancel();
    }
}