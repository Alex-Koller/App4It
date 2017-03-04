package com.dreambig.app4it.service;

import android.app.IntentService;
import android.content.Intent;

import com.appspot.universal_helix_789.androiddevice.Androiddevice;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.dreambig.app4it.util.Settings;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

/**
 * Created by Alexandr on 18/02/2015.
 */
public class HomeBaseTwoService extends IntentService {

    public HomeBaseTwoService() {
        super("HomeBaseTwoService");
    }

    //note that this is run in the worker thread. and that it then stops the service by itself
    @Override
    protected void onHandleIntent(Intent intent) {
        String registrationId = intent.getStringExtra(MessageIdentifiers.REGISTRATION_ID);
        String userId = intent.getStringExtra(MessageIdentifiers.USER_ID);

        if(registrationId != null && userId != null && !registrationId.equals("") && !userId.equals("")) {
            //send it over to home base server!
            sendRegistrationIdToBackend(registrationId, userId);
        }
    }

    private void sendRegistrationIdToBackend(final String deviceToken, final String userId) {

        Androiddevice.Builder builder = new Androiddevice.Builder(AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(), null)
                .setRootUrl(Settings.getHomeBaseEndpointUrl());

        Androiddevice androiddeviceService = builder.build();
        try {
            androiddeviceService.registerAndroidDevice(userId, deviceToken).execute();
        } catch(Exception e) {
            //could fail for reasons. again, not much we can do here
        }

    }


}
