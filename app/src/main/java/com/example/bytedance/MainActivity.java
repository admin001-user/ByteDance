package com.example.bytedance;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.bytedance.adapter.VideoAdapter;
import com.example.bytedance.data.MockData;
import com.example.bytedance.databinding.ActivityMainBinding;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        VideoAdapter videoAdapter = new VideoAdapter(MockData.getVideos());
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(videoAdapter);

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Home"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Following"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("For You"));

        binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_menu);
    }
}