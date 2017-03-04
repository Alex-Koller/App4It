package com.dreambig.app4it.api;

import com.firebase.client.DataSnapshot;

/**
 * Created by Alexandr on 01/01/2015.
 */
public interface FirebaseSnapshotCallback {

    void processSnapshot(boolean success, DataSnapshot snapshot, String errorMessage);

}
