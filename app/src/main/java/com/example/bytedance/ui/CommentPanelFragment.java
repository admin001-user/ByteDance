package com.example.bytedance.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bytedance.adapter.CommentAdapter;
import com.example.bytedance.data.Comment;
import com.example.bytedance.databinding.FragmentCommentPanelBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class CommentPanelFragment extends BottomSheetDialogFragment {

    private FragmentCommentPanelBinding binding;
    private CommentAdapter adapter;
    private List<Comment> comments;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommentPanelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        comments = new ArrayList<>();
        comments.add(new Comment("This is the first comment."));
        comments.add(new Comment("This is the second comment."));

        adapter = new CommentAdapter(comments);
        binding.commentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.commentRecyclerView.setAdapter(adapter);

        binding.sendButton.setOnClickListener(v -> {
            String commentText = binding.commentEditText.getText().toString();
            if (!commentText.isEmpty()) {
                Comment newComment = new Comment(commentText);
                comments.add(newComment);
                adapter.notifyItemInserted(comments.size() - 1);
                binding.commentEditText.setText("");
                binding.commentRecyclerView.scrollToPosition(comments.size() - 1);
            }
        });
    }
}