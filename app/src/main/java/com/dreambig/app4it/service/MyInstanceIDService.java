package com.dreambig.app4it.service;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Created by Alexandr on 17/05/2017.
 */

public class MyInstanceIDService extends InstanceIDListenerService {

    public void onTokenRefresh() {
        try {
            HomeBaseService.reconfirmGCMToken(getApplicationContext());
        } catch (Exception e) {
            //not much that can be done here
        }
    }

}
