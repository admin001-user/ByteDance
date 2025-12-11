package com.example.bytedance.data;

import com.example.bytedance.model.VideoItem;

import java.util.ArrayList;
import java.util.List;

public class MockData {
    //提交时隐藏真实的地址，避免泄露
    public static List<VideoItem> getVideos() {
        return getVideos(0);
    }

    public static List<VideoItem> getVideos(int category) {
        List<VideoItem> videos = new ArrayList<>();
        // 基础数据池
        VideoItem[] pool = {
            new VideoItem("http://t69jhm6kv.hd-bkt.clouddn.com/14783138_2160_3840_30fps.mp4", "http://t69jhm6kv.hd-bkt.clouddn.com/images/14783138_2160_3840_30fps.png", "阳光明媚的草地上，鹿群悠闲地吃草。阳光明媚的草地上，鹿群悠闲地吃草。", "W3Schools", 96_000, "https://picsum.photos/seed/a/80", 1200, 342),
            new VideoItem("http://t69jhm6kv.hd-bkt.clouddn.com/14068233_640_360_30fps.mp4", "http://t69jhm6kv.hd-bkt.clouddn.com/images/14068233_640_360_30fps.png", "城市黄昏下的人群匆匆而过。", "oW3Schols", 83_000, "https://picsum.photos/seed/b/80", 2350, 1024),
            new VideoItem("http://t69jhm6kv.hd-bkt.clouddn.com/14787400_1080_1920_60fps.mp4", "http://t69jhm6kv.hd-bkt.clouddn.com/images/14787400_1080_1920_60fps.png", "海面上微风轻拂，帆船缓缓行驶。", "Author 3", 72_000, "https://picsum.photos/seed/c/80", 560, 45),
            new VideoItem("http://t69jhm6kv.hd-bkt.clouddn.com/2882620-hd_1920_1080_30fps.mp4", "http://t69jhm6kv.hd-bkt.clouddn.com/images/2882620-hd_1920_1080_30fp.png", "街头艺人演奏，围观的人群鼓掌。", "Author 4", 104_000, "https://picsum.photos/seed/d/80", 1890, 892),
            new VideoItem("http://t69jhm6kv.hd-bkt.clouddn.com/17023408-uhd_2160_3840_24fps.mp4", "http://t69jhm6kv.hd-bkt.clouddn.com/images/17023408-uhd_2160_3840_24fps.png", "雨后的城市，路面倒映霓虹。", "Author 5", 55_000, "https://picsum.photos/seed/e/80", 980, 230),
            new VideoItem("http://t69jhm6kv.hd-bkt.clouddn.com/20732252-hd_1080_1920_30fps.mp4", "http://t69jhm6kv.hd-bkt.clouddn.com/images/20732252-hd_1080_1920_30fps.png", "公园里孩子嬉闹的欢乐时光。", "Author 6", 44_000, "https://picsum.photos/seed/f/80", 350, 110)
        };

        // 根据分类返回不同数据子集或顺序
        if (category == 1) { // 关注：返回偶数项
             videos.add(pool[1]);
             videos.add(pool[3]);
             videos.add(pool[5]);
             videos.add(pool[0]);
        } else if (category == 2) { // 同城：返回奇数项
             videos.add(pool[0]);
             videos.add(pool[2]);
             videos.add(pool[4]);
             videos.add(pool[1]);
        } else { // 推荐：全部
             for (VideoItem item : pool) videos.add(item);
        }
        return videos;
    }

    public static List<Comment> getComments() {
        List<Comment> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        list.add(new Comment("新青年俱乐部", "https://picsum.photos/seed/comment1/80", "全是评论点赞，没人关注吗？这个视频拍的太好了！", 221, false, now - 1000 * 60 * 5));
        list.add(new Comment("说唱集合站", "https://picsum.photos/seed/comment2/80", "你这西安话真好听，很有感觉。", 121, true, now - 1000 * 60 * 60));
        list.add(new Comment("七叶贞露", "https://picsum.photos/seed/comment3/80", "求BGM！这首歌叫什么名字呀？", 334, false, now - 1000 * 60 * 60 * 2));
        list.add(new Comment("一只皮皮青蛙啊哥", "https://picsum.photos/seed/comment4/80", "哈哈哈哈哈哈，笑死我了，这个特效绝了。", 334, false, now - 1000 * 60 * 60 * 24));
        list.add(new Comment("园岭南社区老会", "https://picsum.photos/seed/comment5/80", "拍得不错，继续加油！", 12, false, now - 1000 * 60 * 60 * 48));
        list.add(new Comment("张三", "https://picsum.photos/seed/comment6/80", "糟糕，没看出来是特效，太逼真了。", 4919, true, now - 1000 * 60 * 60 * 24 * 5));
        list.add(new Comment("李四", "https://picsum.photos/seed/comment7/80", "在这个位置松手，视频应该继续播放才对。", 5, false, now - 1000 * 60 * 60 * 24 * 6));
        list.add(new Comment("王五", "https://picsum.photos/seed/comment8/80", "评论区好热闹，大家都是哪里来的？", 88, false, now - 1000 * 60 * 60 * 24 * 7));
        list.add(new Comment("赵六", "https://picsum.photos/seed/comment9/80", "这种视频多发点，爱看！", 66, true, now - 1000 * 60 * 60 * 24 * 8));
        list.add(new Comment("钱七", "https://picsum.photos/seed/comment10/80", "这里的风景真不错，想去打卡。", 45, false, now - 1000 * 60 * 60 * 24 * 9));
        
        return list;
    }
}
