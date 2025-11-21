package com.example.bytedance;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.bytedance.adapter.VideoPlayerAdapter;
import com.example.bytedance.data.MockData;
import com.example.bytedance.databinding.ActivityPlayerBinding;
import com.example.bytedance.databinding.ItemVideoPlayerBinding;
import com.example.bytedance.model.VideoItem;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private ExoPlayer player;
    private java.lang.ref.WeakReference<android.widget.ProgressBar> currentProgressRef;
    private java.lang.ref.WeakReference<com.google.android.exoplayer2.ui.PlayerView> currentPlayerViewRef;
    private java.util.List<VideoItem> videos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        player = new ExoPlayer.Builder(this).build();
        videos = MockData.getVideos();
        VideoPlayerAdapter adapter = new VideoPlayerAdapter(videos);
        adapter.setPlayer(player);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        binding.viewPager.setOffscreenPageLimit(1);

        // 播放当前选中页的视频
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switchToVideo(position);
                attachProgressForPosition(position);
            }
        });

        // 全局监听缓冲状态，更新当前页进度条
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
            }
        });

        // 初始播放第 0 项
        if (!videos.isEmpty()) {
            switchToVideo(0);
            attachProgressForPosition(0);
        }
    }

    private void switchToVideo(int position) {
        if (position < 0 || position >= videos.size()) return;
        player.stop();
        MediaItem mediaItem = MediaItem.fromUri(videos.get(position).videoUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    private void attachProgressForPosition(int position) {
        RecyclerView rv = (RecyclerView) binding.viewPager.getChildAt(0);
        RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(position);
        if (vh instanceof com.example.bytedance.adapter.VideoPlayerAdapter.VideoPlayerViewHolder) {
            ItemVideoPlayerBinding itemBinding = ((com.example.bytedance.adapter.VideoPlayerAdapter.VideoPlayerViewHolder) vh).getBinding();
            currentProgressRef = new java.lang.ref.WeakReference<>(itemBinding.progressBar);
            currentPlayerViewRef = new java.lang.ref.WeakReference<>(itemBinding.playerView);

            com.google.android.exoplayer2.ui.PlayerView pv = currentPlayerViewRef.get();
            if (pv != null) {
                pv.setOnClickListener(v -> {
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        player.play();
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}