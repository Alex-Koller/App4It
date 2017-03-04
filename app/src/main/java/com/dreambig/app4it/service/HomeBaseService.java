package com.dreambig.app4it.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.dreambig.app4it.helper.GCMSupport;
import com.google.android.gms.gcm.GoogleCloudMessaging;

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
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
        //the next line is supposed to return the registration id. but on some devices it just throws SERVICE_NOT_AVAILABLE error
        //therefore adoptive approach had to be addopted. follow #deviceRegistration
        try {
            gcm.register(GCMSupport.APP4IT_PROJECT_NUMBER);
        } catch (Exception e) {
            //not much that can be done here
        }
    }

}
