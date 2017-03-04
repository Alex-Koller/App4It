package com.dreambig.app4it.api;

import com.dreambig.app4it.entity.App4ItUserProfile;
import com.firebase.client.FirebaseError;

/**
 * Created by Alexandr on 11/11/2015.
 */
public interface FirebaseUserProfileCallback {

    void acceptUserProfile(App4ItUserProfile userProfile, FirebaseError error);

}
