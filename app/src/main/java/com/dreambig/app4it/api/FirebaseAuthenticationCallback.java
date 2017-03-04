package com.dreambig.app4it.api;

import com.firebase.client.AuthData;
import com.firebase.client.FirebaseError;

/**
 * Created by Alexandr on 25/12/2014.
 */
public interface FirebaseAuthenticationCallback {

    void onSuccess(AuthData authData);
    void onFailure(FirebaseError firebaseError);
    void onWeIgnoredIt();

}
