package com.example.bytedance;

import android.animation.ObjectAnimator;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import jp.wasabeef.glide.transformations.BlurTransformation;
import com.example.bytedance.adapter.VideoPlayerAdapter;
import com.example.bytedance.data.MockData;
import com.example.bytedance.databinding.ActivityPlayerBinding;
import com.example.bytedance.databinding.ItemVideoPlayerBinding;
import com.example.bytedance.model.VideoItem;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.video.VideoSize;

import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheWriter;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerActivity extends AppCompatActivity implements VideoPlayerAdapter.OnItemActionListener {

    private ActivityPlayerBinding binding;
    private ExoPlayer player;
    private VideoPlayerAdapter adapter;
    private final ExecutorService preloadExecutor = Executors.newFixedThreadPool(2);
    
    // WeakReferences to currently active view elements
    private WeakReference<ProgressBar> currentProgressRef;
    private WeakReference<PlayerView> currentPlayerViewRef;
    private WeakReference<ImageView> pauseOverlayRef;
    private WeakReference<ImageView> likeAnimRef;
    private WeakReference<SeekBar> seekBarRef;
    private WeakReference<TextView> currentTimeTextRef;
    private WeakReference<TextView> totalTimeTextRef;
    private WeakReference<ImageView> thumbnailPreviewRef;
    private WeakReference<ImageView> musicDiscRef;
    private WeakReference<ImageView> bgArtworkRef;

    private List<VideoItem> videos;
    private final Set<Integer> likedPositions = new HashSet<>();
    private final Set<Integer> followedPositions = new HashSet<>();
    private int currentPosition = 0;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isLoadingMore = false;
    private boolean isDragging = false; // Flag to check if user is dragging seekbar

    private final Runnable progressUpdater = new Runnable() {
        @Override public void run() {
            // If dragging, don't update seekbar to avoid conflict
            if (isDragging) {
                uiHandler.postDelayed(this, 250);
                return;
            }
            
            SeekBar sb = seekBarRef != null ? seekBarRef.get() : null;
            TextView curT = currentTimeTextRef != null ? currentTimeTextRef.get() : null;
            TextView totT = totalTimeTextRef != null ? totalTimeTextRef.get() : null;
            if (sb != null && player != null) {
                long dur = player.getDuration();
                long pos = player.getCurrentPosition();
                if (dur > 0) {
                    int progress = (int) (pos * 1000 / dur);
                    sb.setProgress(progress);
                    if (curT != null) curT.setText(msToText(pos));
                    if (totT != null) totT.setText(msToText(dur));
                }
            }
            uiHandler.postDelayed(this, 250);
        }
    };

    private int currentCategory = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Get category from intent
        currentCategory = getIntent().getIntExtra("category", 0);

        // Configure CacheDataSource
        SimpleCache simpleCache = ByteDanceApplication.simpleCache;
        DataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(new DefaultHttpDataSource.Factory())
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        
        // Build ExoPlayer with Cache
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(cacheDataSourceFactory))
                .build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        videos = new ArrayList<>(MockData.getVideos(currentCategory));
        
        adapter = new VideoPlayerAdapter(videos);
        adapter.setPlayer(player);
        adapter.setActionListener(this);
        adapter.setLikedPositions(likedPositions);
        adapter.setFollowedPositions(followedPositions);
        
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        binding.viewPager.setOffscreenPageLimit(1);

        // 单页下拉刷新：仅在位于顶部时可用
        binding.swipeRefreshPager.setEnabled(true);
        binding.swipeRefreshPager.setOnRefreshListener(() -> {
            if (videos.isEmpty()) {
                binding.swipeRefreshPager.setRefreshing(false);
                return;
            }
            VideoItem currentVideoBeforeRefresh = videos.get(currentPosition);

            List<VideoItem> refreshed = MockData.getVideos(currentCategory);
            if (refreshed != null && !refreshed.isEmpty()) {
                videos = new ArrayList<>(refreshed);
                adapter.updateVideos(refreshed);

                List<MediaItem> mediaItems = new ArrayList<>();
                for (VideoItem v : videos) mediaItems.add(MediaItem.fromUri(v.videoUrl));
                player.setMediaItems(mediaItems);
                player.prepare();

                int nextPosition = 0;
                if (videos.size() > 1 && videos.get(0).equals(currentVideoBeforeRefresh)) {
                    nextPosition = 1;
                }

                currentPosition = nextPosition;
                binding.viewPager.setCurrentItem(currentPosition, false);
                player.seekTo(currentPosition, 0);
                player.play();
                attachProgressForPosition(currentPosition);
            }
            binding.swipeRefreshPager.setRefreshing(false);
        });

        // 读取列表传入的起始位置
        int startPos = getIntent() != null ? getIntent().getIntExtra("start_position", 0) : 0;
        if (startPos < 0) startPos = 0;
        if (startPos >= videos.size()) startPos = Math.max(0, videos.size() - 1);

        // 播放当前选中页的视频
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                
                // Safety check: Ensure position is valid for the player
                if (position >= 0 && position < player.getMediaItemCount()) {
                    player.seekTo(position, 0);
                    player.play();
                }
                
                // 尝试立即附着视图，避免黑屏
                if (!attachProgressForPosition(position)) {
                     // Retry if failed (e.g. view not ready)
                     binding.viewPager.postDelayed(() -> attachProgressForPosition(position), 100);
                }
                
                // 预加载后续视频 (Disable temporarily to prevent potential freeze/crash issues)
                // preloadNextVideos(position);

                // 顶部才允许下拉刷新
                binding.swipeRefreshPager.setEnabled(!binding.viewPager.canScrollVertically(-1));
                // 接近底部时自动加载更多 (Preload threshold)
                if (position >= videos.size() - 3) {
                    tryLoadMore();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    attachProgressForPosition(currentPosition);
                    binding.swipeRefreshPager.setEnabled(!binding.viewPager.canScrollVertically(-1));
                }
            }
        });

        // 全局监听缓冲状态，更新当前页进度条
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                ProgressBar pb = currentProgressRef != null ? currentProgressRef.get() : null;
                if (pb == null) return;
                if (state == Player.STATE_BUFFERING) {
                    pb.setVisibility(View.VISIBLE);
                } else {
                    pb.setVisibility(View.GONE);
                }
            }
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                ImageView disc = musicDiscRef != null ? musicDiscRef.get() : null;
                if (disc != null) {
                    ObjectAnimator anim = (ObjectAnimator) disc.getTag(R.id.tag_rotation_anim);
                    if (anim == null) {
                        anim = ObjectAnimator.ofFloat(disc, View.ROTATION, 0f, 360f);
                        anim.setDuration(4000);
                        anim.setRepeatCount(ObjectAnimator.INFINITE);
                        anim.setRepeatMode(ObjectAnimator.RESTART);
                        disc.setTag(R.id.tag_rotation_anim, anim);
                    }
                    if (isPlaying) anim.start(); else anim.pause();
                }
            }
            @Override
            public void onVideoSizeChanged(VideoSize videoSize) {
                PlayerView pv = currentPlayerViewRef != null ? currentPlayerViewRef.get() : null;
                ImageView bg = bgArtworkRef != null ? bgArtworkRef.get() : null;
                if (pv == null) return;
                float videoRatio = videoSize.width > 0 && videoSize.height > 0 ? (float) videoSize.width / videoSize.height : 1f;
                DisplayMetrics dm = getResources().getDisplayMetrics();
                float screenRatio = (float) dm.widthPixels / dm.heightPixels;
                boolean canFill = Math.abs(videoRatio - screenRatio) < 0.12f;
                pv.setResizeMode(canFill ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM : AspectRatioFrameLayout.RESIZE_MODE_FIT);
                
                // Don't hide artwork here; wait for first frame
                if (bg != null && !canFill) {
                    bg.setVisibility(View.VISIBLE);
                    if (android.os.Build.VERSION.SDK_INT >= 31) {
                        bg.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.CLAMP));
                    } else {
                        Glide.with(bg.getContext())
                                .load(videos.get(currentPosition).thumbnailUrl)
                                .transform(new CenterCrop(), new BlurTransformation(20, 4))
                                .into(bg);
                    }
                }
            }

            @Override
            public void onRenderedFirstFrame() {
                ImageView bg = bgArtworkRef != null ? bgArtworkRef.get() : null;
                if (bg != null) {
                    bg.setVisibility(View.GONE);
                }
            }
        });

        // 初始定位到传入的起始项并开始播放
        if (!videos.isEmpty()) {
            // 初始化播放列表
            List<MediaItem> mediaItems = new ArrayList<>();
            for (VideoItem v : videos) {
                mediaItems.add(MediaItem.fromUri(v.videoUrl));
            }
            player.setMediaItems(mediaItems);
            player.prepare();

            binding.viewPager.setCurrentItem(startPos, false);
            currentPosition = startPos;
            player.seekTo(startPos, 0);
            player.play();
            
            // 延迟一点附着，确保View已创建
            int finalStartPos = startPos;
            binding.viewPager.post(() -> attachProgressForPosition(finalStartPos));
            binding.swipeRefreshPager.setEnabled(!binding.viewPager.canScrollVertically(-1));
        }
    }

    private void tryLoadMore() {
        if (isLoadingMore) return;
        isLoadingMore = true;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<VideoItem> more = MockData.getVideos(currentCategory);
                if (more != null && !more.isEmpty()) {
                    List<VideoItem> newVideos = new ArrayList<>();
                    List<MediaItem> newMediaItems = new ArrayList<>();
                    for (VideoItem newItem : more) {
                        if (!videos.contains(newItem)) {
                            newVideos.add(newItem);
                            newMediaItems.add(MediaItem.fromUri(newItem.videoUrl));
                        }
                    }

                    if (!newVideos.isEmpty()) {
                        uiHandler.post(() -> {
                            int oldSize = videos.size();
                            videos.addAll(newVideos);
                            adapter.notifyItemRangeInserted(oldSize, newVideos.size());
                            player.addMediaItems(newMediaItems);
                        });
                    }
                }
            } finally {
                isLoadingMore = false;
            }
        });
    }

    // switchToVideo 已废弃，逻辑整合到 onPageSelected 中


    private void preloadNextVideos(int currentPos) {
        // Preload next 3 videos
        for (int i = 1; i <= 3; i++) {
            int targetIndex = currentPos + i;
            if (targetIndex < videos.size()) {
                preloadVideo(videos.get(targetIndex).videoUrl);
            }
        }
    }

    private void preloadVideo(String url) {
        preloadExecutor.execute(() -> {
            try {
                SimpleCache simpleCache = ByteDanceApplication.simpleCache;
                if (simpleCache == null) return;
                
                android.net.Uri uri = android.net.Uri.parse(url);
                DataSpec dataSpec = new DataSpec.Builder()
                        .setUri(uri)
                        .setLength(2 * 1024 * 1024) // Preload 2MB
                        .build();
                
                DataSource dataSource = new DefaultHttpDataSource.Factory().createDataSource();
                CacheWriter cacheWriter = new CacheWriter(
                        new CacheDataSource(simpleCache, dataSource),
                        dataSpec,
                        null,
                        (requestLength, bytesCached, newBytesCached) -> {
                            // Progress callback if needed
                        }
                );
                cacheWriter.cache();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean attachProgressForPosition(int position) {
        RecyclerView rv = (RecyclerView) binding.viewPager.getChildAt(0);
        if (rv == null) return false;
        RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(position);
        if (vh == null) {
            // 视图尚未附着，稍后重试。避免无限递归，限制次数或仅在ScrollIdle时尝试更好。
            // 这里为了简单，仅延时一次。若失败则依赖 onPageScrollStateChanged(IDLE)
            return false;
        }
        if (vh instanceof VideoPlayerAdapter.VideoPlayerViewHolder) {
            ItemVideoPlayerBinding itemBinding = ((VideoPlayerAdapter.VideoPlayerViewHolder) vh).getBinding();
            currentProgressRef = new WeakReference<>(itemBinding.progressBar);
            currentPlayerViewRef = new WeakReference<>(itemBinding.playerView);
            pauseOverlayRef = new WeakReference<>(itemBinding.pauseOverlay);
            likeAnimRef = new WeakReference<>(itemBinding.likeAnim);
            seekBarRef = new WeakReference<>(itemBinding.seekBar);
            currentTimeTextRef = new WeakReference<>(itemBinding.currentTimeText);
            totalTimeTextRef = new WeakReference<>(itemBinding.totalTimeText);
            thumbnailPreviewRef = new WeakReference<>(itemBinding.thumbnailPreview);
            musicDiscRef = new WeakReference<>(itemBinding.musicDisc);
            bgArtworkRef = new WeakReference<>(itemBinding.bgArtwork);

            PlayerView pv = currentPlayerViewRef.get();
            if (pv != null) {
                // Force detach first to ensure clean state, solving the issue where progress moves but video doesn't play
                pv.setPlayer(null);
                
                // Ensure artwork is visible before video renders to prevent black screen
                ImageView bg = bgArtworkRef != null ? bgArtworkRef.get() : null;
                if (bg != null) {
                    bg.setVisibility(View.VISIBLE);
                    bg.setAlpha(1.0f); // Reset alpha if it was faded out
                }

                // 绑定播放器到当前页的 PlayerView
                pv.setPlayer(player);

                // 手势：单击暂停/播放并显示暂停图标；双击点赞心形动画
                GestureDetector gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        ImageView overlay = pauseOverlayRef != null ? pauseOverlayRef.get() : null;
                        if (player.isPlaying()) {
                            player.pause();
                            if (overlay != null) {
                                overlay.setAlpha(0f);
                                overlay.setVisibility(View.VISIBLE);
                                overlay.animate().alpha(0.85f).setDuration(120).start();
                            }
                        } else {
                            player.play();
                            if (overlay != null) {
                                overlay.animate().alpha(0f).setDuration(120).withEndAction(() -> overlay.setVisibility(View.GONE)).start();
                            }
                        }
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        toggleLike(position, itemBinding);
                        ImageView heart = likeAnimRef != null ? likeAnimRef.get() : null;
                        if (heart != null) {
                            heart.setScaleX(0.6f); heart.setScaleY(0.6f);
                            heart.setAlpha(0f);
                            heart.setVisibility(View.VISIBLE);
                            heart.animate()
                                    .alpha(1f)
                                    .scaleX(1.3f)
                                    .scaleY(1.3f)
                                    .setDuration(160)
                                    .withEndAction(() -> heart.animate().alpha(0f).setDuration(200).withEndAction(() -> heart.setVisibility(View.GONE)).start())
                                    .start();
                        }
                        return true;
                    }
                });
                pv.setOnTouchListener((view, event) -> gd.onTouchEvent(event));
            }

            // 进度条显示与更新
            TextView curT = currentTimeTextRef.get();
            TextView totT = totalTimeTextRef.get();
            if (curT != null) curT.setVisibility(View.VISIBLE);
            if (totT != null) totT.setVisibility(View.VISIBLE);
            uiHandler.removeCallbacks(progressUpdater);
            uiHandler.post(progressUpdater);

            SeekBar sb = seekBarRef.get();
            if (sb != null) {
                sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    boolean wasPlaying = false;
                    Runnable pendingSeekTask;

                    @Override public void onStartTrackingTouch(SeekBar seekBar) {
                        isDragging = true;
                        if (seekBar.getParent() != null) {
                            seekBar.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        wasPlaying = player.isPlaying();
                        player.pause();
                        ImageView thumbPrev = thumbnailPreviewRef.get();
                        if (thumbPrev != null) thumbPrev.setVisibility(View.VISIBLE);
                    }
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {
                        isDragging = false;
                        if (seekBar.getParent() != null) {
                            seekBar.getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        long dur = player.getDuration();
                        long target = dur * seekBar.getProgress() / 1000;
                        player.seekTo(target);
                        ImageView thumbPrev = thumbnailPreviewRef.get();
                        if (thumbPrev != null) thumbPrev.setVisibility(View.GONE);
                        player.play();
                    }
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (!fromUser) return;
                        long dur = player.getDuration();
                        long target = dur * progress / 1000;
                        
                        // 使用 Glide 加载预览帧 (Optimized with Debounce)
                        ImageView thumbPrev = thumbnailPreviewRef.get();
                        if (thumbPrev == null) return;
                        
                        if (pendingSeekTask != null) {
                            uiHandler.removeCallbacks(pendingSeekTask);
                        }
                        
                        pendingSeekTask = () -> {
                            Glide.with(PlayerActivity.this)
                                    .asBitmap()
                                    .load(videos.get(currentPosition).videoUrl)
                                    .frame(target * 1000) // microsecond
                                    .centerCrop()
                                    .into(thumbPrev);
                        };
                        uiHandler.postDelayed(pendingSeekTask, 50);
                    }
                });
            }
        }
        return true;
    }

    @Override
    public void onLike(int position, VideoItem item, ItemVideoPlayerBinding binding) {
        toggleLike(position, binding);
    }

    @Override
    public void onFollow(int position, VideoItem item, ItemVideoPlayerBinding binding) {
        toggleFollow(position, binding);
    }

    @Override
    public void onShare(int position, VideoItem item) {
        // Shared handled in adapter, but we can do extra tracking here if needed
    }

    private void toggleLike(int position, ItemVideoPlayerBinding itemBinding) {
        boolean liked = likedPositions.contains(position);
        int count = Math.max(0, videos.get(position).likeCount);
        if (liked) {
            likedPositions.remove(position);
            count = Math.max(0, count - 1);
            itemBinding.likeButton.setColorFilter(android.graphics.Color.WHITE);
        } else {
            likedPositions.add(position);
            count = count + 1;
            itemBinding.likeButton.setColorFilter(android.graphics.Color.RED);
        }
        videos.get(position).likeCount = count;
        itemBinding.likeCountText.setText(formatCount(count));
        
        // Sync with adapter
        adapter.setLikedPositions(likedPositions);
    }

    private void toggleFollow(int position, ItemVideoPlayerBinding itemBinding) {
        boolean followed = followedPositions.contains(position);
        if (followed) {
            followedPositions.remove(position);
            itemBinding.followButton.setVisibility(View.VISIBLE);
            itemBinding.followButton.setImageResource(android.R.drawable.ic_input_add);
            itemBinding.followButton.setColorFilter(android.graphics.Color.parseColor("#FF3B30")); // Red
            itemBinding.followButton.setBackgroundResource(R.drawable.circle_bg_white);
        } else {
            followedPositions.add(position);
            // Change to checkmark first
            itemBinding.followButton.setImageResource(R.drawable.ic_check_circle); // Need check icon
            itemBinding.followButton.setColorFilter(android.graphics.Color.GREEN);
            itemBinding.followButton.setBackground(null);
            
            // Animate and hide
            itemBinding.followButton.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                         itemBinding.followButton.setVisibility(View.GONE);
                         itemBinding.followButton.setScaleX(1f);
                         itemBinding.followButton.setScaleY(1f);
                    })
                    .start();
        }
        // Sync with adapter
        adapter.setFollowedPositions(followedPositions);
    }

    private String formatCount(int num) {
        if (num < 10000) return String.valueOf(num);
        int w = num / 10000;
        int remainder = (num % 10000) / 1000;
        return w + "." + remainder + "W";
    }

    private String msToText(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}
