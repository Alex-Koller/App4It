package com.dreambig.app4it.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.dreambig.app4it.service.HomeBaseService;


/**
 * Created by Alexandr on 18/02/2015.
 */
public class AppUpdateReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(final Context context, Intent intent) {
        //app has been updated... that means registration id may have changed, call a service to fire the registration process again
        context.startService(new Intent(context, HomeBaseService.class));
    }


}
