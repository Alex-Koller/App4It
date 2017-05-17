package com.dreambig.app4it.helper;

import android.app.Activity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Created by Alexandr on 22/01/2015.
 */
public class GCMSupport {

    private final static String APP4IT_PROJECT_ID = "universal-helix-789";
    private final static String APP4IT_PROJECT_NUMBER = "889158669406";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static String getApp4itProjectId() {
        return APP4IT_PROJECT_ID;
    }

    public static String getApp4itProjectNumber() {
        return APP4IT_PROJECT_NUMBER;
    }

    public static boolean areGoogleAppServicesReady(Activity activity) {
        int checkCode = checkPlayServices(activity, PLAY_SERVICES_RESOLUTION_REQUEST);

        switch(checkCode) {
            case 0:
                //L og.i("StartActivity", "Google app services check positive");
                return true;

            case 1:
                //L og.i("StartActivity", "Google app services check negative");
                return false;

            default:
                //L og.i("StartActivity", "Google app services check not recoverable");
                return false;
        }
    }

    private static int checkPlayServices(Activity context, int PLAY_SERVICES_RESOLUTION_REQUEST) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, context,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                return 2;
            }
            return 1;
        }
        return 0;
    }

    public static int googlePlayServicesVersion() {
        return GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE;
    }

}
