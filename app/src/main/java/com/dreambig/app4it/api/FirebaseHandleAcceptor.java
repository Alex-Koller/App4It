package com.dreambig.app4it.api;

import com.firebase.client.ChildEventListener;

/**
 * Created by Alexandr on 04/01/2015.
 */
public interface FirebaseHandleAcceptor {

    void accept(ChildEventListener childEventListener);

}
