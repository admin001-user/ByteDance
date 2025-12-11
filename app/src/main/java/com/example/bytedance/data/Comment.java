package com.example.bytedance.data;

public class Comment {
    private String author;
    private String avatarUrl; // Changed from int resId to String url
    private String text;
    private int likeCount;
    private boolean liked;
    private long timestamp; // Unix timestamp in ms

    public Comment(String author, String avatarUrl, String text, int likeCount, boolean liked, long timestamp) {
        this.author = author;
        this.avatarUrl = avatarUrl;
        this.text = text;
        this.likeCount = likeCount;
        this.liked = liked;
        this.timestamp = timestamp;
    }

    public String getAuthor() { return author; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getText() { return text; }
    public int getLikeCount() { return likeCount; }
    public boolean isLiked() { return liked; }
    public long getTimestamp() { return timestamp; }

    public void setLiked(boolean liked) { this.liked = liked; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
}
