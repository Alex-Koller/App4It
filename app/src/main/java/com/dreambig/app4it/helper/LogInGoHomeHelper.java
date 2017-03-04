package com.dreambig.app4it.helper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import com.dreambig.app4it.App4ItApplication;
import com.dreambig.app4it.HomeActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.api.FirebaseAuthenticationCallback;
import com.dreambig.app4it.api.LogInGoHomeUser;
import com.dreambig.app4it.entity.BehaviourOverrides;
import com.dreambig.app4it.entity.FilterSettings;
import com.dreambig.app4it.entity.FirebaseUser;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.service.HomeBaseService;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.dreambig.app4it.util.SharedPreferencesKeys;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

/**
 * Created by Alexandr on 21/02/2015.
 */
public class LogInGoHomeHelper {

    public static void logInAndGoHome(final Activity activity, final String email, final String password, final String fullPhoneNumber, final String internationalCode, final boolean doPostRegistrationSteps) {
        final FirebaseGateway firebaseGateway = new FirebaseGateway(activity);

        showWeAreConnecting(activity);
        firebaseGateway.logIn(email,password,true,new FirebaseAuthenticationCallback() {
            @Override
            public void onSuccess(AuthData authData) {
                showWeAreConnected(activity);
                FirebaseUser firebaseUser = new FirebaseUser(authData.getUid(), fullPhoneNumber);
                if(doPostRegistrationSteps) {
                    postRegistrationSteps(activity, firebaseUser, email, password, internationalCode, firebaseUser.getUserId(), firebaseUser.getUserNumber());
                }
                goHome(activity, firebaseUser, internationalCode, email, password);
            }

            @Override
            public void onFailure(FirebaseError firebaseError) {
                if(activity instanceof LogInGoHomeUser) {
                    ((LogInGoHomeUser)activity).defreeze();
                }

                UIHelper.showBriefMessage(activity.getApplicationContext(),"Failed to log in :-( " + firebaseError.getMessage());
            }

            @Override
            public void onWeIgnoredIt() {
                //this should never happen, prioritised attempt can't be ignored
                UIHelper.showBriefMessage(activity.getApplicationContext(),"The attempt to log in was ignored");
            }
        });
    }

    private static void showWeAreConnecting(Activity activity) {
        TextView textView = (TextView)activity.findViewById(R.id.connectingNotice);
        if(textView != null) {
            textView.setVisibility(View.VISIBLE);
        }
    }

    private static void showWeAreConnected(Activity activity) {
        TextView textView = (TextView)activity.findViewById(R.id.connectingNotice);
        if(textView != null) {
            String text = activity.getResources().getString(R.string.connected);
            textView.setText(text);
        }
    }

    private static void goHome(Activity activity, FirebaseUser user, String internationalCode, final String email, final String password) {
        //L og.d("StartActivity", "Transferring to home activity");
        makeSureWeAreAlwaysLoggedIn(activity, email, password);
        activity.startService(new Intent(activity.getApplicationContext(), HomeBaseService.class)); //this will (re)register the device id
        refreshPhoneBook(activity, user.getUserNumber(),internationalCode);

        Intent intent = new Intent(activity.getApplicationContext(), HomeActivity.class);
        Bundle extras = activity.getIntent().getExtras();
        if(isThisWithIntentToGoToComments(extras)) {
            intent.putExtra(MessageIdentifiers.OVERRIDE_DEFAULT_APP4IT_BEHAVIOUR, BehaviourOverrides.GO_TO_COMMENTS);
            intent.putExtra(MessageIdentifiers.ACTIVITY_ID, extras.getString(MessageIdentifiers.ACTIVITY_ID));
            intent.putExtra(MessageIdentifiers.ACTIVITY_TITLE, extras.getString(MessageIdentifiers.ACTIVITY_TITLE));
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }

    public static void makeSureWeAreAlwaysLoggedIn(final Activity activity, final String email, final String password) {
        final FirebaseGateway firebaseGateway = new FirebaseGateway(activity);

        firebaseGateway.watchAuthenticationState(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if(authData == null) {
                    //L og.d("StartActivity", "We are logged out now!!!!!!!!!!!!!!!!!!");
                    firebaseGateway.logIn(email, password, false, new FirebaseAuthenticationCallback() {
                        @Override
                        public void onSuccess(AuthData authData) {
                            //L og.d("StartActivity", "Re-logged in successfully");
                        }

                        @Override
                        public void onFailure(FirebaseError firebaseError) {
                            //L og.e("StartActivity", "Failed to re-login: " + firebaseError.getMessage());
                        }

                        @Override
                        public void onWeIgnoredIt() {
                            //L og.d("StartActivity", "We ignored the attempt to re-login!");
                        }
                    });
                }
            }
        });
    }

    private static void refreshPhoneBook(Activity activity, String homeUserPhoneNumber, String internationalCode) {
        try {
            ((App4ItApplication) activity.getApplication()).refreshPhonebookSynchronously(homeUserPhoneNumber, internationalCode);
        } catch(Exception e) {
            //L og.e("StartActivity","Failed refreshing phone book: " + e.getMessage());
        }
    }

    private static boolean isThisWithIntentToGoToComments(Bundle extras) {
        if(extras == null) {
            return false;
        } else {
            String defaultBehaviourOverride = extras.getString(MessageIdentifiers.OVERRIDE_DEFAULT_APP4IT_BEHAVIOUR);
            return BehaviourOverrides.GO_TO_COMMENTS.equals(defaultBehaviourOverride);
        }
    }

    private static void postRegistrationSteps(Activity activity, FirebaseUser user, String email, String password, String internationalCode, String loggedInUserId, String loggedInUserNumber) {
        //save credentials
        saveUserDetailsIntoPreferences(activity,email,password,internationalCode,loggedInUserId,loggedInUserNumber);

        //save default filter settings (show all)
        ((App4ItApplication)activity.getApplication()).saveFilterSettings(new FilterSettings(true,true,true,true,true,true,true,true,true,true));

        //create the basic entries in firebase
        FirebaseGateway firebaseGateway = new FirebaseGateway(activity);
        firebaseGateway.createBasicEntries(user,email);
    }

    private static void saveUserDetailsIntoPreferences(Activity activity, String userEmail, String userPassword, String countryPrefix, String loggedInUserId, String loggedInUserNumber) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SharedPreferencesKeys.USER_EMAIL, userEmail);
        editor.putString(SharedPreferencesKeys.USER_PASSWORD, userPassword);
        editor.putString(SharedPreferencesKeys.COUNTRY_PREFIX, countryPrefix);
        editor.putString(SharedPreferencesKeys.REGISTERED_USER_ID, loggedInUserId);
        editor.putString(SharedPreferencesKeys.REGISTERED_USER_NUMBER, loggedInUserNumber);
        editor.apply();
    }

}
