package com.example.bytedance;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bytedance.adapter.VideoPlayerAdapter;
import com.example.bytedance.data.MockData;
import com.example.bytedance.databinding.ActivityPlayerBinding;
import com.google.android.exoplayer2.ExoPlayer;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        player = new ExoPlayer.Builder(this).build();

        VideoPlayerAdapter adapter = new VideoPlayerAdapter(MockData.getVideos());
        adapter.setPlayer(player);

        binding.viewPager.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}