package com.example.bytedance.data;

import com.example.bytedance.model.VideoItem;

import java.util.ArrayList;
import java.util.List;

public class MockData {
    //提交时隐藏真实的地址，避免泄露
    public static List<VideoItem> getVideos() {
        List<VideoItem> videos = new ArrayList<>();
        videos.add(new VideoItem("http://xxx.com/14783138_2160_3840_30fps.mp4", "http://xxx.com/images/14783138_2160_3840_30fps.png", "阳光明媚的草地上，鹿群悠闲地吃草。阳光明媚的草地上，鹿群悠闲地吃草。", "W3Schools"));
        videos.add(new VideoItem("http://xxx.com/14734488_1080_1920_24fps.mp4", "http://xxx.com/images/14734488_1080_1920_24fps.png", "This is the second video.", "oW3Schols"));
        videos.add(new VideoItem("http://xxx.com/16100219-uhd_2160_3840_30fps.mp4", "http://xxx.com/images/16100219-uhd_2160_3840_30fps.png", "This is the third video.", "Author 3"));
        videos.add(new VideoItem("http://xxx.com/17023408-uhd_2160_3840_24fps.mp4", "http://xxx.com/images/17023408-uhd_2160_3840_24fps.png", "This is the fourth video.The car is running on the road!", "Author 4"));
        videos.add(new VideoItem("http://xxx.com/20732252-hd_1080_1920_30fps.mp4", "http://xxx.com/images/20732252-hd_1080_1920_30fps.png", "This is the fifth video.", "Author 5"));
        videos.add(new VideoItem("http://xxx.com/14787400_1080_1920_60fps.mp4", "http://xxx.com/images/14787400_1080_1920_60fps.png", "This is the sixth video.", "Author 6"));
        return videos;
    }
}