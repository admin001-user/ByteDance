package com.example.bytedance.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * 顶部 Tab 的 ViewPager2 适配器：提供 推荐 / 关注 / 同城 三个页面
 * 目前三个页面均复用双列瀑布流的 VideoListFragment
 */
public class HomePagerAdapter extends FragmentStateAdapter {

    public HomePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 0: 推荐, 1: 关注, 2: 同城
        return VideoListFragment.newInstance(position);
    }

    @Override
    public int getItemCount() {
        return 3; // 推荐 / 关注 / 同城
    }
}

