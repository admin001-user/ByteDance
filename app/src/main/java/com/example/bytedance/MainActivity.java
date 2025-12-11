package com.example.bytedance;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.bytedance.adapter.VideoAdapter;
import com.example.bytedance.adapter.VideoPlayerAdapter;
import com.example.bytedance.data.MockData;
import com.example.bytedance.databinding.ActivityMainBinding;
import com.example.bytedance.databinding.ItemVideoPlayerBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.example.bytedance.util.CacheManager;
import java.lang.ref.WeakReference;
import java.util.List;
import android.os.Handler;
import android.widget.SeekBar;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Locale;
import android.media.MediaMetadataRetriever;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.LruCache;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import android.widget.ImageButton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.animation.ObjectAnimator;
import android.view.animation.LinearInterpolator;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.TextPaint;

import androidx.lifecycle.ViewModelProvider;
import com.example.bytedance.viewmodel.VideoViewModel;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean isTwoColumn = true;
    private ExoPlayer player;
    private VideoPlayerAdapter playerAdapter;
    private VideoViewModel videoViewModel;
    private List<com.example.bytedance.model.VideoItem> videos;
    private boolean isLoadingMore = false;
    private int nextPageIndex = 1;
    private WeakReference<android.widget.ProgressBar> currentProgressRef;
    private WeakReference<com.google.android.exoplayer2.ui.PlayerView> currentPlayerViewRef;
    private WeakReference<SeekBar> currentSeekBarRef;
    private WeakReference<ImageView> currentPauseOverlayRef;
    private WeakReference<TextView> currentTimeTextRef;
    private WeakReference<TextView> totalTimeTextRef;
    private WeakReference<ImageView> currentThumbPreviewRef;
    private WeakReference<TextView> currentDescriptionRef;
    private final Handler progressHandler = new Handler();
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            SeekBar sb = currentSeekBarRef != null ? currentSeekBarRef.get() : null;
            TextView ct = currentTimeTextRef != null ? currentTimeTextRef.get() : null;
            TextView tt = totalTimeTextRef != null ? totalTimeTextRef.get() : null;
            if (player != null) {
                long duration = player.getDuration();
                long position = player.getCurrentPosition();
                if (duration > 0 && duration != com.google.android.exoplayer2.C.TIME_UNSET) {
                    if (!isSeeking && sb != null) {
                        int progress = (int) (position * 1000 / duration);
                        sb.setProgress(progress);
                    }
                    if (ct != null) ct.setText(formatTime(position));
                    if (tt != null) tt.setText(formatTime(duration));
                }
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    private static String formatTime(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", m, s);
        }
    }

    private String currentVideoUrl;
    private long lastPreviewUpdateMs = 0L;
    private boolean isSeeking = false;
    private MediaMetadataRetriever previewRetriever;
    private final LruCache<String, Bitmap> thumbCache = new LruCache<>(50);
    private final java.util.Random rand = new java.util.Random();
    private final ExecutorService thumbExecutor = Executors.newSingleThreadExecutor();
    private Future<?> thumbFuture;
    private final java.util.Map<String, Boolean> likedMap = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> likeCountMap = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> commentCountMap = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> shareCountMap = new java.util.HashMap<>();
    private final java.util.Map<String, Boolean> followMap = new java.util.HashMap<>();
    private WeakReference<android.widget.ImageView> likeAnimRef;
    private WeakReference<android.widget.ImageView> musicDiscRef;
    private WeakReference<android.widget.ImageView> authorAvatarRef;
    private WeakReference<android.widget.ImageView> followButtonRef;
    private WeakReference<android.widget.TextView> authorNameTextRef;
    private ObjectAnimator discRotateAnimator;

    private void updateThumbPreview(long ms) {
        ImageView iv = currentThumbPreviewRef != null ? currentThumbPreviewRef.get() : null;
        if (iv == null || currentVideoUrl == null) return;
        // 节流到300ms，避免高频抽帧
        long now = SystemClock.uptimeMillis();
        if (now - lastPreviewUpdateMs < 300) return;
        lastPreviewUpdateMs = now;

        long bucket = (ms / 1000L) * 1000L; // 1000ms分桶：降低抽帧频率以提高响应速度
        String key = currentVideoUrl + "@" + bucket;
        if (iv != null) iv.setTag(key);
        Bitmap cached = thumbCache.get(key);
        if (cached != null) {
            ImageView target = currentThumbPreviewRef != null ? currentThumbPreviewRef.get() : null;
            if (target != null && key.equals(target.getTag())) target.setImageBitmap(cached);
            return;
        }

        new Thread(() -> {
            try {
                synchronized (MainActivity.this) {
                    if (previewRetriever == null) {
                        previewRetriever = new MediaMetadataRetriever();
                        java.util.HashMap<String, String> headers = new java.util.HashMap<>();
                        previewRetriever.setDataSource(currentVideoUrl, headers);
                    }
                }
                Bitmap bmp = previewRetriever.getFrameAtTime(bucket * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (bmp != null) {
                    // 缩小分辨率以提升显示速度（按缩略图视图宽度或默认120px，最大不超过320px）
                    int targetW = 120;
                    ImageView targetView = currentThumbPreviewRef != null ? currentThumbPreviewRef.get() : null;
                    if (targetView != null && targetView.getWidth() > 0) targetW = Math.min(targetView.getWidth(), 320);
                    int w = bmp.getWidth();
                    int h = bmp.getHeight();
                    if (w > 0 && h > 0 && targetW > 0 && targetW < w) {
                        int targetH = (int) ((long) h * targetW / w);
                        Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(bmp, targetW, Math.max(1, targetH), false);
                        bmp = scaled;
                    }
                    thumbCache.put(key, bmp);
                    final Bitmap toShow = bmp;
                    runOnUiThread(() -> {
                        ImageView target = currentThumbPreviewRef != null ? currentThumbPreviewRef.get() : null;
                        if (target != null && key.equals(target.getTag())) target.setImageBitmap(toShow);
                    });
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private static String formatCount(int count) {
        if (count < 10000) return String.valueOf(count);
        float w = count / 10000f;
        String s = (w >= 100 ? String.format(java.util.Locale.US, "%.0f", w)
                : String.format(java.util.Locale.US, "%.1f", w));
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return s + "w";
    }

    private android.net.Uri getShareableThumbUri() {
        try {
            ImageView iv = currentThumbPreviewRef != null ? currentThumbPreviewRef.get() : null;
            if (iv == null) return null;
            android.graphics.drawable.Drawable d = iv.getDrawable();
            if (!(d instanceof android.graphics.drawable.BitmapDrawable)) return null;
            Bitmap bmp = ((android.graphics.drawable.BitmapDrawable) d).getBitmap();
            if (bmp == null) return null;
            java.io.File dir = new java.io.File(getCacheDir(), "shared_images");
            if (!dir.exists()) dir.mkdirs();
            java.io.File file = new java.io.File(dir, "thumb_" + System.currentTimeMillis() + ".png");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            return androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        ((AppCompatActivity) this).setContentView(binding.getRoot());

        // 顶部 Tab + ViewPager2：推荐/关注/同城
        binding.swipeRefreshGrid.setVisibility(android.view.View.GONE);
        binding.swipeRefreshPager.setVisibility(android.view.View.GONE);
        if (binding.tabsPager != null) {
            binding.tabsPager.setAdapter(new com.example.bytedance.ui.HomePagerAdapter(this));
            new TabLayoutMediator(binding.tabLayout, binding.tabsPager, (tab, position) -> {
                switch (position) {
                    case 0: tab.setText("推荐"); break;
                    case 1: tab.setText("关注"); break;
                    case 2: tab.setText("同城"); break;
                }
            }).attach();
        }

        videoViewModel = new ViewModelProvider(this).get(VideoViewModel.class);
        videos = new java.util.ArrayList<>();

        videoViewModel.getVideoList().observe(this, newVideos -> {
            boolean isRefreshing = binding.swipeRefreshPager.isRefreshing() || binding.swipeRefreshGrid.isRefreshing();
            videos.clear();
            videos.addAll(newVideos);
            
            // Ensure first video is different from current if refreshing
            if (isRefreshing && currentVideoUrl != null && !videos.isEmpty() && currentVideoUrl.equals(videos.get(0).videoUrl)) {
                 int swap = -1;
                 for (int i = 1; i < videos.size(); i++) {
                     if (!currentVideoUrl.equals(videos.get(i).videoUrl)) { swap = i; break; }
                 }
                 if (swap != -1) {
                     com.example.bytedance.model.VideoItem tmp = videos.get(0);
                     videos.set(0, videos.get(swap));
                     videos.set(swap, tmp);
                 }
            }
            
            if (binding.recyclerView.getAdapter() != null) binding.recyclerView.getAdapter().notifyDataSetChanged();
            if (playerAdapter != null) playerAdapter.notifyDataSetChanged();

            binding.swipeRefreshGrid.setRefreshing(false);
            binding.swipeRefreshPager.setRefreshing(false);

            if (isRefreshing && !isTwoColumn) {
                 binding.viewPager.setCurrentItem(0, false);
                 binding.viewPager.post(() -> {
                     switchToVideo(0);
                     attachProgressForPosition(0);
                 });
            }
        });
        videoViewModel.loadVideos();

        VideoAdapter videoAdapter = new VideoAdapter(videos);
        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(isTwoColumn ? 2 : 1, RecyclerView.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        binding.recyclerView.setItemAnimator(null);
        binding.recyclerView.setLayoutManager(sglm);
        binding.recyclerView.setAdapter(videoAdapter);
        // 下拉刷新（双列模式）
        binding.swipeRefreshGrid.setOnRefreshListener(() -> {
            refreshVideos();
            binding.swipeRefreshGrid.setRefreshing(false);
        });
        // 上拉加载更多（双列模式）
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
                // 只有滚动到顶时允许下拉刷新，避免瀑布流顶部不在 0 时无法触发
                boolean atTop = recyclerView.computeVerticalScrollOffset() == 0;
                binding.swipeRefreshGrid.setEnabled(atTop);
                if (dy <= 0) return;
                RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
                if (lm instanceof StaggeredGridLayoutManager) {
                    StaggeredGridLayoutManager s = (StaggeredGridLayoutManager) lm;
                    int[] lastPos = s.findLastVisibleItemPositions(null);
                    int last = 0;
                    for (int p : lastPos) { if (p > last) last = p; }
                    if (last >= videos.size() - 2) { loadMoreVideos(); }
                }
            }
        });

        // 顶部导航：由 TabLayoutMediator 驱动，无需手动添加 Tab

        // 初始化缓存与预取
        CacheManager.init(getApplicationContext());
        java.util.ArrayList<String> urlsForPrefetch = new java.util.ArrayList<>();
        for (com.example.bytedance.model.VideoItem item : videos) { urlsForPrefetch.add(item.videoUrl); }
        CacheManager.prefetchTopN(urlsForPrefetch, 3); // 启动时预取前3个视频

        // 初始化内流播放器（使用缓存数据源）与适配器（主界面直接单列播放）
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(CacheManager.mediaSourceFactory(this))
                .setLoadControl(new DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                                30_000,  // minBufferMs：加大缓冲
                                300_000, // maxBufferMs：更大上限
                                500,     // bufferForPlaybackMs：快速起播
                                2_000    // bufferForPlaybackAfterRebufferMs
                        ).build())
                .build();
        playerAdapter = new VideoPlayerAdapter(videos);
        playerAdapter.setPlayer(player);
        binding.viewPager.setAdapter(playerAdapter);
        binding.viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        binding.viewPager.setOffscreenPageLimit(1);
        // 下拉刷新（单列模式）
        binding.swipeRefreshPager.setOnRefreshListener(() -> {
            refreshVideos();
            binding.swipeRefreshPager.setRefreshing(false);
        });

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switchToVideo(position);
                // 确保 ViewHolder 已附着后再绑定进度/点击
                binding.viewPager.post(() -> attachProgressForPosition(position));
                // 靠近末尾时，自动加载更多（单列模式）
                if (position >= videos.size() - 2) {
                    loadMoreVideos();
                }
                // 只有当前页在顶部时允许下拉刷新
                binding.swipeRefreshPager.setEnabled(!binding.viewPager.canScrollVertically(-1));
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    // 重新评估是否在顶部以启用下拉刷新
                    binding.swipeRefreshPager.setEnabled(!binding.viewPager.canScrollVertically(-1));
                }
            }
        });

        // 移除基于 Tab 的单双列切换监听与相关按钮
        if (binding.toggleButton != null) binding.toggleButton.setVisibility(android.view.View.GONE);
        binding.modeFab.setVisibility(android.view.View.GONE);
        updateTabTitle();

        // 点击封面进入内流并定位到对应视频
        videoAdapter.setOnVideoClickListener((position, thumbnailView) -> {
            if (isTwoColumn) {
                isTwoColumn = false;
                // 简单的缩放+淡出过渡
                thumbnailView.animate().scaleX(1.1f).scaleY(1.1f).setDuration(120).withEndAction(() -> {
                    binding.recyclerView.setVisibility(android.view.View.GONE);
                    binding.swipeRefreshGrid.setVisibility(android.view.View.GONE);
                    binding.viewPager.setAlpha(0f);
                    binding.swipeRefreshPager.setVisibility(android.view.View.VISIBLE);
                    binding.viewPager.setVisibility(android.view.View.VISIBLE);
                    binding.viewPager.animate().alpha(1f).setDuration(150).start();
                    binding.modeFab.setVisibility(android.view.View.GONE);
                    if (binding.toggleButton != null) binding.toggleButton.setVisibility(android.view.View.GONE);
                    // 等 ViewPager2 可见并完成布局后再跳转并绑定，避免末尾项卡加载
                    binding.viewPager.post(() -> {
                        binding.viewPager.setCurrentItem(position, false);
                        switchToVideo(position);
                        attachProgressForPosition(position);
                    });
                    updateTabTitle();
                }).start();
            } else {
                // 已在单列时，仅跳转到该项
                binding.viewPager.setCurrentItem(position, true);
            }
        });

        binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_menu);
        // 默认选中 Home 并进行缩放效果
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_home);
        animateBottomItemSelected(R.id.navigation_home);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            animateBottomItemSelected(id);
            if (id == R.id.navigation_profile) {
                android.content.SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
                boolean loggedIn = sp.getBoolean("logged_in", false);
                android.content.Intent intent = new android.content.Intent(
                        MainActivity.this,
                        loggedIn ? com.example.bytedance.ui.ProfileActivity.class : com.example.bytedance.ui.LoginActivity.class
                );
                startActivity(intent);
                return true; // 保持选中态以显示缩放
            }
            // 其他导航项目前不跳转，但保留选中态并显示缩放
            return true;
        });
    }

    private void animateBottomItemSelected(int itemId) {
        try {
            android.view.View child0 = binding.bottomNavigation.getChildAt(0);
            if (!(child0 instanceof android.view.ViewGroup)) return;
            android.view.ViewGroup menuView = (android.view.ViewGroup) child0;

            // 通过菜单顺序找到选中项索引，避免使用受限的内部 API
            android.view.Menu menu = binding.bottomNavigation.getMenu();
            int targetIndex = -1;
            for (int i = 0; i < menu.size(); i++) {
                android.view.MenuItem mi = menu.getItem(i);
                if (mi != null && mi.getItemId() == itemId) { targetIndex = i; break; }
            }

            // 重置所有项缩放
            for (int i = 0; i < menuView.getChildCount(); i++) {
                android.view.View itemView = menuView.getChildAt(i);
                if (itemView != null) itemView.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }

            // 放大目标项
            if (targetIndex >= 0 && targetIndex < menuView.getChildCount()) {
                android.view.View target = menuView.getChildAt(targetIndex);
                if (target != null) target.animate().scaleX(1.12f).scaleY(1.12f).setDuration(120).start();
            }
        } catch (Exception ignored) {}
    }

    private void switchToVideo(int position) {
        if (position < 0 || position >= videos.size()) return;
        player.stop();
        MediaItem mediaItem = MediaItem.fromUri(videos.get(position).videoUrl);
        player.setMediaItem(mediaItem);
        currentVideoUrl = videos.get(position).videoUrl;
        // 重建/重置预览检索器并清空缓存
        synchronized (MainActivity.this) {
            try {
                if (previewRetriever != null) previewRetriever.release();
            } catch (Exception ignored) {}
            previewRetriever = null;
            thumbCache.evictAll();
            if (thumbFuture != null) {
                try { thumbFuture.cancel(true); } catch (Exception ignored) {}
                thumbFuture = null;
            }
        }
        player.prepare();
        player.setPlayWhenReady(true);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
    }

    private void startThumbPreGen(String url, long durationMs) {
        if (thumbFuture != null) {
            try { thumbFuture.cancel(true); } catch (Exception ignored) {}
            thumbFuture = null;
        }
        final long capMs = Math.min(durationMs, 45_000L);
        final int maxFrames = Math.min(80, (int) (capMs / 1000L));
        thumbFuture = thumbExecutor.submit(() -> {
            MediaMetadataRetriever local = null;
            try {
                local = new MediaMetadataRetriever();
                java.util.HashMap<String, String> headers = new java.util.HashMap<>();
                local.setDataSource(url, headers);
                int cached = 0;
                for (long bucket = 0; bucket <= capMs && cached < maxFrames; bucket += 1000L) {
                    if (java.lang.Thread.currentThread().isInterrupted()) break;
                    String key = url + "@" + bucket;
                    Bitmap hit = thumbCache.get(key);
                    if (hit == null) {
                        Bitmap bmp = local.getFrameAtTime(bucket * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        if (bmp != null) {
                            int w = bmp.getWidth();
                            int h = bmp.getHeight();
                            int targetW = 120;
                            if (w > 0 && h > 0 && targetW > 0 && targetW < w) {
                                int targetH = (int) ((long) h * targetW / w);
                                bmp = android.graphics.Bitmap.createScaledBitmap(bmp, targetW, Math.max(1, targetH), false);
                            }
                            thumbCache.put(key, bmp);
                            cached++;
                        }
                    }
                    try { java.lang.Thread.sleep(15); } catch (InterruptedException ie) { break; }
                }
            } catch (Exception ignored) {
            } finally {
                if (local != null) { try { local.release(); } catch (Exception ignored) {} }
            }
        });
    }

    private void attachProgressForPosition(int position) {
        RecyclerView rv = (RecyclerView) binding.viewPager.getChildAt(0);
        if (rv == null) {
            binding.viewPager.post(() -> attachProgressForPosition(position));
            return;
        }
        RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(position);
        if (vh == null) {
            // ViewHolder might not be ready yet, retry after a short delay
            binding.viewPager.postDelayed(() -> attachProgressForPosition(position), 50);
            return;
        }
        if (vh instanceof com.example.bytedance.adapter.VideoPlayerAdapter.VideoPlayerViewHolder) {
            ItemVideoPlayerBinding itemBinding = ((com.example.bytedance.adapter.VideoPlayerAdapter.VideoPlayerViewHolder) vh).getBinding();
            // 先解绑旧视图，避免多个视图竞争同一 Player 导致黑屏
            com.google.android.exoplayer2.ui.PlayerView oldPv = currentPlayerViewRef != null ? currentPlayerViewRef.get() : null;
            // 优化：如果新旧视图相同，不需要解绑再绑定
            if (oldPv != null && oldPv != itemBinding.playerView) {
                oldPv.setPlayer(null);
            }

            // 强制重置 Player 以确保 Surface 正确绑定（解决滑动返回时有声音无画面/进度条动无画面问题）
            if (itemBinding.playerView.getPlayer() == player) {
                itemBinding.playerView.setPlayer(null);
            }

            // 绑定新视图与进度
            currentProgressRef = new WeakReference<>(itemBinding.progressBar);
            currentPlayerViewRef = new WeakReference<>(itemBinding.playerView);
            currentSeekBarRef = new WeakReference<>(itemBinding.seekBar);
            currentPauseOverlayRef = new WeakReference<>(itemBinding.pauseOverlay);
            currentTimeTextRef = new WeakReference<>(itemBinding.currentTimeText);
            totalTimeTextRef = new WeakReference<>(itemBinding.totalTimeText);
            currentThumbPreviewRef = new WeakReference<>(itemBinding.thumbnailPreview);
            currentDescriptionRef = new WeakReference<>(itemBinding.descriptionText);
            likeAnimRef = new WeakReference<>(itemBinding.likeAnim);
            musicDiscRef = new WeakReference<>(itemBinding.musicDisc);
            authorAvatarRef = new WeakReference<>(itemBinding.authorAvatar);
            followButtonRef = new WeakReference<>(itemBinding.followButton);
            authorNameTextRef = new WeakReference<>(itemBinding.authorNameText);
            // 默认不显示时间文本，避免与进度条重叠
            if (itemBinding.currentTimeText != null) itemBinding.currentTimeText.setVisibility(android.view.View.GONE);
            if (itemBinding.totalTimeText != null) itemBinding.totalTimeText.setVisibility(android.view.View.GONE);

            // 右侧操作区：点赞/评论/分享
            ImageButton likeBtn = itemBinding.likeButton;
            ImageButton commentBtn = itemBinding.commentButton;
            ImageButton shareBtn = itemBinding.shareButton;
            TextView likeCountText = itemBinding.likeCountText;
            TextView commentCountText = itemBinding.commentCountText;
            TextView shareCountText = itemBinding.shareCountText;
            // 作者名绑定与描述可展开
            if (authorNameTextRef != null) {
                TextView tv = authorNameTextRef.get();
                if (tv != null && position >= 0 && position < videos.size()) {
                    String author = videos.get(position).author;
                    tv.setText("@" + (author == null ? "作者" : author));
                }
            }
            TextView desc = currentDescriptionRef != null ? currentDescriptionRef.get() : null;
            if (desc != null && position >= 0 && position < videos.size()) {
                String full = videos.get(position).description == null ? "" : videos.get(position).description;
                applyExpandableDescription(desc, full);
            }
            if (likeBtn != null && currentVideoUrl != null) {
                final String url = currentVideoUrl;
                if (!likeCountMap.containsKey(url)) likeCountMap.put(url, 1000 + rand.nextInt(9000));
                if (!likedMap.containsKey(url)) likedMap.put(url, false);
                likeBtn.setColorFilter(Boolean.TRUE.equals(likedMap.get(url)) ? ContextCompat.getColor(this, R.color.douyinIconLiked) : ContextCompat.getColor(this, R.color.douyinIcon));
                if (likeCountText != null) likeCountText.setText(formatCount(likeCountMap.get(url)));
                likeBtn.setOnClickListener(v -> {
                    boolean liked = Boolean.TRUE.equals(likedMap.get(url));
                    likedMap.put(url, !liked);
                    int count = likeCountMap.getOrDefault(url, 0);
                    likeCountMap.put(url, liked ? Math.max(0, count - 1) : count + 1);
                    likeBtn.setColorFilter(!liked ? ContextCompat.getColor(this, R.color.douyinIconLiked) : ContextCompat.getColor(this, R.color.douyinIcon));
                    if (likeCountText != null) likeCountText.setText(formatCount(likeCountMap.get(url)));
                });
            }
            if (commentBtn != null && currentVideoUrl != null) {
                final String url = currentVideoUrl;
                if (!commentCountMap.containsKey(url)) commentCountMap.put(url, 100 + rand.nextInt(9000));
                if (commentCountText != null) commentCountText.setText(formatCount(commentCountMap.get(url)));
                commentBtn.setOnClickListener(v -> {
                    try {
                        com.example.bytedance.ui.CommentPanelFragment commentPanelFragment = new com.example.bytedance.ui.CommentPanelFragment();
                        commentPanelFragment.show(getSupportFragmentManager(), "comment_panel");
                    } catch (Exception e) {
                        android.widget.Toast.makeText(this, "评论面板打开失败", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
            if (shareBtn != null && currentVideoUrl != null) {
                final String url = currentVideoUrl;
                shareBtn.setOnClickListener(v -> {
                    try {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                        String title = currentDescriptionRef != null && currentDescriptionRef.get() != null
                                ? currentDescriptionRef.get().getText().toString() : "分享视频";
                        android.net.Uri stream = getShareableThumbUri();
                        if (stream != null) {
                            intent.setType("image/*");
                            intent.putExtra(android.content.Intent.EXTRA_STREAM, stream);
                            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } else {
                            intent.setType("text/plain");
                        }
                        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, title);
                        intent.putExtra(android.content.Intent.EXTRA_TEXT, title + "\n" + currentVideoUrl);
                        startActivity(android.content.Intent.createChooser(intent, "分享视频"));
                        shareCountMap.put(url, shareCountMap.getOrDefault(url, 0) + 1);
                        if (shareCountText != null) shareCountText.setText(formatCount(shareCountMap.get(url)));
                    } catch (Exception ignored) {}
                });
                if (!shareCountMap.containsKey(url)) shareCountMap.put(url, 50 + rand.nextInt(500));
                if (shareCountText != null) shareCountText.setText(formatCount(shareCountMap.get(url)));
            }

            com.google.android.exoplayer2.ui.PlayerView pv = currentPlayerViewRef.get();
            if (pv != null) {
                pv.setUseArtwork(false);
                pv.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT);
                pv.setResizeMode(com.google.android.exoplayer2.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                pv.setKeepContentOnPlayerReset(false);
                pv.setPlayer(player);
                // 单击播放/暂停 + 双击点赞
                GestureDetector detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        if (player.isPlaying()) player.pause(); else player.play();
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (currentVideoUrl == null) return true;
                        boolean liked = Boolean.TRUE.equals(likedMap.get(currentVideoUrl));
                        likedMap.put(currentVideoUrl, true);
                        likeCountMap.put(currentVideoUrl, likeCountMap.getOrDefault(currentVideoUrl, 0) + (liked ? 0 : 1));
                        if (likeCountText != null) likeCountText.setText(formatCount(likeCountMap.get(currentVideoUrl)));
                        if (likeBtn != null) likeBtn.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.douyinIconLiked));
                        showHeartAnim();
                        return true;
                    }
                });
                pv.setOnTouchListener((v, event) -> detector.onTouchEvent(event));
            }

            // 设置进度条拖动
            SeekBar sb = currentSeekBarRef.get();
            if (sb != null) {
                sb.setMax(1000);
                // 未拖动时降低透明度，形成“虚化”效果
                sb.setAlpha(0.6f);
                sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && player.getDuration() > 0 && player.getDuration() != com.google.android.exoplayer2.C.TIME_UNSET) {
                            long seekTo = player.getDuration() * progress / 1000;
                            player.seekTo(seekTo);
                            updateThumbPreview(seekTo);
                            TextView ct = currentTimeTextRef != null ? currentTimeTextRef.get() : null;
                            TextView tt = totalTimeTextRef != null ? totalTimeTextRef.get() : null;
                            if (ct != null) ct.setText(formatTime(seekTo));
                            if (tt != null) tt.setText(formatTime(player.getDuration()));
                        }
                    }

                    @Override public void onStartTrackingTouch(SeekBar seekBar) {
                        // 拖动时提升不透明度
                        seekBar.setAlpha(1.0f);
                        TextView ct = currentTimeTextRef != null ? currentTimeTextRef.get() : null;
                        TextView tt = totalTimeTextRef != null ? totalTimeTextRef.get() : null;
                        ImageView thumb = currentThumbPreviewRef != null ? currentThumbPreviewRef.get() : null;
                        TextView desc = currentDescriptionRef != null ? currentDescriptionRef.get() : null;
                        if (ct != null) ct.setVisibility(android.view.View.VISIBLE);
                        if (tt != null) tt.setVisibility(android.view.View.VISIBLE);
                        if (desc != null) desc.setVisibility(android.view.View.GONE);
                        if (thumb != null) thumb.setVisibility(android.view.View.VISIBLE);
                        isSeeking = true;
                        // 暂停以便拖动预览帧更稳定
                        if (player.isPlaying()) player.pause();
                    }

                    @Override public void onStopTrackingTouch(SeekBar seekBar) {
                        // 松手后隐藏缩略图，恢复文本与继续播放
                        // 恢复“虚化”透明度
                        seekBar.setAlpha(0.6f);
                        TextView ct = currentTimeTextRef != null ? currentTimeTextRef.get() : null;
                        TextView tt = totalTimeTextRef != null ? totalTimeTextRef.get() : null;
                        ImageView thumb = currentThumbPreviewRef != null ? currentThumbPreviewRef.get() : null;
                        TextView desc = currentDescriptionRef != null ? currentDescriptionRef.get() : null;
                        if (thumb != null) thumb.setVisibility(android.view.View.GONE);
                        if (ct != null) ct.setVisibility(android.view.View.GONE);
                        if (tt != null) tt.setVisibility(android.view.View.GONE);
                        if (desc != null) desc.setVisibility(android.view.View.VISIBLE);
                        isSeeking = false;
                        // 依据最终进度进行一次精确定位，确保从拖动位置继续播放
                        long duration = player.getDuration();
                        if (duration > 0 && duration != com.google.android.exoplayer2.C.TIME_UNSET) {
                            int p = seekBar.getProgress();
                            long seekTo = duration * p / 1000;
                            player.seekTo(seekTo);
                        }
                        player.play();
                    }
                });
            }

            // 关注按钮逻辑（头像右下角 + 号）
            if (followButtonRef != null) {
                android.widget.ImageView fb = followButtonRef.get();
                android.widget.ImageView avatar = authorAvatarRef != null ? authorAvatarRef.get() : null;
                String author = position >= 0 && position < videos.size() ? videos.get(position).author : "作者";
                boolean followed = Boolean.TRUE.equals(followMap.get(author));
                if (fb != null) {
                    fb.setAlpha(followed ? 0.0f : 1.0f);
                    fb.setOnClickListener(v -> {
                        boolean f = Boolean.TRUE.equals(followMap.get(author));
                        followMap.put(author, !f);
                        fb.animate().alpha(!f ? 0.0f : 1.0f).setDuration(160).start();
                        android.widget.Toast.makeText(MainActivity.this, !f ? "已关注" : "已取消关注", android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
                if (avatar != null) {
                    avatar.setOnClickListener(v -> {
                        android.widget.Toast.makeText(MainActivity.this, "作者主页暂未实现", android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }
        // 全局缓冲监听（只更新当前项）
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                android.widget.ProgressBar pb = currentProgressRef != null ? currentProgressRef.get() : null;
                if (pb == null) return;
                if (state == Player.STATE_BUFFERING) {
                    pb.setVisibility(android.view.View.VISIBLE);
                } else {
                    pb.setVisibility(android.view.View.GONE);
                }
                if (state == Player.STATE_READY) {
                    long duration = player.getDuration();
                    if (duration > 0 && duration != com.google.android.exoplayer2.C.TIME_UNSET && currentVideoUrl != null) {
                        startThumbPreGen(currentVideoUrl, duration);
                    }
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                ImageView overlay = currentPauseOverlayRef != null ? currentPauseOverlayRef.get() : null;
                if (overlay != null) {
                    overlay.setVisibility(isPlaying ? android.view.View.GONE : android.view.View.VISIBLE);
                }
                if (isPlaying) {
                    progressHandler.removeCallbacks(progressRunnable);
                    progressHandler.post(progressRunnable);
                    startDiscRotate();
                } else {
                    progressHandler.removeCallbacks(progressRunnable);
                    stopDiscRotate();
                }
            }
        });
    }

    private void showHeartAnim() {
        android.widget.ImageView heart = likeAnimRef != null ? likeAnimRef.get() : null;
        if (heart == null) return;
        heart.setVisibility(android.view.View.VISIBLE);
        heart.setScaleX(0.1f);
        heart.setScaleY(0.1f);
        heart.setAlpha(0f);
        heart.animate().alpha(1f).scaleX(1.2f).scaleY(1.2f).setDuration(150)
                .withEndAction(() -> heart.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(250)
                        .withEndAction(() -> heart.setVisibility(android.view.View.GONE)).start()).start();
    }

    private void startDiscRotate() {
        android.widget.ImageView disc = musicDiscRef != null ? musicDiscRef.get() : null;
        if (disc == null) return;
        if (discRotateAnimator == null) {
            discRotateAnimator = ObjectAnimator.ofFloat(disc, "rotation", 0f, 360f);
            discRotateAnimator.setDuration(2000);
            discRotateAnimator.setInterpolator(new LinearInterpolator());
            discRotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        }
        if (!discRotateAnimator.isStarted()) discRotateAnimator.start();
    }

    private void stopDiscRotate() {
        if (discRotateAnimator != null && discRotateAnimator.isRunning()) {
            discRotateAnimator.cancel();
        }
    }

    // 将“展开”放到描述文本末尾，初始两行，超过两行时才显示
    private void applyExpandableDescription(TextView desc, String fullText) {
        if (desc == null) return;
        if (fullText == null) fullText = "";
        final String ft = fullText;
        // 初始为两行末尾省略
        desc.setMaxLines(2);
        desc.setEllipsize(TextUtils.TruncateAt.END);
        desc.setText(ft);
        desc.post(() -> {
            Layout layout = desc.getLayout();
            if (layout == null) return;
            int lineCount = layout.getLineCount();
            // 仅当文本超过两行或第二行被省略时显示“展开”
            boolean needExpand = lineCount > 2 || (lineCount >= 2 && layout.getEllipsisCount(1) > 0);
            if (!needExpand) {
                // 不需要展开，保持原文本即可
                return;
            }
            // 计算第二行末尾可见位置
            int end = layout.getLineEnd(Math.min(1, lineCount - 1));
            String indicator = "… 展开";
            int reserve = indicator.length();
            int cut = Math.max(0, end - reserve);
            String shown = ft.substring(0, Math.min(cut, ft.length())).trim();
            // 避免末尾已经是句号或省略
            if (shown.endsWith("…")) {
                indicator = " 展开"; // 已有省略则不重复
            }
            String display = shown + indicator;
            SpannableString ss = new SpannableString(display);
            int start = display.length() - ("展开").length();
            int endSpan = display.length();
            ss.setSpan(new ClickableSpan() {
                @Override public void onClick(android.view.View widget) {
                    showExpandedWithCollapse(desc, ft);
                }
                @Override public void updateDrawState(TextPaint ds) {
                    ds.setColor(Color.WHITE);
                    ds.setUnderlineText(false);
                }
            }, start, endSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            desc.setText(ss);
            desc.setMovementMethod(LinkMovementMethod.getInstance());
            desc.setHighlightColor(Color.TRANSPARENT);
        });
    }

    // 展开后在末尾追加“ 收起”可点击，点击后恢复两行省略
    private void showExpandedWithCollapse(TextView desc, String fullText) {
        if (desc == null) return;
        if (fullText == null) fullText = "";
        final String ft = fullText;
        String indicator = " 收起";
        String display = ft + indicator;
        SpannableString ss = new SpannableString(display);
        int start = display.length() - ("收起").length();
        int end = display.length();
        ss.setSpan(new ClickableSpan() {
            @Override public void onClick(android.view.View widget) {
                applyExpandableDescription(desc, ft);
            }
            @Override public void updateDrawState(TextPaint ds) {
                ds.setColor(Color.WHITE);
                ds.setUnderlineText(false);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // 展开显示全文，并在末尾添加“收起”链接
        desc.setMaxLines(Integer.MAX_VALUE);
        desc.setEllipsize(null);
        desc.setText(ss);
        desc.setMovementMethod(LinkMovementMethod.getInstance());
        desc.setHighlightColor(Color.TRANSPARENT);
    }

    private void updateTabTitle() {
        TabLayout.Tab tab = binding.tabLayout.getTabAt(0);
        if (tab != null) {
            tab.setText("推荐");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
        progressHandler.removeCallbacksAndMessages(null);
        // 释放预览帧资源与缓存，避免内存泄漏
        if (previewRetriever != null) {
            try { previewRetriever.release(); } catch (Exception ignored) {}
            previewRetriever = null;
        }
        if (thumbCache != null) {
            try { thumbCache.evictAll(); } catch (Exception ignored) {}
        }
        if (thumbFuture != null) {
            try { thumbFuture.cancel(true); } catch (Exception ignored) {}
            thumbFuture = null;
        }
        try { thumbExecutor.shutdownNow(); } catch (Exception ignored) {}
        currentProgressRef = null;
        currentPlayerViewRef = null;
        currentSeekBarRef = null;
        currentPauseOverlayRef = null;
        currentTimeTextRef = null;
        totalTimeTextRef = null;
        currentThumbPreviewRef = null;
        currentDescriptionRef = null;
    }

    // 刷新数据：委托给 ViewModel
    private void refreshVideos() {
        videoViewModel.refreshVideos();
    }

    // 加载更多：委托给 ViewModel
    private void loadMoreVideos() {
        if (isLoadingMore) return;
        isLoadingMore = true;
        videoViewModel.loadMoreVideos();
        // 简单的防抖延迟重置
        new Handler().postDelayed(() -> isLoadingMore = false, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 若未登录，确保默认选中 Home，避免 Mine 保持放大
        android.content.SharedPreferences sp = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean loggedIn = sp.getBoolean("logged_in", false);
        if (!loggedIn) {
            binding.bottomNavigation.setSelectedItemId(R.id.navigation_home);
            animateBottomItemSelected(R.id.navigation_home);
        }
    }
}
