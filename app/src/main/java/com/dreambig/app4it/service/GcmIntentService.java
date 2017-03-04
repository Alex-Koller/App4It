package com.dreambig.app4it.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.dreambig.app4it.App4ItApplication;
import com.dreambig.app4it.HomeActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.entity.BehaviourOverrides;
import com.dreambig.app4it.receiver.GcmBroadcastReceiver;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.google.android.gms.gcm.GoogleCloudMessaging;


/**
 * Created by Alexandr on 22/01/2015.
 */
public class GcmIntentService extends IntentService {
    public static final String INSTRUCTION_NAVIGATE_TO_COMMENT = "NAVIGATE_TO_COMMENT";

    private static final String HOME_BASE_MESSAGE_ACTION_SHOW_COMMENTS = "showComments";

    private static final String GCM_PAYLOAD_MESSAGE_PART = "message";
    private static final String GCM_PAYLOAD_ACTION_PART = "action";
    private static final String GCM_PAYLOAD_SUBJECT_ID_PART = "subjectId";
    private static final String GCM_PAYLOAD_SUBJECT_TITLE_PART = "subjectTitle";
    private NotificationManager mNotificationManager;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    private boolean doNotPostThisNotification(String action, String subjectId) {
        App4ItApplication app4ItApplication = (App4ItApplication)getApplication();

        //when it's a comments notification for activity whose comments we are just looking at
        if(HOME_BASE_MESSAGE_ACTION_SHOW_COMMENTS.equalsIgnoreCase(action) && subjectId.equals(app4ItApplication.getActivityIdWhoseCommentsAreBeingLookedAt())) {
            return true;
        }

        return false;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             *
             * Appears we get some trash here too so check message on null
             */
             if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                 String message = extras.getString(GCM_PAYLOAD_MESSAGE_PART);
                 String action = extras.getString(GCM_PAYLOAD_ACTION_PART);
                 String subjectId = extras.getString(GCM_PAYLOAD_SUBJECT_ID_PART);
                 String subjectTitle = extras.getString(GCM_PAYLOAD_SUBJECT_TITLE_PART);
                 if(message != null && !doNotPostThisNotification(action,subjectId)) {
                     sendNotification(message, action, subjectId, subjectTitle);
                 }
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private Intent prepareNotifyIntent(String action, String subjectId, String subjectTitle) {
        Intent ret = new Intent(this, HomeActivity.class);
        if(HOME_BASE_MESSAGE_ACTION_SHOW_COMMENTS.equalsIgnoreCase(action)) {
            ret.putExtra(MessageIdentifiers.ADDITIONAL_INSTRUCTION,INSTRUCTION_NAVIGATE_TO_COMMENT);
            ret.putExtra(MessageIdentifiers.ACTIVITY_ID, subjectId);
            ret.putExtra(MessageIdentifiers.ACTIVITY_TITLE, subjectTitle);
        }
        ret.putExtra(MessageIdentifiers.OVERRIDE_DEFAULT_APP4IT_BEHAVIOUR, BehaviourOverrides.GO_TO_START_ACTIVITY);
        ret.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return ret;
    }

    // Put the message into a notification and post it.
    private void sendNotification(String msg, String action, String subjectId, String subjectTitle) {
        int uniqId = msg.hashCode();

        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notifyIntent = prepareNotifyIntent(action,subjectId,subjectTitle);
        PendingIntent contentIntent = PendingIntent.getActivity(this, uniqId, notifyIntent, 0);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notificationicon)
                        .setAutoCancel(true)
                        .setContentTitle("BeApp4It")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setVibrate(new long[] { 0, 500, 100, 500 });
        mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(uniqId, mBuilder.build());

    }
}
