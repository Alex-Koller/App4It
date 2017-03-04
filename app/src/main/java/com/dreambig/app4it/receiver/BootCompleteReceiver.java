package com.dreambig.app4it.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dreambig.app4it.service.HomeBaseService;


/**
 * Created by Alexandr on 18/02/2015.
 */
public class BootCompleteReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(final Context context, Intent intent) {
        //we are handling this because a system update can change registration id. and it's followed by a reboot. this may take a bit after the reboot to kick in
        context.startService(new Intent(context, HomeBaseService.class));
    }


}
