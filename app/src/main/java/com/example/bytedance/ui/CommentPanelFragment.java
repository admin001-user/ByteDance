package com.example.bytedance.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bytedance.R;
import com.example.bytedance.adapter.CommentAdapter;
import com.example.bytedance.data.Comment;
import com.example.bytedance.databinding.FragmentCommentPanelBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.lifecycle.ViewModelProvider;
import com.example.bytedance.viewmodel.CommentViewModel;

public class CommentPanelFragment extends BottomSheetDialogFragment implements CommentAdapter.OnCommentClickListener {

    private FragmentCommentPanelBinding binding;
    private CommentAdapter adapter;
    private CommentViewModel viewModel;
    // private List<Comment> comments; // Removed, handled by Adapter/ViewModel

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        viewModel = new ViewModelProvider(this).get(CommentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommentPanelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
            bottomSheetDialog.getBehavior().setSkipCollapsed(true);
            bottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            
            // Adjust for keyboard
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();
        if (view != null) {
            View parent = (View) view.getParent();
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
            // Set height to 70% of screen height
            DisplayMetrics displayMetrics = new DisplayMetrics();
            if (getActivity() != null) {
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                behavior.setPeekHeight((int) (displayMetrics.heightPixels * 0.7));
            }
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            parent.setBackgroundResource(android.R.color.transparent);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup Adapter
        adapter = new CommentAdapter(new ArrayList<>(), this);
        binding.commentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.commentRecyclerView.setAdapter(adapter);

        // Observe ViewModel
        viewModel.getCommentList().observe(getViewLifecycleOwner(), list -> {
            adapter.setData(list);
            updateHeader(list.size());
            updateEmptyState(list.isEmpty());
        });
        viewModel.loadComments();

        // Close Button
        binding.closeButton.setOnClickListener(v -> dismiss());

        // Send Button Logic
        binding.sendButton.setEnabled(false);
        binding.commentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.length() > 0;
                binding.sendButton.setEnabled(hasText);
                binding.sendButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), 
                        hasText ? R.color.douyinBlue : android.R.color.darker_gray));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.sendButton.setOnClickListener(v -> {
            String text = binding.commentEditText.getText().toString().trim();
            if (!text.isEmpty()) {
                addComment(text);
                binding.commentEditText.setText("");
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && binding.commentEditText != null) {
                    imm.hideSoftInputFromWindow(binding.commentEditText.getWindowToken(), 0);
                }
            }
        });
        
        // Automatically show keyboard when opening?
        // User asked: "弹出时自动显示键盘" (Automatically show keyboard when popped up)
        binding.commentEditText.requestFocus();
        binding.commentEditText.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.commentEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 300);
    }

    private void addComment(String text) {
        Comment newComment = new Comment(
                "我", 
                "https://picsum.photos/seed/me/80", 
                text, 
                0, 
                false, 
                System.currentTimeMillis()
        );
        viewModel.addComment(newComment);
        binding.commentRecyclerView.scrollToPosition(0);
    }

    private void updateHeader(int count) {
        binding.headerTitle.setText(String.format(Locale.getDefault(), "%d条评论", count));
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.commentRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyView.setVisibility(View.GONE);
            binding.commentRecyclerView.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onCommentClick(Comment comment) {
        String replyText = "回复 @" + comment.getAuthor() + ": ";
        binding.commentEditText.setText(replyText);
        binding.commentEditText.setSelection(replyText.length());

        // Show keyboard
        binding.commentEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.commentEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }
}
