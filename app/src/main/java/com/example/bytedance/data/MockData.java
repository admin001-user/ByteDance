package com.example.bytedance.data;

import com.example.bytedance.model.VideoItem;

import java.util.ArrayList;
import java.util.List;

public class MockData {
    public static List<VideoItem> getVideos() {
        List<VideoItem> videos = new ArrayList<>();
        videos.add(new VideoItem("https://www.w3schools.com/html/mov_bbb.mp4", "https://picsum.photos/seed/animation3/400/300", "3D Animation Demo.", "W3Schools"));
        videos.add(new VideoItem("https://storage.googleapis.com/webfundamentals-assets/videos/chrome.mp4", "https://picsum.photos/seed/nature2/400/300", "Nature Landscape Timelapse", "oW3Schols"));
        videos.add(new VideoItem("http://vfx.mtime.cn/Video/2019/03/18/mp4/190318214226685784.mp4", "https://p.qqan.com/up/2021-4/16194928272148324.jpg", "This is the third video.", "Author 3"));
        videos.add(new VideoItem("http://vfx.mtime.cn/Video/2019/03/19/mp4/190319212559089721.mp4", "https://p.qqan.com/up/2021-4/16194928272148324.jpg", "This is the fourth video.", "Author 4"));
        videos.add(new VideoItem("http://vfx.mtime.cn/Video/2019/03/18/mp4/190318231014076505.mp4", "https://p.qqan.com/up/2021-4/16194928272148324.jpg", "This is the fifth video.", "Author 5"));
        videos.add(new VideoItem("http://vfx.mtime.cn/Video/2019/03/18/mp4/190318214226685784.mp4", "https.p.qqan.com/up/2021-4/16194928272148324.jpg", "This is the sixth video.", "Author 6"));
        return videos;
    }
}