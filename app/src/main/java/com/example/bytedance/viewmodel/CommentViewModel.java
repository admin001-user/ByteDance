package com.example.bytedance.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bytedance.data.Comment;
import com.example.bytedance.data.MockData;

import java.util.List;

public class CommentViewModel extends ViewModel {

    private final MutableLiveData<List<Comment>> commentList = new MutableLiveData<>();

    public LiveData<List<Comment>> getCommentList() {
        return commentList;
    }

    public void loadComments() {
        if (commentList.getValue() == null) {
            List<Comment> comments = MockData.getComments();
            commentList.setValue(comments);
        }
    }

    public void addComment(Comment comment) {
        List<Comment> current = commentList.getValue();
        if (current != null) {
            current.add(0, comment);
            commentList.setValue(current);
        }
    }
}
