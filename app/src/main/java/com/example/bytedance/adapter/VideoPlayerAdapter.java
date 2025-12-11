package com.example.bytedance.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.example.bytedance.R;
import com.example.bytedance.databinding.ItemVideoPlayerBinding;
import com.example.bytedance.model.VideoItem;
import com.example.bytedance.ui.CommentPanelFragment;
import com.google.android.exoplayer2.ExoPlayer;

import androidx.recyclerview.widget.DiffUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VideoPlayerAdapter extends RecyclerView.Adapter<VideoPlayerAdapter.VideoPlayerViewHolder> {

    private List<VideoItem> videos;
    private ExoPlayer player;
    private final Set<Integer> likedPositions = new HashSet<>();
    private final Set<Integer> followedPositions = new HashSet<>();
    private OnItemActionListener actionListener;

    public interface OnItemActionListener {
        void onLike(int position, VideoItem item, ItemVideoPlayerBinding binding);
        void onFollow(int position, VideoItem item, ItemVideoPlayerBinding binding);
        void onShare(int position, VideoItem item);
    }

    public VideoPlayerAdapter(List<VideoItem> videos) {
        this.videos = videos;
    }

    public void setActionListener(OnItemActionListener listener) {
        this.actionListener = listener;
    }

    public void setLikedPositions(Set<Integer> liked) {
        this.likedPositions.clear();
        this.likedPositions.addAll(liked);
    }

    public void setFollowedPositions(Set<Integer> followed) {
        this.followedPositions.clear();
        this.followedPositions.addAll(followed);
    }

    @NonNull
    @Override
    public VideoPlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVideoPlayerBinding binding = ItemVideoPlayerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VideoPlayerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoPlayerViewHolder holder, int position) {
        // Bind static data
        holder.bind(videos.get(position), position, likedPositions.contains(position), followedPositions.contains(position));
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public void setPlayer(ExoPlayer player) {
        this.player = player;
    }

    public void updateVideos(List<VideoItem> newVideos) {
        final VideoDiffCallback diffCallback = new VideoDiffCallback(this.videos, newVideos);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.videos.clear();
        this.videos.addAll(newVideos);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public void onViewRecycled(@NonNull VideoPlayerViewHolder holder) {
        super.onViewRecycled(holder);
        ItemVideoPlayerBinding b = holder.getBinding();
        if (b != null) {
            if (b.playerView != null) {
                b.playerView.setPlayer(null);
                b.playerView.setOnTouchListener(null);
            }
        }
    }

    public class VideoPlayerViewHolder extends RecyclerView.ViewHolder {
        private final ItemVideoPlayerBinding binding;

        public VideoPlayerViewHolder(ItemVideoPlayerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(VideoItem video, int position, boolean isLiked, boolean isFollowed) {
            // Description
            binding.descriptionText.setText(video.description);
            
            // Reset state
            binding.pauseOverlay.setVisibility(View.GONE);
            binding.pauseOverlay.setAlpha(0.85f); // Reset alpha in case animation changed it

            // Author
            binding.authorNameText.setText(video.author);
            Glide.with(itemView.getContext())
                    .load(video.avatarUrl)
                    .circleCrop()
                    .into(binding.authorAvatar);
            
            // Avatar Click -> Profile
            binding.authorAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), com.example.bytedance.ui.ProfileActivity.class);
                itemView.getContext().startActivity(intent);
            });

            // Cover/Background
            Glide.with(itemView.getContext())
                    .load(video.thumbnailUrl)
                    .transform(new CenterCrop())
                    .into(binding.bgArtwork);

            // Like Count & State
            binding.likeCountText.setText(formatCount(Math.max(0, video.likeCount)));
            // Comment Count
            binding.commentCountText.setText(formatCount(Math.max(0, video.commentCount)));

            if (isLiked) {
                binding.likeButton.setColorFilter(android.graphics.Color.RED);
            } else {
                binding.likeButton.setColorFilter(android.graphics.Color.WHITE);
            }
            binding.likeButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onLike(position, video, binding);
                }
            });

            // Follow State
            if (isFollowed) {
                binding.followButton.setVisibility(View.GONE);
            } else {
                binding.followButton.setVisibility(View.VISIBLE);
                binding.followButton.setImageResource(android.R.drawable.ic_input_add);
                binding.followButton.setColorFilter(android.graphics.Color.parseColor("#FF3B30"));
                binding.followButton.setBackgroundResource(R.drawable.circle_bg_white);
            }
            binding.followButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onFollow(position, video, binding);
                }
            });

            // Comment
            binding.commentButton.setOnClickListener(v -> {
                CommentPanelFragment commentPanelFragment = new CommentPanelFragment();
                if (itemView.getContext() instanceof AppCompatActivity) {
                    AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                    commentPanelFragment.show(activity.getSupportFragmentManager(), commentPanelFragment.getTag());
                }
            });

            // Share - System Share
            binding.shareButton.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this video: " + video.videoUrl);
                itemView.getContext().startActivity(Intent.createChooser(shareIntent, "Share via"));
                
                if (actionListener != null) {
                    actionListener.onShare(position, video);
                }
            });
        }

        public ItemVideoPlayerBinding getBinding() {
            return binding;
        }
    }

    private static class VideoDiffCallback extends DiffUtil.Callback {
        private final List<VideoItem> oldList;
        private final List<VideoItem> newList;

        public VideoDiffCallback(List<VideoItem> oldList, List<VideoItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).videoUrl.equals(newList.get(newItemPosition).videoUrl);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

    private String formatCount(int num) {
        if (num < 10000) return String.valueOf(num);
        int w = num / 10000;
        int remainder = (num % 10000) / 1000;
        if (remainder == 0) {
            return w + "W";
        }
        return w + "." + remainder + "W";
    }
}
