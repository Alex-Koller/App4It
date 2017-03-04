package com.dreambig.app4it;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.dreambig.app4it.api.FirebaseApp4ItUsers;
import com.dreambig.app4it.api.Phonebook;
import com.dreambig.app4it.api.PhonebookCallback;
import com.dreambig.app4it.entity.App4ItInvitationItem;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserCandidate;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.enums.InvitationStatus;
import com.dreambig.app4it.fragment.InvitationListFragment;
import com.dreambig.app4it.fragment.LoadingFragment;
import com.dreambig.app4it.helper.A4ItHelper;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.impl.App4ItUserProfileManager;
import com.dreambig.app4it.impl.PhonebookImpl;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.dreambig.app4it.util.StringUtil;

public class InviteActivity extends Activity  {

    private boolean areWeLive;
    private String activityId;
    private String activityOwnerId;
    private String activityTitle;
    private ArrayList<App4ItInvitationItem> invitationItems;

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();
    }

    public void addInvitation(App4ItUser user, InvitationStatus status) {
        App4ItInvitationItem newItem = new App4ItInvitationItem(user, status);
        invitationItems.add(newItem);
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(MessageIdentifiers.INVITATION_LIST, invitationItems);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        parseIntentIntoInstanceVariables(getIntent(),savedInstanceState);
    }

    private void displayLoadingView() {
        LoadingFragment loadingFragment = new LoadingFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.invitations_fragment_container, loadingFragment);
        fragmentTransaction.commit();
    }

    private void parseIntentIntoInstanceVariables(Intent intent, Bundle savedInstanceState) {
        activityId = intent.getStringExtra(MessageIdentifiers.ACTIVITY_ID);
        activityOwnerId = intent.getStringExtra(MessageIdentifiers.ACTIVITY_OWNER_ID);
        activityTitle = intent.getStringExtra(MessageIdentifiers.ACTIVITY_TITLE);
        if(savedInstanceState == null) {
            //L og.d("InviteActivity", "It's a fresh activity, using data from intent");
            invitationItems = intent.getParcelableArrayListExtra(MessageIdentifiers.INVITATION_LIST);
        } else {
            //L og.d("InviteActivity", "Restoring invitation list from bundle");
            invitationItems = savedInstanceState.getParcelableArrayList(MessageIdentifiers.INVITATION_LIST);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        areWeLive = false;
        getDelegate().activityStops();
    }

    @Override
    protected void onResume() {
        super.onResume();
        areWeLive = true;
        //L og.d("InviteActivity", "onResume()");
        displayLoadingView();

        final App4ItApplication delegate = getDelegate();

        delegate.activityStarts(new PhonebookCallback() {
            @Override
            public void phoneBookContacts(boolean refreshed, Map<String, String> numberToName) {
                if (numberToName == null) {
                    UIHelper.showLongMessage(getApplicationContext(), "Failed to read your phone book :-(");
                } else {
                    Phonebook phonebook = new PhonebookImpl(getApplicationContext());
                    phonebook.getApp4ItUsers(numberToName, delegate.getLoggedInUserId(), false, new FirebaseApp4ItUsers() {
                        @Override
                        public void processAp4ItUsers(final List<App4ItUser> users, List<App4ItUserCandidate> userCandidates) {
                            if(areWeLive) {
                                //here the home user won't be in the list. but the likelihood that we land on this screen and their profile is not downloaded yet is tiny
                                App4ItUserProfileManager.addToCacheUsers(InviteActivity.this,users,delegate.getLoggedInUserId(),new App4ItUserProfileManager.AddingToCacheCallback() {
                                    @Override
                                    public void completed() {
                                        if(areWeLive) {
                                            proceedWithUsers(users);
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

    }

    private void proceedWithUsers(List<App4ItUser> users) {

        List<App4ItInvitationItem> allIncludingInvitationItems = new ArrayList<>(invitationItems);
        allIncludingInvitationItems.addAll(fabricateInvitationItems(getUsersWithoutInvitation(users,invitationItems)));


        Collections.sort(allIncludingInvitationItems, new Comparator<App4ItInvitationItem>() {
            @Override
            public int compare(App4ItInvitationItem first, App4ItInvitationItem second) {

                App4ItUserProfile firstProfile = App4ItUserProfileManager.getUserProfile(InviteActivity.this,first.getUser());
                App4ItUserProfile secondProfile = App4ItUserProfileManager.getUserProfile(InviteActivity.this,second.getUser());
                InvitationStatus firstInvitation = first.getStatus();
                InvitationStatus secondInvitation = second.getStatus();

                //invitation status decides first then name
                if(firstInvitation == null && secondInvitation == null) {
                    return StringUtil.compareStringsPhoneNumbersLast(firstProfile.getName(), secondProfile.getName());
                } else if (firstInvitation == null) {
                    //means second has some invitation status
                    return 1;
                } else if (secondInvitation == null) {
                    return -1;
                } else {
                    //both have a status
                    int statusesCompared = A4ItHelper.compareInvitationStatus(firstInvitation,secondInvitation);

                    if(statusesCompared == 0) {
                        //both same status, name decides
                        return StringUtil.compareStringsPhoneNumbersLast(firstProfile.getName(), secondProfile.getName());
                    } else {
                        return statusesCompared;
                    }
                }

            }
        });

        displayList(allIncludingInvitationItems);
    }

    private void displayList(List<App4ItInvitationItem> invitationItemList) {
        InvitationListFragment listFragment = new InvitationListFragment();
        listFragment.setArguments(getArgumentsForInvitationsFragment());
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.invitations_fragment_container, listFragment);
        fragmentTransaction.commit();
        listFragment.setContent(invitationItemList);
    }

    private Bundle getArgumentsForInvitationsFragment() {
        Bundle arguments = new Bundle();
        arguments.putString(MessageIdentifiers.ACTIVITY_ID, activityId);
        arguments.putString(MessageIdentifiers.ACTIVITY_OWNER_ID,activityOwnerId);
        arguments.putString(MessageIdentifiers.ACTIVITY_TITLE, activityTitle);
        return arguments;
    }

    private List<App4ItInvitationItem> fabricateInvitationItems(List<App4ItUser> forUsers) {
        List<App4ItInvitationItem> ret = new ArrayList<>();

        for(App4ItUser user : forUsers) {
            ret.add(new App4ItInvitationItem(user,null));
        }

        return ret;
    }

    private List<App4ItUser> getUsersWithoutInvitation(List<App4ItUser> inListOfUsers, List<App4ItInvitationItem> invitationItemList) {

        List<App4ItUser> ret = new ArrayList<>(inListOfUsers);

        for(App4ItInvitationItem invitationItem : invitationItemList) {
            ret.remove(invitationItem.getUser());
        }

        return ret;
    }

}
