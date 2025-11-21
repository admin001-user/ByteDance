package com.example.bytedance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bytedance.databinding.ItemVideoPlayerBinding;
import com.example.bytedance.model.VideoItem;
import com.example.bytedance.ui.CommentPanelFragment;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

import java.util.List;

public class VideoPlayerAdapter extends RecyclerView.Adapter<VideoPlayerAdapter.VideoPlayerViewHolder> {

    private final List<VideoItem> videos;
    private ExoPlayer player;

    public VideoPlayerAdapter(List<VideoItem> videos) {
        this.videos = videos;
    }

    @NonNull
    @Override
    public VideoPlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVideoPlayerBinding binding = ItemVideoPlayerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VideoPlayerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoPlayerViewHolder holder, int position) {
        // 仅绑定数据，播放器绑定由外部控制，确保只有当前页持有播放器
        holder.bind(videos.get(position));
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public void setPlayer(ExoPlayer player) {
        this.player = player;
    }

    public static class VideoPlayerViewHolder extends RecyclerView.ViewHolder {
        private final ItemVideoPlayerBinding binding;

        public VideoPlayerViewHolder(ItemVideoPlayerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(VideoItem video) {
            // 不在此处切换媒体，避免离屏项影响当前播放
            binding.commentButton.setOnClickListener(v -> {
                CommentPanelFragment commentPanelFragment = new CommentPanelFragment();
                if (itemView.getContext() instanceof AppCompatActivity) {
                    AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                    commentPanelFragment.show(activity.getSupportFragmentManager(), commentPanelFragment.getTag());
                }
            });

            // 单列播放时显示描述文案
            binding.descriptionText.setText(video.description);
        }

        public ItemVideoPlayerBinding getBinding() {
            return binding;
        }
    }
}