package com.dreambig.app4it.api;

import com.dreambig.app4it.repository.FirebaseGateway;

/**
 * Created by Alexandr on 01/01/2015.
 */
public interface FirebaseActivityInviteeCallback {

    void processUser(FirebaseGateway firebaseGateway, String userIdentifier);

}
