package com.dreambig.app4it;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dreambig.app4it.api.Phonebook;
import com.dreambig.app4it.api.PhonebookCallback;
import com.dreambig.app4it.async.PhonebookReaderAsyncTask;
import com.dreambig.app4it.entity.FilterSettings;
import com.dreambig.app4it.impl.PhonebookImpl;
import com.dreambig.app4it.util.SharedPreferencesKeys;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexandr on 25/12/2014.
 *
 * Note that you can't rely on this object to live throughout the life of the app. it can be destroyed and recreated
 */
public class App4ItApplication extends Application {

    private String loggedInUserId;
    private String loggedInUserNumber;
    private String internationalPhonePrefix;
    private Map<String,String> phoneNumberToName;

    private long mostRecentActivityStart;
    private long mostRecentActivityStop;

    public App4ItApplication() {
        super();
    }

    public PersistedInfo getPersistedInfo() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String userEmail = sharedPrefs.getString(SharedPreferencesKeys.USER_EMAIL, null);
        final String userPassword = sharedPrefs.getString(SharedPreferencesKeys.USER_PASSWORD, null);
        final String internationalCode = sharedPrefs.getString(SharedPreferencesKeys.COUNTRY_PREFIX, null);

        if(userEmail != null && userPassword != null && internationalCode != null) {
            return new PersistedInfo(userEmail.trim(),userPassword.trim(),internationalCode.trim()); //being defensive with the trimming here
        } else {
            return null;
        }
    }

    public void activityStarts(final PhonebookCallback callback) {
        mostRecentActivityStart = new Date().getTime();

        if(refreshPhonebook()) {

            String homeUserNumber = getLoggedInUserNumber();
            String prefix = getInternationalPhonePrefix();

            PhonebookReaderAsyncTask task = new PhonebookReaderAsyncTask(getApplicationContext(),prefix,homeUserNumber,new PhonebookCallback() {
                @Override
                public void phoneBookContacts(boolean refreshed, Map<String, String> numberToName) {
                    //let's take that one for later use here. mind that it can be null
                    if(numberToName != null) setPhoneNumberToName(numberToName);
                    //and call callback
                    if(callback != null) callback.phoneBookContacts(true,numberToName);
                }
            });

            task.execute();
        } else {
            if(callback != null) callback.phoneBookContacts(false,phoneNumberToName);
        }

    }

    public void activityStops() {
        mostRecentActivityStop = new Date().getTime();
    }

    private boolean refreshPhonebook() {
        return phoneNumberToName == null || (mostRecentActivityStart - mostRecentActivityStop) > 6000; //6 seconds is pretty random. too short would cause reload on use case of "ah, one more thing"
    }

    public void refreshPhonebookSynchronously(String homeUserNumber, String phonePrefix) throws Exception {
        Phonebook phonebook = new PhonebookImpl(this);
        Map<String,List<String>> deviceContacts = phonebook.getNameAndFullPhoneNumbers(phonePrefix, homeUserNumber);
        phoneNumberToName = phonebook.getFullPhoneNumberToName(deviceContacts);
    }

    public String getNameFromPhoneNumber(String phoneNumber) {
        return phoneNumberToName == null ? null : phoneNumberToName.get(phoneNumber);
    }

    public void setPhoneNumberToName(Map<String, String> phoneNumberToName) {
        this.phoneNumberToName = phoneNumberToName;
    }

    public Map<String,String> getPhoneNumberToNameMap() {
        if(phoneNumberToName == null) return new HashMap<>();
        else return new HashMap<>(phoneNumberToName);
    }

    public boolean hasPhoneBookBeenLoaded() {
        return phoneNumberToName != null;
    }


    public String getLoggedInUserId() {
        if(loggedInUserId == null) {
            loggedInUserId = getValueFromPersistantData(SharedPreferencesKeys.REGISTERED_USER_ID);
        }

        //L og.i("App4ItApplication", "Logged in user id is being returned " + loggedInUserId);
        return loggedInUserId;
    }

    public String getLoggedInUserNumber() {
        if(loggedInUserNumber == null) {
            loggedInUserNumber = getValueFromPersistantData(SharedPreferencesKeys.REGISTERED_USER_NUMBER);
        }

        //L og.i("App4ItApplication", "Logged in user number is being returned " + loggedInUserNumber);
        return loggedInUserNumber;
    }

    public String getInternationalPhonePrefix() {
        if(internationalPhonePrefix == null) {
            internationalPhonePrefix = getValueFromPersistantData(SharedPreferencesKeys.COUNTRY_PREFIX);
        }

        //L og.i("App4ItApplication", "International code prefix being returned " + internationalPhonePrefix);
        return internationalPhonePrefix;
    }

    private String getValueFromPersistantData(String key) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String ret = sharedPrefs.getString(key, null);

        //being defensive with the trimming here
        if(ret != null) {
            ret = ret.trim();
        }
        return ret;
    }

    public void storeActivityIdWhoseCommentsAreBeingLookedAt(String activityId) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SharedPreferencesKeys.ACTIVITY_ID_WHOSE_COMMENTS_ARE_OPEN, activityId);
        editor.apply();
    }

    public void removeAnyActivityIdWhoseCommentsAreBeingLookedAt() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.remove(SharedPreferencesKeys.ACTIVITY_ID_WHOSE_COMMENTS_ARE_OPEN);
        editor.apply();
    }

    public String getActivityIdWhoseCommentsAreBeingLookedAt() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return sharedPrefs.getString(SharedPreferencesKeys.ACTIVITY_ID_WHOSE_COMMENTS_ARE_OPEN, null);
    }

    public void saveFilterSettings(FilterSettings filterSettings) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_CATCHUP,filterSettings.showCatchUp());
        editor.putBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_CULTURAL,filterSettings.showCultural());
        editor.putBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_FOOD_AND_DRINK,filterSettings.showFoodAndDrink());
        editor.putBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_NIGHTOUT,filterSettings.showNightOut());
        editor.putBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_SPORT,filterSettings.showSport());
        editor.putBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_UNDISCLOSED,filterSettings.showUndisclosed());
        editor.putBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_GOING,filterSettings.showGoing());
        editor.putBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_NOT_GOING,filterSettings.showNotGoing());
        editor.putBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_UNANSWERED,filterSettings.showUnanswered());
        editor.putBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_CREATED_BY_ME,filterSettings.showCreatedByMe());
        editor.apply();
    }

    public FilterSettings loadFilterSettings() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //the default value should never be used but just in case - better to show everything
        boolean showCatchup = sharedPrefs.getBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_CATCHUP,true);
        boolean showCultural = sharedPrefs.getBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_CULTURAL,true);
        boolean showFoodAndDrink = sharedPrefs.getBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_FOOD_AND_DRINK,true);
        boolean showNightout = sharedPrefs.getBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_NIGHTOUT,true);
        boolean showSport = sharedPrefs.getBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_SPORT,true);
        boolean showUndisclosed = sharedPrefs.getBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_UNDISCLOSED,true);
        boolean showGoing = sharedPrefs.getBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_GOING,true);
        boolean showNotGoing = sharedPrefs.getBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_NOT_GOING,true);
        boolean showUnanswered = sharedPrefs.getBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_UNANSWERED,true);
        boolean showCreatedByMe = sharedPrefs.getBoolean(SharedPreferencesKeys.FILTER_SETTING_SHOW_CREATED_BY_ME,true);

        return new FilterSettings(showUndisclosed,showCatchup,showCultural,showNightout,showSport,showFoodAndDrink,showGoing,showNotGoing,showUnanswered,showCreatedByMe);
    }

    public class PersistedInfo {
        private String email;
        private String password;
        private String internationalCode;

        private PersistedInfo(String email, String password, String internationalCode) {
            this.email = email;
            this.password = password;
            this.internationalCode = internationalCode;
        }

        public String getEmail() {
            return email;
        }

        public String getPassword() {
            return password;
        }

        public String getInternationalCode() {
            return internationalCode;
        }
    }
}
