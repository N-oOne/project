package com.example.music_chenqianyu;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.music_chenqianyu.adapter.BannerAdapter;
import com.example.music_chenqianyu.adapter.HorizontalCardAdapter;
import com.example.music_chenqianyu.adapter.HorizontalSpaceItemDecoration;
import com.example.music_chenqianyu.adapter.TwoColumnAdapter;
import com.example.music_chenqianyu.model.HomeResponse;
import com.example.music_chenqianyu.model.ModuleConfig;
import com.example.music_chenqianyu.model.MusicInfo;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeActivity extends AppCompatActivity {
    private boolean shouldAutoPlay = true;
    private boolean fromPlayerActivity = false;
    private boolean isFirstLoad = true;

    private MiniPlayerView miniPlayerView;
    private List<MusicInfo> currentModuleMusics = new ArrayList<>();

    private static final String TAG = "HomeActivity";
    private static final int PAGE_SIZE = 4;
    private static final long AUTO_SCROLL_DELAY = 3000;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    // Views
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;
    private LinearLayout loadMoreLayout;
    private ViewPager2 bannerViewPager;
    private LinearLayout bannerIndicator;
    private RecyclerView horizontalCardRecyclerView;
    private LinearLayout dailyRecommendContainer;
    private RecyclerView twoColumnRecyclerView;
    private TextInputLayout searchLayout;

    // Data lists
    private final List<MusicInfo> bannerMusicList = new ArrayList<>();
    private final List<MusicInfo> horizontalCardList = new ArrayList<>();
    private final List<MusicInfo> dailyRecommendList = new ArrayList<>();
    private final List<MusicInfo> twoColumnList = new ArrayList<>();

    // Adapters
    private BannerAdapter bannerAdapter;
    private HorizontalCardAdapter horizontalCardAdapter;
    private TwoColumnAdapter twoColumnAdapter;

    // Auto scroll
    private Handler autoScrollHandler = new Handler();
    private Runnable autoScrollRunnable;

    private final OkHttpClient client = new OkHttpClient();

    private boolean isServiceBound = false;
    private MusicService musicService;
    private final ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isServiceBound = true;
            updateMiniPlayer();

            // 服务绑定后检查是否需要自动播放
            if (shouldAutoPlay && !currentModuleMusics.isEmpty()) {
                playFirstSong();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 初始化迷你播放器
        miniPlayerView = findViewById(R.id.miniPlayerView);
        miniPlayerView.setVisibility(View.GONE);

        // 绑定音乐服务
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, musicConnection, Context.BIND_AUTO_CREATE);

        initViews();
        setupScrollListener();
        fetchHomeData(currentPage);
        setupSearch();
    }

    private void playFirstSong() {
        if (currentModuleMusics.isEmpty() || !isServiceBound || musicService == null) return;

        MusicInfo firstMusic = currentModuleMusics.get(0);
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.putParcelableArrayListExtra("playList", new ArrayList<>(currentModuleMusics));
        serviceIntent.putExtra("currentPosition", 0);
        serviceIntent.setAction(MusicService.ACTION_PLAY);
        startService(serviceIntent);

        miniPlayerView.setVisibility(View.VISIBLE);
        miniPlayerView.setMusic(firstMusic, true);
        miniPlayerView.setCurrentPlaylist(currentModuleMusics);
        shouldAutoPlay = false;

        // 确保点击事件能正确传递
        miniPlayerView.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, PlayerActivity.class);
            intent.putParcelableArrayListExtra("playList", new ArrayList<>(currentModuleMusics));
            intent.putExtra("currentPosition", 0);
            intent.putExtra("isPlaying", true);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
        });
    }

    private final BroadcastReceiver musicStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case MusicService.ACTION_MUSIC_CHANGED:
                    MusicInfo music = intent.getParcelableExtra("music");
                    boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
                    ArrayList<MusicInfo> playlist = intent.getParcelableArrayListExtra("playList");

                    // 确保数据完整才更新UI
                    if (music != null && playlist != null && !playlist.isEmpty()) {
                        currentModuleMusics = playlist;
                        updateMiniPlayerWithMusic(music, isPlaying, playlist);
                    }
                    break;

                case MusicService.ACTION_PLAYBACK_STATE_CHANGED:
                    boolean playing = intent.getBooleanExtra("isPlaying", false);
                    miniPlayerView.setPlaying(playing);
                    break;
            }
        }
    };
    private void updateMiniPlayerWithMusic(MusicInfo music, boolean isPlaying, List<MusicInfo> playlist) {
        miniPlayerView.setVisibility(View.VISIBLE);
        miniPlayerView.setCurrentPlaylist(playlist);
        miniPlayerView.setMusic(music, isPlaying);

        // 确保点击事件能正确传递
        miniPlayerView.setOnClickListener(v -> {
            if (music != null && playlist != null && !playlist.isEmpty()) {
                int position = playlist.indexOf(music);
                if (position >= 0) {
                    Intent intent = new Intent(HomeActivity.this, PlayerActivity.class);
                    intent.putParcelableArrayListExtra("playList", new ArrayList<>(playlist));
                    intent.putExtra("currentPosition", position);
                    intent.putExtra("isPlaying", isPlaying);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
                }
            }
        });
    }

    private void updateMiniPlayer() {
        if (isServiceBound && musicService != null) {
            MusicInfo currentMusic = musicService.getCurrentMusic();
            List<MusicInfo> playlist = musicService.getCurrentPlaylist();

            if (currentMusic != null && playlist != null && !playlist.isEmpty()) {
                updateMiniPlayerWithMusic(
                        currentMusic,
                        musicService.isPlaying(),
                        playlist
                );
            } else {
                miniPlayerView.setVisibility(View.GONE);
            }
        }
    }
    // 新增方法：处理首页音乐项的点击事件
    public void onMusicItemClick(View view, int position, List<MusicInfo> playlist) {
        if (position < 0 || position >= playlist.size()) {
            Toast.makeText(this, "无效的音乐位置", Toast.LENGTH_SHORT).show();
            return;
        }

        MusicInfo music = playlist.get(position);

        // 更新服务端播放列表
        if (isServiceBound && musicService != null) {
            musicService.setCurrentPlaylist(playlist);
            musicService.playMusic(music);
        }

        // 更新迷你播放器
        updateMiniPlayerWithMusic(music, true, playlist);

        // 跳转到播放页
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putParcelableArrayListExtra("playList", new ArrayList<>(playlist));
        intent.putExtra("currentPosition", position);
        intent.putExtra("isPlaying", true);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
    }


    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        scrollView = findViewById(R.id.scrollView);
        loadMoreLayout = findViewById(R.id.loadMoreLayout);
        searchLayout = findViewById(R.id.searchLayout);
        bannerViewPager = findViewById(R.id.bannerViewPager);
        bannerIndicator = findViewById(R.id.bannerIndicator);
        horizontalCardRecyclerView = findViewById(R.id.horizontalCardRecyclerView);
        dailyRecommendContainer = findViewById(R.id.dailyRecommendContainer);
        twoColumnRecyclerView = findViewById(R.id.twoColumnRecyclerView);

        // 初始化下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentPage = 1;
            isLastPage = false;
            fetchHomeData(currentPage);
        });

        // 设置下拉刷新颜色
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        // 初始化Banner
        bannerAdapter = new BannerAdapter(bannerMusicList);
        bannerViewPager.setAdapter(bannerAdapter);

        // 设置初始位置为中间，以实现循环效果
        int initialPosition = Integer.MAX_VALUE / 2;
        bannerViewPager.setCurrentItem(initialPosition, false);

        // 设置Banner滑动监听
        bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (bannerMusicList.isEmpty()) return;
                updateBannerIndicator(position % bannerAdapter.getRealItemCount());

                // 移除之前的自动滚动任务
                autoScrollHandler.removeCallbacks(autoScrollRunnable);
                // 重新启动自动滚动
                autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // 在滑动停止时确保位置正确
                if (state == ViewPager2.SCROLL_STATE_IDLE && !bannerMusicList.isEmpty()) {
                    int currentItem = bannerViewPager.getCurrentItem();
                    int realCount = bannerAdapter.getRealItemCount();
                    if (currentItem == 0) {
                        bannerViewPager.setCurrentItem(realCount * 100, false);
                    } else if (currentItem == bannerAdapter.getItemCount() - 1) {
                        bannerViewPager.setCurrentItem(realCount * 100 - 1, false);
                    }
                }
            }
        });

        // 初始化自动轮播任务
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (bannerMusicList.size() > 1) {
                    int currentItem = bannerViewPager.getCurrentItem();
                    bannerViewPager.setCurrentItem(currentItem + 1, true);
                }
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY);
            }
        };

        // 初始化横划大卡
        horizontalCardAdapter = new HorizontalCardAdapter(horizontalCardList);
        horizontalCardRecyclerView.setAdapter(horizontalCardAdapter);
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        horizontalCardRecyclerView.setLayoutManager(horizontalLayoutManager);
        horizontalCardRecyclerView.addItemDecoration(new HorizontalSpaceItemDecoration(
                getResources().getDimensionPixelSize(R.dimen.horizontal_card_spacing)));

        // 初始化一行两列
        twoColumnAdapter = new TwoColumnAdapter(twoColumnList);
        twoColumnRecyclerView.setAdapter(twoColumnAdapter);
        twoColumnRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }

    private void setupScrollListener() {
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (isLoading || isLastPage) {
                return;
            }

            View view = scrollView.getChildAt(scrollView.getChildCount() - 1);
            int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));

            // 当滚动到底部时触发加载更多
            if (diff <= 100) {
                currentPage++;
                fetchHomeData(currentPage);
            }
        });
    }

    private void fetchHomeData(int page) {
        isLoading = true;

        if (page == 1) {
            swipeRefreshLayout.setRefreshing(true);
        } else {
            loadMoreLayout.setVisibility(View.VISIBLE);
        }

        String url = "https://hotfix-service-prod.g.mi.com/music/homePage?current=" + page + "&size=" + PAGE_SIZE;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    isLoading = false;
                    swipeRefreshLayout.setRefreshing(false);
                    loadMoreLayout.setVisibility(View.GONE);
                    Toast.makeText(HomeActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "网络请求失败", e);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("请求失败，状态码: " + response.code());
                    }

                    String jsonData = response.body().string();
                    Log.d(TAG, "响应数据: " + jsonData);

                    final HomeResponse homeResponse = new Gson().fromJson(jsonData, HomeResponse.class);

                    runOnUiThread(() -> {
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);
                        loadMoreLayout.setVisibility(View.GONE);

                        if (homeResponse == null || homeResponse.getCode() != 200 || homeResponse.getData() == null) {
                            Toast.makeText(HomeActivity.this, "数据处理错误", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<ModuleConfig> records = homeResponse.getData().getRecords();
                        if (records == null || records.isEmpty()) {
                            isLastPage = true;
                            return;
                        }

                        if (page == 1) {
                            // 第一页数据，清空原有数据
                            bannerMusicList.clear();
                            horizontalCardList.clear();
                            dailyRecommendList.clear();
                            twoColumnList.clear();
                            randomPlayModuleMusic(homeResponse.getData().getRecords());
                        }

                        for (ModuleConfig module : records) {
                            if (module != null && module.getMusicInfoList() != null && !module.getMusicInfoList().isEmpty()) {
                                switch (module.getModuleName()) {
                                    case "banner":
                                        if (page == 1) {
                                            bannerMusicList.addAll(module.getMusicInfoList());
                                            bannerAdapter.notifyDataSetChanged();
                                            setupBannerIndicator();
                                            startAutoScroll();
                                        }
                                        break;
                                    case "横滑大卡":
                                        horizontalCardList.addAll(module.getMusicInfoList());
                                        horizontalCardAdapter.notifyDataSetChanged();
                                        if (page == 1) {
                                            horizontalCardRecyclerView.scrollToPosition(Integer.MAX_VALUE / 2);
                                        }
                                        break;
                                    case "一行一列_test1":
                                        dailyRecommendList.addAll(module.getMusicInfoList());
                                        setupDailyRecommend();
                                        break;
                                    case "一行两列_test1":
                                        twoColumnList.addAll(module.getMusicInfoList());
                                        twoColumnAdapter.notifyDataSetChanged();
                                        break;
                                }
                            }
                        }

                        if (records.size() < PAGE_SIZE) {
                            isLastPage = true;
                        }
                    });
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "JSON解析错误", e);
                    runOnUiThread(() -> {
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);
                        loadMoreLayout.setVisibility(View.GONE);
                        Toast.makeText(HomeActivity.this, "数据解析错误", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "数据处理错误", e);
                    runOnUiThread(() -> {
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);
                        loadMoreLayout.setVisibility(View.GONE);
                        Toast.makeText(HomeActivity.this, "数据处理错误", Toast.LENGTH_SHORT).show();
                    });
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
            }
        });
    }

    private void randomPlayModuleMusic(List<ModuleConfig> modules) {
        if (modules == null || modules.isEmpty()) return;

        List<ModuleConfig> validModules = modules.stream()
                .filter(m -> m.getMusicInfoList() != null && !m.getMusicInfoList().isEmpty())
                .collect(Collectors.toList());

        if (validModules.isEmpty()) return;

        ModuleConfig randomModule = validModules.get(new Random().nextInt(validModules.size()));
        currentModuleMusics = new ArrayList<>(randomModule.getMusicInfoList());

        // 如果服务已绑定，立即播放；否则设置标志，等待服务绑定
        if (isServiceBound && musicService != null) {
            playFirstSong();
        } else {
            shouldAutoPlay = true;
        }

        // 确保MiniPlayerView有正确的点击监听器
        miniPlayerView.setOnClickListener(v -> {
            if (!currentModuleMusics.isEmpty()) {
                Intent intent = new Intent(HomeActivity.this, PlayerActivity.class);
                intent.putParcelableArrayListExtra("playList", new ArrayList<>(currentModuleMusics));
                intent.putExtra("currentPosition", 0);
                intent.putExtra("isPlaying", true);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
            }
        });
    }
    public MusicService getMusicService() {
        return musicService;
    }
    private void setupDailyRecommend() {
        dailyRecommendContainer.removeAllViews();

        // 检查是否有数据
        if (dailyRecommendList.isEmpty()) {
            return;
        }

        // 只取第一条数据
        MusicInfo music = dailyRecommendList.get(0);
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.daily_recommend_item, dailyRecommendContainer, false);

        ImageView dailyImage = itemView.findViewById(R.id.dailyImage);
        TextView musicTitle = itemView.findViewById(R.id.musicTitle);
        TextView musicAuthor = itemView.findViewById(R.id.musicAuthor);
        ImageView addButton = itemView.findViewById(R.id.addButton);

        Glide.with(this)
                .load(music.getCoverUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .into(dailyImage);

        musicTitle.setText(music.getMusicName());
        musicAuthor.setText(music.getAuthor());

        // 设置点击事件
        itemView.setOnClickListener(v -> {
            onMusicItemClick(v, 0, dailyRecommendList);
        });

        // 保留加号按钮功能
        addButton.setOnClickListener(v -> {
            Toast.makeText(HomeActivity.this,
                    "将\"" + music.getMusicName() + "\"添加到音乐列表",
                    Toast.LENGTH_SHORT).show();
            addToPlaylist(music);
        });

        dailyRecommendContainer.addView(itemView);
    }
    // 添加到播放列表
    public void addToPlaylist(MusicInfo music) {
        if (isServiceBound && musicService != null) {
            List<MusicInfo> currentPlaylist = musicService.getCurrentPlaylist();
            musicService.addToPlaylist(music);
            Toast.makeText(this,
                    currentPlaylist.contains(music) ?
                            "\"" + music.getMusicName() + "\"已在列表中" :
                            "已添加\"" + music.getMusicName() + "\"",
                    Toast.LENGTH_SHORT).show();

            // 检查是否已存在
            boolean alreadyExists = false;
            for (MusicInfo item : currentPlaylist) {
                if (item.getId()==music.getId()) {
                    alreadyExists = true;
                    break;
                }
            }

            if (!alreadyExists) {
                // 添加到播放列表
                currentPlaylist.add(music);
                musicService.setCurrentPlaylist(currentPlaylist);

                Toast.makeText(this,
                        "已添加\"" + music.getMusicName() + "\"到播放列表",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "\"" + music.getMusicName() + "\"已在播放列表中",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "服务未准备好，请稍后再试", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBannerIndicator() {
        bannerIndicator.removeAllViews();

        if (bannerMusicList.isEmpty()) return;

        int size = getResources().getDimensionPixelSize(R.dimen.banner_indicator_size);
        int margin = getResources().getDimensionPixelSize(R.dimen.banner_indicator_margin);

        for (int i = 0; i < bannerAdapter.getRealItemCount(); i++) {
            ImageView indicator = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            indicator.setLayoutParams(params);
            indicator.setImageResource(i == 0 ?
                    R.drawable.indicator_selected : R.drawable.indicator_unselected);
            bannerIndicator.addView(indicator);
        }
    }

    private void updateBannerIndicator(int position) {
        if (bannerMusicList.isEmpty() || bannerIndicator.getChildCount() == 0) return;

        int realPosition = position % bannerAdapter.getRealItemCount();
        for (int i = 0; i < bannerIndicator.getChildCount(); i++) {
            ImageView indicator = (ImageView) bannerIndicator.getChildAt(i);
            indicator.setImageResource(i == realPosition ?
                    R.drawable.indicator_selected : R.drawable.indicator_unselected);
        }
    }

    private void setupSearch() {
        searchLayout.setEndIconOnClickListener(v -> {
            String query = searchLayout.getEditText().getText().toString().trim();
            if (!query.isEmpty()) {
                Toast.makeText(this, "搜索: " + query, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startAutoScroll() {
        if (bannerMusicList.size() > 1) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
            autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
        }
    }

    private void stopAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAutoScroll();

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.ACTION_MUSIC_CHANGED);
        filter.addAction(MusicService.ACTION_PLAYBACK_STATE_CHANGED);
        registerReceiver(musicStateReceiver, filter);

        // 每次返回首页都强制刷新状态
        updateMiniPlayer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 100) {
            fromPlayerActivity = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoScroll();
        unregisterReceiver(musicStateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoScroll();
        autoScrollHandler.removeCallbacksAndMessages(null);
        if (isServiceBound) {
            unbindService(musicConnection);
            isServiceBound = false;
        }
    }
}