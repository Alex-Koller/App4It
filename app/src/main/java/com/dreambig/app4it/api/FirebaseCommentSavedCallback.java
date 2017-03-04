package com.dreambig.app4it.api;

import com.dreambig.app4it.CommentsActivity;

/**
 * Created by Alexandr on 31/08/2015.
 */
public interface FirebaseCommentSavedCallback {

    void accept(String commentId, CommentsActivity.PostStatus postStatus);

}
