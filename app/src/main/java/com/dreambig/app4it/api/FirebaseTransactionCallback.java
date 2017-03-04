package com.dreambig.app4it.api;

import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;

/**
 * Created by Alexandr on 04/01/2015.
 */
public interface FirebaseTransactionCallback {

    public void transactionEnded(FirebaseError firebaseError, boolean committed, DataSnapshot snapshot);

}
