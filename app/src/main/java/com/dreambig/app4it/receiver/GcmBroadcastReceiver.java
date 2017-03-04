package com.dreambig.app4it.receiver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;


import com.dreambig.app4it.service.GcmIntentService;
import com.dreambig.app4it.service.HomeBaseTwoService;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.dreambig.app4it.util.SharedPreferencesKeys;


/**
 * Created by Alexandr on 22/01/2015.
 */
public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if("com.google.android.c2dm.intent.REGISTRATION".equalsIgnoreCase(intent.getAction())) {
            //this should arrive anytime when the gcm.register is called. #deviceRegistration
            takeCareOfRegistration(context, intent);
        } else {
            takeCareOfNotification(context, intent);
        }

        setResultCode(Activity.RESULT_OK);
    }

    private void takeCareOfRegistration(Context context, Intent intent) {
        String registrationId = intent.getExtras().getString("registration_id");

        if(registrationId != null && !registrationId.equals("")) {
            //by now the user id should be saved in the references. but just in case it's not we check for null
            String userId = getUserId(context);
            if(userId != null) {
                //we need to do this via service because it has to run in a background thread. It's called HomeBase Two as for second step in the process
                Intent intentForService = new Intent(context, HomeBaseTwoService.class);
                intentForService.putExtra(MessageIdentifiers.REGISTRATION_ID, registrationId);
                intentForService.putExtra(MessageIdentifiers.USER_ID, userId);
                context.startService(intentForService);
            } else {
               //well, bad luck for now. but really shouldn't happen. maybe if someone downloads app, never registers and then updates
            }
        }

    }

    private void takeCareOfNotification(Context context, Intent intent) {
        // Explicitly specify that GcmIntentService will handle the intent.
        ComponentName comp = new ComponentName(context.getPackageName(), GcmIntentService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
    }

    private String getUserId(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPrefs.getString(SharedPreferencesKeys.REGISTERED_USER_ID, null);
    }

}
