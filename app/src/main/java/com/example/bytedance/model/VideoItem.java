package com.example.bytedance.model;

/**
 * 视频实体（双列推荐与详情页共用）
 *
 * 字段说明：
 * - videoUrl：视频播放地址
 * - thumbnailUrl：封面地址（用于列表展示和共享元素转场）
 * - description：视频描述文案
 * - author：作者昵称
 * - durationMs：时长（毫秒），用于在列表右下角显示 mm:ss
 * - avatarUrl：作者头像地址（圆形展示）
 * - likeCount：点赞数（用于双列 Item 展示）
 * - commentCount: 评论数
 */
public class VideoItem {
    public String videoUrl;
    public String thumbnailUrl;
    public String description;
    public String author;

    // 扩展字段
    public long durationMs;
    public String avatarUrl;
    public int likeCount;
    public int commentCount;

    /**
     * 旧版构造函数（兼容现有调用）
     */
    public VideoItem(String videoUrl, String thumbnailUrl, String description, String author) {
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.description = description;
        this.author = author;
        this.durationMs = 0L;
        this.avatarUrl = null;
        this.likeCount = 0;
        this.commentCount = 0;
    }

    /**
     * 扩展构造函数（匹配 MockData 中的 7 参数调用，保留兼容性）
     */
    public VideoItem(String videoUrl, String thumbnailUrl, String description, String author,
                     long durationMs, String avatarUrl, int likeCount) {
        this(videoUrl, thumbnailUrl, description, author, durationMs, avatarUrl, likeCount, 0);
    }

    /**
     * 全参数构造函数
     */
    public VideoItem(String videoUrl, String thumbnailUrl, String description, String author,
                     long durationMs, String avatarUrl, int likeCount, int commentCount) {
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.description = description;
        this.author = author;
        this.durationMs = durationMs;
        this.avatarUrl = avatarUrl;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoItem videoItem = (VideoItem) o;
        return durationMs == videoItem.durationMs &&
                likeCount == videoItem.likeCount &&
                commentCount == videoItem.commentCount &&
                java.util.Objects.equals(videoUrl, videoItem.videoUrl) &&
                java.util.Objects.equals(thumbnailUrl, videoItem.thumbnailUrl) &&
                java.util.Objects.equals(description, videoItem.description) &&
                java.util.Objects.equals(author, videoItem.author) &&
                java.util.Objects.equals(avatarUrl, videoItem.avatarUrl);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(videoUrl, thumbnailUrl, description, author, durationMs, avatarUrl, likeCount, commentCount);
    }
}
