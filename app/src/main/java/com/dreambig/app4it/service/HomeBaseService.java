package com.dreambig.app4it.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dreambig.app4it.helper.GCMSupport;
import com.dreambig.app4it.receiver.GcmBroadcastReceiver;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Created by Alexandr on 18/02/2015.
 *
 * This is a badly named class. It should be called something like RegisterGcmService
 *
 * It follows these ideas: https://snowdog.co/blog/dealing-with-service_not_available-google-cloud-messaging/
 */
public class HomeBaseService extends IntentService {

    public HomeBaseService() {
        super("HomeBaseService");
    }

    //note that this is run in the worker thread. and that it then stops the service by itself
    @Override
    protected void onHandleIntent(Intent intent) {
        /*
        the next line is supposed to return the registration id. but on some devices it just throws SERVICE_NOT_AVAILABLE error
        therefore adoptive approach had to be addopted. follow #deviceRegistration

        since then GCM register is deprecated so use newer way of doing - InstanceID
        if it's worth doing we could check for version of google play services and do it the old way of instance id n/a
        */

        try {
            reconfirmGCMToken(getApplicationContext());
        } catch (Exception e) {
            //not much that can be done here
        }
    }

    public static void reconfirmGCMToken(Context context) throws IOException {
        String authorizedEntity = GCMSupport.getApp4itProjectNumber();
        InstanceID instanceID = InstanceID.getInstance(context);
        String token = instanceID.getToken(authorizedEntity,GoogleCloudMessaging.INSTANCE_ID_SCOPE);
        GcmBroadcastReceiver.takeCareOfRegistration(context,token);
    }

}
