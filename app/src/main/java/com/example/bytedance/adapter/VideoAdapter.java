package com.example.bytedance.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.bytedance.databinding.ItemVideoBinding;
import com.example.bytedance.model.VideoItem;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final List<VideoItem> videos;

    public VideoAdapter(List<VideoItem> videos) {
        this.videos = videos;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVideoBinding binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VideoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        holder.bind(videos.get(position));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    listener.onVideoClick(pos, holder.binding.thumbnail);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        private final ItemVideoBinding binding;

        public VideoViewHolder(ItemVideoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(VideoItem video) {
            // 文案与作者
            binding.description.setText(video.description);
            binding.author.setText(video.author);

            // 封面图（共享元素转场目标）
            Glide.with(binding.thumbnail.getContext())
                    .load(video.thumbnailUrl)
                    .placeholder(android.R.color.darker_gray)
                    .fitCenter()
                    .into(binding.thumbnail);

            // 头像（左下角圆形）
            if (video.avatarUrl != null) {
                Glide.with(binding.ivAvatar.getContext())
                        .load(video.avatarUrl)
                        .placeholder(android.R.color.darker_gray)
                        .circleCrop()
                        .into(binding.ivAvatar);
            }

            // 时长（mm:ss）
            long totalSec = Math.max(0, video.durationMs / 1000);
            long m = (totalSec % 3600) / 60;
            long s = totalSec % 60;
            binding.tvDuration.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s));

            // 点赞数
            binding.tvLikes.setText(String.valueOf(Math.max(0, video.likeCount)));
        }
    }

    public interface OnVideoClickListener {
        void onVideoClick(int position, View thumbnailView);
    }

    private OnVideoClickListener listener;

    public void setOnVideoClickListener(OnVideoClickListener listener) {
        this.listener = listener;
    }
}
