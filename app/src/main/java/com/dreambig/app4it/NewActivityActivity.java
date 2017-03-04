package com.dreambig.app4it;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dreambig.app4it.adapter.NewActivityInvitationsAdapter;
import com.dreambig.app4it.api.FirebaseActivitySaveCallback;
import com.dreambig.app4it.api.FirebaseApp4ItUsers;
import com.dreambig.app4it.api.NewsCenter;
import com.dreambig.app4it.api.Phonebook;
import com.dreambig.app4it.api.PhonebookCallback;
import com.dreambig.app4it.entity.App4ItActivity;
import com.dreambig.app4it.entity.App4ItMapLocation;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserCandidate;
import com.dreambig.app4it.fragment.LoadingFragment;
import com.dreambig.app4it.fragment.NewActivityDetailsFragment;
import com.dreambig.app4it.fragment.NewActivityInvitationListFragment;
import com.dreambig.app4it.impl.NewsCenterImpl;
import com.dreambig.app4it.impl.PhonebookImpl;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.MapHelper;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.dreambig.app4it.helper.UIHelper;
import com.firebase.client.FirebaseError;

public class NewActivityActivity extends Activity  {

    private boolean areWeLive;
    private List<App4ItUser> usersList;
    private LoadingFragment loadingFragment;
    private NewActivityDetailsFragment detailsFragment;
    private NewActivityInvitationListFragment invitationListFragment;
    private Dialog mapDialog;

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();
    }

    @Override
    protected void onPause() {
        super.onPause();
        areWeLive = false;
        //L og.d("NewActivityActivity", "onPause()");
        getDelegate().activityStops();
    }

    @Override
    protected void onResume() {
        super.onResume();
        areWeLive = true;
        //L og.d("NewActivityActivity", "onResume()");

        final App4ItApplication delegate = getDelegate();

        delegate.activityStarts(new PhonebookCallback() {
            @Override
            public void phoneBookContacts(boolean refreshed, Map<String, String> numberToName) {
                if (numberToName == null) {
                    UIHelper.showLongMessage(getApplicationContext(), "Failed to read your phone book :-(");
                } else {
                    Phonebook phonebook = new PhonebookImpl(getApplicationContext());
                    phonebook.getApp4ItUsers(numberToName, getDelegate().getLoggedInUserId(), false, new FirebaseApp4ItUsers() {
                        @Override
                        public void processAp4ItUsers(List<App4ItUser> users, List<App4ItUserCandidate> userCandidates) {
                            usersList = users;
                            if(areWeLive) {
                                if(invitationListFragment.getListAdapter() != null) {
                                    ((NewActivityInvitationsAdapter)invitationListFragment.getListAdapter()).setUsersList(users);
                                    if(loadingFragment.isVisible()) toggleFragmentsVisibility(false,true,false);
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //L og.d("NewActivityActivity", "onCreate()...");
        setContentView(R.layout.activity_new_activity);
        changeFontOnTopButtons();

        putInFragments();
        boolean startWithInvitationList = startWithInvitationList(savedInstanceState);
        toggleFragmentsVisibility(!startWithInvitationList, false, startWithInvitationList);
        addCallbacksToButtons();

    }

    private void changeFontOnTopButtons() {
        Typeface tf = Typeface.createFromAsset(getAssets(),
                "fonts/MavenPro-Regular.ttf");

        Button flipButton = (Button) findViewById(R.id.btnDetailsInvitationListFlip);
        flipButton.setTextSize(20.0f);
        flipButton.setTypeface(tf);

        Button saveButton = (Button) findViewById(R.id.btnSaveNewActivity);
        saveButton.setTextSize(20.0f);
        saveButton.setTypeface(tf);
    }

    private boolean startWithInvitationList(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            return savedInstanceState.getBoolean(MessageIdentifiers.SHOW_INVITATION_LIST); //this is false if no such property
        }

        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if((invitationListFragment != null && invitationListFragment.isVisible()) || (loadingFragment != null && loadingFragment.isVisible())) {
            outState.putBoolean(MessageIdentifiers.SHOW_INVITATION_LIST,true);
        }
    }

    private void addCallbacksToButtons() {
        Button flipButton = (Button) findViewById(R.id.btnDetailsInvitationListFlip);
        flipButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(detailsFragment.isVisible()) {
                    //flipping to invitation list
                    hideSoftKeyboard();
                    flipToInvitationListRequired();
                } else {
                    //flipping to details list
                    flipToDetailsRequired();
                }
            }
        });

        Button saveButton = getSaveButton();
        saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard();
                saveThisActivity();
            }
        });
    }

    private Button getSaveButton() {
        return (Button) findViewById(R.id.btnSaveNewActivity);
    }

    private void savingStarts(Button saveButton) {
        saveButton.setText(R.string.saving);
        saveButton.setEnabled(false);
        saveButton.setTextColor(Color.BLACK);
    }

    private void savingEnds(Button saveButton) {
        saveButton.setText(R.string.save);
        saveButton.setEnabled(true);
        saveButton.setTextColor(Color.BLACK);
    }

    private void saveThisActivity() {
        String whereValue = getWhereValue().trim();
        if(!whereValue.equals("") && detailsFragment.getMapLocationInsertedThroughMap() == null) {
            offerAddingMapLocation();
        } else if (detailsFragment.getAddressInsertedThroughMap() != null && !whereValue.equals(detailsFragment.getAddressInsertedThroughMap())) {
            offerUpdatingTheMapLocationToReflectWhereValue();
        } else {
            proceedWithSaving();
        }
    }

    private void offerUpdatingTheMapLocationToReflectWhereValue() {
        final boolean removingWhereString = getWhereValue().trim().equals("");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.update_the_map_location));

        if(removingWhereString) {
           builder.setMessage(getResources().getString(R.string.because_place_of_the_event_has_been_removed));
        } else {
            builder.setMessage(getResources().getString(R.string.place_of_event_change_to_this).replace("THIS_LOCATION",getWhereValue()));
        }


        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(removingWhereString) {
                    detailsFragment.setMapLocationInsertedThroughMap(null);
                    proceedWithSaving();
                } else {
                    userWantsToSearchForTheWhereString();
                }
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                proceedWithSaving();
            }
        });

        mapDialog = builder.create();
        mapDialog.show();
    }

    private void offerAddingMapLocation() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.do_you_want_to_add_map_location));
        builder.setMessage(getResources().getString(R.string.for_this).replace("THIS_LOCATION",getWhereValue()));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                userWantsToSearchForTheWhereString();
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                proceedWithSaving();
            }
        });

        mapDialog = builder.create();
        mapDialog.show();
    }

    private void offerTooManyResultsOpenInMap() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.more_than_one_result));
        builder.setMessage(getResources().getString(R.string.do_you_want_to_open_map));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                detailsFragment.navigateToWriteMap();
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //nothing to do
            }
        });

        mapDialog = builder.create();
        mapDialog.show();
    }

    private void offerNothingFoundSaveAnyway() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.nothing_found_on_the_map));
        builder.setMessage(getResources().getString(R.string.save_anyway));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                detailsFragment.setMapLocationInsertedThroughMap(null);
                proceedWithSaving();
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //nothing to do
            }
        });

        mapDialog = builder.create();
        mapDialog.show();
    }

    private void userWantsToSearchForTheWhereString() {
        List<Address> addresses = MapHelper.findMapAddressForString(NewActivityActivity.this,getWhereValue());
        if(addresses == null || addresses.size() == 0) {
            offerNothingFoundSaveAnyway();
        } else if (addresses.size() > 1) {
            offerTooManyResultsOpenInMap();
        } else {
            Address address = addresses.get(0);
            detailsFragment.setMapLocationInsertedThroughMap(new App4ItMapLocation(address.getLatitude(),address.getLongitude()));
            proceedWithSaving();
        }
    }

    private String getWhereValue() {
        return ((EditText)findViewById(R.id.newActivityLayout_where)).getText().toString();
    }

    private void proceedWithSaving() {
        final Button saveButton = getSaveButton();
        //validate
        if(detailsFragment.isEverythingValidAndGoodToSaveEventDetails()) {
            savingStarts(saveButton);
            //collect info from the details view controller
            final App4ItActivity draftActivity = detailsFragment.scrapActivity();

            //save the activity
            final String loggedInUserIdentifier = getDelegate().getLoggedInUserId();
            final String loggedInUserNumber = getDelegate().getLoggedInUserNumber();

            final FirebaseGateway firebaseGateway = new FirebaseGateway(this);
            final NewsCenter newsCenter = new NewsCenterImpl();
            firebaseGateway.saveNewActivity(draftActivity.getTitle(),draftActivity.getMoreAbout(),draftActivity.getWhenFormat(),draftActivity.getWhenValue(),draftActivity.getWhereAsString(),draftActivity.getMapLocation(),draftActivity.getType(),loggedInUserIdentifier,loggedInUserNumber,new FirebaseActivitySaveCallback() {
                @Override
                public void accept(FirebaseError firebaseError, String activityIdentifier) {
                    if(firebaseError != null) {
                        savingEnds(saveButton);
                        UIHelper.showBriefMessage(getApplicationContext(),"Failed saving your event :-(. " + firebaseError.getMessage());
                    } else {
                        //stick the activity to the user created bucket
                        firebaseGateway.addToUsersUserCreatedBucket(loggedInUserIdentifier, activityIdentifier);

                        //add the creator as GOING to the invitation list
                        firebaseGateway.addUserToInvitationListAndSetGoing(loggedInUserIdentifier, loggedInUserNumber, activityIdentifier);

                        //and invite people
                        for(App4ItUser invitedUser : invitationListFragment.getCheckedOnes()) {
                            //L og.d("NewActivityActivity", "Inviting a guest");

                            firebaseGateway.inviteUserToActivityNoTransaction(activityIdentifier,invitedUser.getUserId(),invitedUser.getNumber());
                            firebaseGateway.addToUsersInvitedToBucket(activityIdentifier,invitedUser.getUserId(),loggedInUserIdentifier,loggedInUserIdentifier);

                            //and let them know about this
                            newsCenter.postNewsAboutBeingInvitedToActivity(getApplicationContext(),activityIdentifier,draftActivity.getTitle(),loggedInUserIdentifier,invitedUser.getUserId());
                        }

                        //question is whether this shouldn't be called only after all is done & completed
                        UIHelper.showBriefMessage(getApplicationContext(),getResources().getString(R.string.saved));
                        finish();
                    }
                }
            });
        }
    }

    private void flipToDetailsRequired() {
        toggleFragmentsVisibility(true, false, false);
    }

    private void flipToInvitationListRequired() {
        if(usersList == null) {
            //we are still only loading the list
            toggleFragmentsVisibility(false,false,true);
        } else {
            toggleFragmentsVisibility(false,true,false);
        }
    }

    private void toggleFragmentsVisibility(boolean showDetails, boolean showInvitations, boolean showLoading) {
        Button flipButton = ((Button)findViewById(R.id.btnDetailsInvitationListFlip));

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if(showDetails) {
            transaction.show(detailsFragment);
            flipButton.setText(R.string.invitation_list);
        } else {
            transaction.hide(detailsFragment);
        }

        if(showInvitations) {
            transaction.show(invitationListFragment);
            flipButton.setText(R.string.event_details);
        } else {
            transaction.hide(invitationListFragment);
        }

        if(showLoading) {
            transaction.show(loadingFragment);
            flipButton.setText(R.string.event_details);
        } else {
            transaction.hide(loadingFragment);
        }

        transaction.commit();
    }

    private void putInFragments() {
        if(!retrieveDetailsFragment()) putInDetailsFragment();
        if(!retrieveInvitationList()) putInInvitationListFragment();
        if(!retrieveLoadingFragment()) putInLoadingFragment();
    }

    private boolean retrieveLoadingFragment() {
        loadingFragment = (LoadingFragment)getFragmentManager().findFragmentByTag("loadingFragment");
        return loadingFragment != null;
    }

    private boolean retrieveDetailsFragment() {
        detailsFragment = (NewActivityDetailsFragment)getFragmentManager().findFragmentByTag("detailsFragment");
        return detailsFragment != null;
    }

    private boolean retrieveInvitationList() {
        invitationListFragment = (NewActivityInvitationListFragment)getFragmentManager().findFragmentByTag("invitationListFragment");
        return invitationListFragment != null;
    }

    private void putInLoadingFragment() {
        loadingFragment = new LoadingFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.new_activity_frame_container, loadingFragment,"loadingFragment");
        transaction.commit();
    }

    private void putInDetailsFragment() {
        detailsFragment = new NewActivityDetailsFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.new_activity_frame_container, detailsFragment,"detailsFragment");
        transaction.commit();
    }

    private void putInInvitationListFragment() {
        invitationListFragment = new NewActivityInvitationListFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.new_activity_frame_container, invitationListFragment,"invitationListFragment");
        transaction.commit();
    }

    public void giveMeContacts(NewActivityInvitationsAdapter adapter) {
        if(usersList == null) {
            //mhmm.... we must be still loading. should never happen as this fragment should be shown only when there's data
            adapter.setUsersList(new ArrayList<App4ItUser>());
        } else {
            adapter.setUsersList(usersList);
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        View detailsFragmentView = detailsFragment.getView();
        if(detailsFragmentView != null) {
            imm.hideSoftInputFromWindow(detailsFragmentView.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mapDialog != null) {
            mapDialog.dismiss();
            mapDialog = null;
        }

    }

}
