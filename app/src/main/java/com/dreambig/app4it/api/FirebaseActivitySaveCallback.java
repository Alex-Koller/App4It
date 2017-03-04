package com.dreambig.app4it.api;

import com.firebase.client.FirebaseError;

/**
 * Created by Alexandr on 10/01/2015.
 */
public interface FirebaseActivitySaveCallback {

    void accept(FirebaseError firebaseError, String activityIdentifier);

}
