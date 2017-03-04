package com.dreambig.app4it;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.dreambig.app4it.adapter.ContactsAdapter;
import com.dreambig.app4it.api.FirebaseApp4ItUsers;
import com.dreambig.app4it.api.Phonebook;
import com.dreambig.app4it.api.PhonebookCallback;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserCandidate;
import com.dreambig.app4it.fragment.ContactsListFragment;
import com.dreambig.app4it.impl.App4ItUserProfileManager;
import com.dreambig.app4it.impl.PhonebookImpl;
import com.dreambig.app4it.helper.UIHelper;

public class ContactsActivity extends Activity {

    private List<App4ItUser> listOfUsers;
    private List<App4ItUserCandidate> listOfUserCandidates;
    private boolean areWeLive;
    private boolean goingToShare;
    private ContactsAdapter contactsAdapter;


    public void contactsAdapterStartsLoading() {
        findViewById(R.id.contacts_fragment_container).setVisibility(View.GONE);
        findViewById(R.id.contacts_loading_notice).setVisibility(View.VISIBLE);

        invalidateOptionsMenu(); //this will get the options menu to hide
    }

    public void contactsAdapterStopsLoading() {
        findViewById(R.id.contacts_loading_notice).setVisibility(View.GONE);
        findViewById(R.id.contacts_fragment_container).setVisibility(View.VISIBLE);

        invalidateOptionsMenu(); //this will get the options menu to show
    }

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(goingToShare) {
            //we just came back from sharing. don't reload
            goingToShare = false;
            //refresh the list though
            if(contactsAdapter != null) { contactsAdapter.notifyDataSetChanged(); }
        } else {
            //normal opening or resuming the activity
            areWeLive = true;
            reloadLists();
        }
    }

    private void reloadLists() {
        contactsAdapterStartsLoading();
        //L og.d("ContactsActivity", "onResume() called");
        getDelegate().activityStarts(new PhonebookCallback() {
            @Override
            public void phoneBookContacts(boolean refreshed, Map<String, String> numberToName) {
                if (numberToName == null) {
                    UIHelper.showLongMessage(getApplicationContext(), "Failed to read your phone book :-(");
                } else {
                    Phonebook phonebook = new PhonebookImpl(getApplicationContext());
                    phonebook.getApp4ItUsers(numberToName, getDelegate().getLoggedInUserId(), true, new FirebaseApp4ItUsers() {
                        @Override
                        public void processAp4ItUsers(List<App4ItUser> users, List<App4ItUserCandidate> userCandidates) {
                            if(areWeLive) {
                                proceedWithUsers(users, userCandidates);
                            } //if we are not live then there won't be anywhere to stick this and the app will crash
                        }
                    });
                }
            }
        });
    }

    private void proceedWithUsers(final List<App4ItUser> users, final List<App4ItUserCandidate> userCandidates) {
        App4ItUserProfileManager.addToCacheUsers(this,users,getDelegate().getLoggedInUserId(),new App4ItUserProfileManager.AddingToCacheCallback() {
            @Override
            public void completed() {
                if(areWeLive) {
                    sortUsersAndShowThem(users,userCandidates);
                }
            }
        });
    }

    private void sortUsersAndShowThem(List<App4ItUser> users, List<App4ItUserCandidate> userCandidates) {
        //sort them
        Collections.sort(users, new Comparator<App4ItUser>() {
            @Override
            public int compare(App4ItUser lhs, App4ItUser rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });

        Collections.sort(userCandidates, new Comparator<App4ItUserCandidate>() {
            @Override
            public int compare(App4ItUserCandidate lhs, App4ItUserCandidate rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });

        //keep them
        listOfUsers = users;
        listOfUserCandidates = userCandidates;

        //stick the fragment in
        ContactsListFragment listFragment = new ContactsListFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.contacts_fragment_container, listFragment);
        fragmentTransaction.commit();

        //now any moment the adapter will ask for the data
        contactsAdapterStopsLoading();
    }

    public void giveMeContacts(ContactsAdapter contactsAdapter) {
        this.contactsAdapter = contactsAdapter;

        if(listOfUsers != null) contactsAdapter.setData(listOfUsers,listOfUserCandidates);
        else contactsAdapter.setData(new ArrayList<App4ItUser>(), new ArrayList<App4ItUserCandidate>());

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(goingToShare) {
            //don't do anything
        } else {
            areWeLive = false;
            getDelegate().activityStops();
        }
    }
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contacts);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.contacts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_share_button:
                goingToShare();
                shareTheApp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);

        View loadingNotice = findViewById(R.id.contacts_loading_notice);
        if(loadingNotice == null || loadingNotice.getVisibility() == View.VISIBLE) {
            menu.getItem(0).setEnabled(false);
        } else {
            menu.getItem(0).setEnabled(true);
        }

        return true;
    }

    private void shareTheApp() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getTextToShare());
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    private String getTextToShare() {
        return "Hello! Please join me in using BeApp4It. It's a great app for sharing ideas for events and activities. It's here on Apple App Store: http://itunes.com/apps/beapp4it. And here on Google Play: http://play.google.com/store/apps/details?id=com.dreambig.app4it. Or look at the website www.beapp4it.com.";
    }

    public void goingToShare() {
        this.goingToShare = true;
    }

}
