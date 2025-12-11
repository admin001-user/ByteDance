package com.example.bytedance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bytedance.R;
import com.example.bytedance.data.Comment;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments;

    private OnCommentClickListener listener;

    public interface OnCommentClickListener {
        void onCommentClick(Comment comment);
    }

    public CommentAdapter(List<Comment> comments, OnCommentClickListener listener) {
        this.comments = comments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(comments.get(position));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void setData(List<Comment> list) {
        this.comments = list;
        notifyDataSetChanged();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        private ImageView avatar, likeIcon;
        private TextView authorText, commentText, likeCountText, timestampText;
        private OnCommentClickListener listener;

        public CommentViewHolder(@NonNull View itemView, OnCommentClickListener listener) {
            super(itemView);
            this.listener = listener;
            avatar = itemView.findViewById(R.id.avatar);
            likeIcon = itemView.findViewById(R.id.like_icon);
            authorText = itemView.findViewById(R.id.author);
            commentText = itemView.findViewById(R.id.comment_text);
            likeCountText = itemView.findViewById(R.id.like_count);
            timestampText = itemView.findViewById(R.id.timestamp);
        }

        public void bind(Comment comment) {
            authorText.setText(comment.getAuthor());
            commentText.setText(comment.getText());
            
            Glide.with(itemView.getContext())
                 .load(comment.getAvatarUrl())
                 .circleCrop()
                 .placeholder(R.drawable.ic_launcher_foreground)
                 .into(avatar);

            likeCountText.setText(String.valueOf(comment.getLikeCount()));
            timestampText.setText(formatTime(comment.getTimestamp()));

            updateLikeState(comment.isLiked());

            likeIcon.setOnClickListener(v -> {
                boolean liked = !comment.isLiked();
                comment.setLiked(liked);
                int count = comment.getLikeCount();
                comment.setLikeCount(liked ? count + 1 : Math.max(0, count - 1));
                likeCountText.setText(String.valueOf(comment.getLikeCount()));
                
                updateLikeState(liked);
                
                // Animation
                likeIcon.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction(() -> {
                    likeIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                }).start();
            });
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentClick(comment);
                }
            });
        }

        private void updateLikeState(boolean isLiked) {
            if (isLiked) {
                likeIcon.setImageResource(R.drawable.ic_douyin_like_1);
                likeIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.douyinIconLiked));
            } else {
                likeIcon.setImageResource(R.drawable.ic_douyin_like_1);
                likeIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.douyinIconGrey));
            }
        }

        private String formatTime(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            if (diff < 60 * 1000) {
                return "刚刚";
            } else if (diff < 60 * 60 * 1000) {
                return (diff / (60 * 1000)) + "分钟前";
            } else if (diff < 24 * 60 * 60 * 1000) {
                return (diff / (60 * 60 * 1000)) + "小时前";
            } else {
                return (diff / (24 * 60 * 60 * 1000)) + "天前";
            }
        }
    }
}
