package com.dreambig.app4it.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.dreambig.app4it.api.FirebaseApp4ItUsers;
import com.dreambig.app4it.api.FirebaseStringProcessor;
import com.dreambig.app4it.api.Phonebook;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserCandidate;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.StringUtil;


public class PhonebookImpl implements Phonebook {
	
	private Context context;
    private int numberOfExpectedResponses;
    private int numberOfArrivedResponses;
    private List<App4ItUser> app4ItUsers;
    private List<App4ItUserCandidate> app4ItUserCandidates;
	
	public PhonebookImpl(Context context) {
		this.context = context;
	}

    @Override
    public void getApp4ItUsers(final Map<String,String> phoneBook, final String userIdentifierToSkip, final boolean careAboutNonUsers, final FirebaseApp4ItUsers onComplete) {
        app4ItUsers = new ArrayList<>();
        app4ItUserCandidates = new ArrayList<>();
        numberOfExpectedResponses = phoneBook.size();
        numberOfArrivedResponses = 0;

        runCallbackIfDone(onComplete); //this would do something only on an empty phone book!

        FirebaseGateway firebaseGateway = new FirebaseGateway(context);

        for(final String phoneNumber : phoneBook.keySet()) {
            firebaseGateway.getUserIdentifierForNumber(phoneNumber, new FirebaseStringProcessor() {
                @Override
                public void process(String string) {
                    String userIdentifier = string;
                    if(userIdentifier != null && (userIdentifierToSkip == null || !userIdentifier.equals(userIdentifierToSkip))) {

                        App4ItUser user = new App4ItUser(userIdentifier);
                        user.setNumber(phoneNumber);
                        user.setName(phoneBook.get(phoneNumber));

                        app4ItUsers.add(user);
                    } else if (userIdentifier == null && careAboutNonUsers) {

                        String name = phoneBook.get(phoneNumber);
                        if(name != null && !"".equals(name.trim())) {
                            app4ItUserCandidates.add(new App4ItUserCandidate(name,phoneNumber));
                        }

                    }

                    //either case increase counter
                    responseReceived();

                    //and maybe we are done
                    runCallbackIfDone(onComplete);
                }
            });

        }
    }

    private void runCallbackIfDone(FirebaseApp4ItUsers onComplete) {

        if(numberOfExpectedResponses == numberOfArrivedResponses) {
            onComplete.processAp4ItUsers(app4ItUsers,app4ItUserCandidates);
        }

    }

    private void responseReceived() {

        numberOfArrivedResponses++;

    }
	
	@Override
	public Map<String,String> getFullPhoneNumberToName(Map<String,List<String>> nameToFullNumbers) {
		Map<String,String> ret = new HashMap<>();

        for(Map.Entry<String,List<String>> entry : nameToFullNumbers.entrySet()) {
            String name = entry.getKey();
            List<String> fullNumbers = entry.getValue();

            for(String fullNumber : fullNumbers) {
                ret.put(fullNumber, name);
            }
        }
		
		return ret;
	}



    @Override
    public Map<String,List<String>> getNameAndFullPhoneNumbers(String internationalCode, String homeUserNumber) throws Exception {
        Map<String,List<String>> ret = new HashMap<>();
        Map<String,PhonebookEntry> idToPhonebookEntry = new HashMap<>();


        ContentResolver contentResolver = context.getContentResolver();
        String[] columns = new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME};
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, columns, ContactsContract.Contacts.HAS_PHONE_NUMBER + " != '0'", null, null);

        if(cursor.moveToFirst())
        {
            do
            {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                idToPhonebookEntry.put(id, new PhonebookEntry(id, displayName));
            } while (cursor.moveToNext()) ;
            cursor.close();
        }

        //now get the numbers

        String allIds = StringUtil.toListOfValuesCommaSeparated(idToPhonebookEntry.keySet());
        String[] columnsInner = new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NUMBER};

        Cursor pCur = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,columnsInner,ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" IN (" + allIds + ")",null, null);
        while (pCur.moveToNext())
        {
            String contactId = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
            String contactNumber = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String numberToSave = makeNumberFullNumber(internationalCode,contactNumber);
            //some people have their own number saved, we don't want to treat it as a contact
            if(numberToSave != null && (homeUserNumber == null || !homeUserNumber.equals(numberToSave)) && isItFirebaseFriendlyNumber(numberToSave)) {
                PhonebookEntry phonebookEntry = idToPhonebookEntry.get(contactId);
                if(phonebookEntry != null) {
                    //should never be null
                    phonebookEntry.numbers.add(numberToSave);
                }
            }
        }
        pCur.close();

        //construct ret

        for(PhonebookEntry phonebookEntry : idToPhonebookEntry.values()) {
            if(phonebookEntry.numbers.size() > 0) {
                ret.put(phonebookEntry.contactName,new ArrayList<>(phonebookEntry.numbers));
            }
        }


        return ret;
    }
	
	public static String makeNumberFullNumber(String countryCodePrefix, String number) {
		if(number == null) {
			return null;
		}

        number = getRidOfWhiteSpaces(number);

        if (number.startsWith("00")) {
			//is already the way we want it
			return number;
		} else if (number.startsWith("+")) {
			//is almost the way we want it
			return number.replaceFirst("\\+", "00");
		} else if (number.startsWith("0")) {
			//let's get rid of the leading zero
			return countryCodePrefix + number.substring(1,number.length());
		} else {
			return countryCodePrefix + number;
		}
	}

	
	public static String getRidOfWhiteSpaces(String in) {
		return in.replaceAll("\\s", "");
	}

    private boolean isItFirebaseFriendlyNumber(String in) {
        //Firebase paths must not contain '.', '#', '$', '[', or ']'. Therefore such numbers would crash the app further down. so chuck them out already
        //in our case it should only be a whitespace-free number here

        boolean valid = true;

        for (int i = 0; i < in.length(); i++){
            char c = in.charAt(i);
            valid = Character.isDigit(c);
            if (!valid)
            {
                break;
            }
        }

        return valid;
    }

    private class PhonebookEntry {
        private String id;
        private String contactName;
        private Set<String> numbers = new HashSet<>();

        private PhonebookEntry(String id, String contactName) {
            this.id = id;
            this.contactName = contactName;
        }
    }

}

