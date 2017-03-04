package com.dreambig.app4it.async;

import android.content.Context;
import android.os.AsyncTask;

import com.dreambig.app4it.api.Phonebook;
import com.dreambig.app4it.api.PhonebookCallback;
import com.dreambig.app4it.impl.PhonebookImpl;

import java.util.List;
import java.util.Map;

/**
 * Created by Alexandr on 03/01/2015.
 */
public class PhonebookReaderAsyncTask extends AsyncTask<String,Void,Map<String,String>> {

    private Context context;
    private String phonePrefix;
    private String homeUserNumber;
    private PhonebookCallback callback;

    public PhonebookReaderAsyncTask(Context context, String phonePrefix, String homeUserNumber, PhonebookCallback callback) {
        this.context = context;
        this.phonePrefix = phonePrefix;
        this.homeUserNumber = homeUserNumber;
        this.callback = callback;
    }

    @Override
    protected void onPostExecute(Map<String, String> phoneBook) {
        callback.phoneBookContacts(true,phoneBook);
    }

    @Override
    protected Map<String, String> doInBackground(String ...args) {
        Phonebook phonebook = new PhonebookImpl(context);

        try {
            Map<String, List<String>> deviceContacts = phonebook.getNameAndFullPhoneNumbers(phonePrefix, homeUserNumber);
            return phonebook.getFullPhoneNumberToName(deviceContacts);
        } catch (Exception e) {
            //L og.e("PhonebookReaderAsyncTask", "Failed reading phone book " + e.getMessage(), e);
            return null;
        }
    }


}
