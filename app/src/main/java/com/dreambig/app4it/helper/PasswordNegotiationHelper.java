package com.dreambig.app4it.helper;

import android.os.AsyncTask;

import com.appspot.universal_helix_789.androiddevice.Androiddevice;
import com.appspot.universal_helix_789.androiddevice.model.PasswordClaimParcel;
import com.dreambig.app4it.util.Settings;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

/**
 * Created by Alexandr on 21/02/2015.
 */
public class PasswordNegotiationHelper {

    public interface MessageRequestCreatedCallback {
        void success(Long claimId);
        void failure(String error);
    }

    public interface PasswordRequestCallback {
        void success(String password);
        void failure(String reason);
    }

    public static void sendMeATextMessage(String phoneNumber, final MessageRequestCreatedCallback messageRequestCreatedCallback) {

        new AsyncTask<String,Void,PasswordClaimParcel>() {

            @Override
            protected PasswordClaimParcel doInBackground(String... params) {
                String phoneNumber = params[0];

                Androiddevice.Builder builder = new Androiddevice.Builder(AndroidHttp.newCompatibleTransport(),
                        new AndroidJsonFactory(), null)
                        .setRootUrl(Settings.getHomeBaseEndpointUrl());

                Androiddevice androiddeviceService = builder.build();
                try {
                    return androiddeviceService.sendConfirmationTextAndroid(phoneNumber).execute();
                } catch(Exception e) {
                    PasswordClaimParcel errorCarrying = new PasswordClaimParcel();
                    errorCarrying.setClaimId(null);
                    errorCarrying.setClaimStatus(e.getMessage());
                    return errorCarrying;
                }

            }

            @Override
            protected void onPostExecute(PasswordClaimParcel passwordClaimParcel) {
                if(passwordClaimParcel.getClaimId() == null) {
                    messageRequestCreatedCallback.failure(passwordClaimParcel.getClaimStatus());
                } else {
                    messageRequestCreatedCallback.success(passwordClaimParcel.getClaimId());
                }
            }
        }.execute(phoneNumber);

    }

    public static void retrievePassword(Long claimId, String code, final PasswordRequestCallback passwordRequestCallback) {
        new AsyncTask<String,Void,PasswordClaimParcel>() {

            @Override
            protected PasswordClaimParcel doInBackground(String... params) {
                Long claimId = Long.valueOf(params[0]);
                String code = params[1];

                Androiddevice.Builder builder = new Androiddevice.Builder(AndroidHttp.newCompatibleTransport(),
                        new AndroidJsonFactory(), null)
                        .setRootUrl(Settings.getHomeBaseEndpointUrl());

                Androiddevice androiddeviceService = builder.build();
                try {
                    return androiddeviceService.getUserPasswordAndroid(claimId, code).execute();
                } catch(Exception e) {
                    PasswordClaimParcel errorCarrying = new PasswordClaimParcel();
                    errorCarrying.setPassword(null);
                    errorCarrying.setClaimStatus(e.getMessage());
                    return errorCarrying;
                }

            }

            @Override
            protected void onPostExecute(PasswordClaimParcel passwordClaimParcel) {
                if(passwordClaimParcel.getPassword() == null || passwordClaimParcel.getPassword().trim().equals("")) {
                    passwordRequestCallback.failure(passwordClaimParcel.getClaimStatus());
                } else {
                    passwordRequestCallback.success(passwordClaimParcel.getPassword());
                }
            }
        }.execute(String.valueOf(claimId),code);
    }

}
