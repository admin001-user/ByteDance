package com.example.bytedance.model;

public class VideoItem {
    public String videoUrl;
    public String thumbnailUrl;
    public String description;
    public String author;

    public VideoItem(String videoUrl, String thumbnailUrl, String description, String author) {
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.description = description;
        this.author = author;
    }
}