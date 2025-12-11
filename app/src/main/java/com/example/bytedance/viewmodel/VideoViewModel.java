package com.example.bytedance.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bytedance.data.MockData;
import com.example.bytedance.model.VideoItem;

import java.util.Collections;
import java.util.List;

public class VideoViewModel extends ViewModel {

    private final MutableLiveData<List<VideoItem>> videoList = new MutableLiveData<>();

    public LiveData<List<VideoItem>> getVideoList() {
        return videoList;
    }

    public void loadVideos() {
        if (videoList.getValue() == null || videoList.getValue().isEmpty()) {
            refreshVideos();
        }
    }

    private int category = 0;

    public void setCategory(int category) {
        this.category = category;
    }

    public void refreshVideos() {
        List<VideoItem> videos = MockData.getVideos(category);
        videoList.setValue(videos);
    }

    public void loadMoreVideos() {
        List<VideoItem> current = videoList.getValue();
        if (current != null) {
            List<VideoItem> newVideos = MockData.getVideos(category);
            // Simulate paging by adding a subset
            current.addAll(newVideos.subList(0, Math.min(5, newVideos.size())));
            videoList.setValue(current);
        }
    }
}
