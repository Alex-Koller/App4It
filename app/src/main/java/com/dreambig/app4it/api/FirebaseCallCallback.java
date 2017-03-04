package com.dreambig.app4it.api;

import com.firebase.client.FirebaseError;

/**
 * Created by Alexandr on 25/12/2014.
 */
public interface FirebaseCallCallback {

    void onSuccess();
    void onFailure(FirebaseError firebaseError);

}
