package com.example.bytedance.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.example.bytedance.adapter.VideoAdapter;
import com.example.bytedance.data.MockData;
import com.example.bytedance.model.VideoItem;
import com.example.bytedance.databinding.FragmentVideoListBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 双列推荐列表 Fragment：
 * - SwipeRefreshLayout 下拉刷新
 * - RecyclerView 使用 StaggeredGridLayoutManager 实现 2 列瀑布流
 * - Item 点击带共享元素转场跳转到 PlayerActivity
 */
public class VideoListFragment extends Fragment {

    private FragmentVideoListBinding binding;
    private final List<VideoItem> data = new ArrayList<>();
    private VideoAdapter adapter;
    private boolean isLoadingMore = false;
    private com.example.bytedance.viewmodel.VideoViewModel viewModel;
    private int category = 0;

    public static VideoListFragment newInstance(int category) {
        VideoListFragment fragment = new VideoListFragment();
        Bundle args = new Bundle();
        args.putInt("category", category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getInt("category", 0);
        }
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(com.example.bytedance.viewmodel.VideoViewModel.class);
        viewModel.setCategory(category);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVideoListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // StaggeredGridLayoutManager：2 列瀑布流（禁用间隙自动处理，避免重排造成重叠错位）
        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        binding.recyclerView.setLayoutManager(sglm);
        // 关闭默认 ItemAnimator，避免高度变化导致的覆盖和重绘闪烁
        binding.recyclerView.setItemAnimator(null);
        binding.recyclerView.setHasFixedSize(false);

        adapter = new VideoAdapter(data);
        binding.recyclerView.setAdapter(adapter);

        viewModel.getVideoList().observe(getViewLifecycleOwner(), videos -> {
            data.clear();
            data.addAll(videos);
            adapter.notifyDataSetChanged();
            binding.swipeRefresh.setRefreshing(false);
            isLoadingMore = false;
        });

        viewModel.loadVideos();

        // 下拉刷新：重新获取数据
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refreshVideos();
        });

        // 上拉加载更多：滚动到底部追加一页数据
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 只有滚动到顶时才允许下拉刷新，避免瀑布流顶部不在 0 导致无法触发
                boolean atTop = recyclerView.computeVerticalScrollOffset() == 0;
                binding.swipeRefresh.setEnabled(atTop && !isLoadingMore);
                if (dy <= 0) return;
                if (isLoadingMore) return;

                RecyclerView.Adapter<?> ad = recyclerView.getAdapter();
                if (ad == null) return;

                int[] last = sglm.findLastVisibleItemPositions(null);
                int lastMax = Math.max(last[0], last.length > 1 ? last[1] : last[0]);
                if (lastMax >= ad.getItemCount() - 2) {
                    isLoadingMore = true;
                    viewModel.loadMoreVideos();
                }
            }
        });

        // 列表点击：共享元素转场进入 PlayerActivity
        adapter.setOnVideoClickListener((position, thumbnailView) -> {
            android.content.Intent intent = new android.content.Intent(requireContext(), com.example.bytedance.PlayerActivity.class);
            intent.putExtra("start_position", position);
            intent.putExtra("category", category);

            androidx.core.util.Pair<View, String> pair = new androidx.core.util.Pair<>(thumbnailView, "video_thumbnail");
            androidx.core.app.ActivityOptionsCompat options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), pair);
            startActivity(intent, options.toBundle());
        });
    }

    // Removed manual loadMore logic as it is handled by ViewModel

}
