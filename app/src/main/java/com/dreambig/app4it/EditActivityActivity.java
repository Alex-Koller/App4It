package com.dreambig.app4it;


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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import com.dreambig.app4it.api.App4ItActivityManager;
import com.dreambig.app4it.api.FirebaseActivityUpdateCallback;
import com.dreambig.app4it.api.FirebaseSnapshotCallback;
import com.dreambig.app4it.api.NewsCenter;
import com.dreambig.app4it.api.SuccessOrFailureCallback;
import com.dreambig.app4it.entity.App4ItActivity;
import com.dreambig.app4it.entity.App4ItActivityParcel;
import com.dreambig.app4it.entity.App4ItMapLocation;
import com.dreambig.app4it.enums.NewsType;
import com.dreambig.app4it.fragment.NewActivityDetailsFragment;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.impl.App4ItActivityManagerImpl;
import com.dreambig.app4it.impl.NewsCenterImpl;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.MapHelper;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;

import java.util.ArrayList;
import java.util.List;

public class EditActivityActivity extends Activity {

    private Dialog mapDialog;
	private NewActivityDetailsFragment detailsFragment;

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //L og.d("EditActivityActivity", "onPause()");
        getDelegate().activityStops();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //L og.d("EditActivityActivity", "onResume()");
        getDelegate().activityStarts(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //L og.d("EditActivityActivity", "onCreate()...");
        setContentView(R.layout.activity_edit_activity);

        changeFontOnTopButtons();
        putInFragments();
        addCallbacksToButtons();
    }

    private void changeFontOnTopButtons() {
        Typeface tf = Typeface.createFromAsset(getAssets(),
                "fonts/MavenPro-Regular.ttf");

        Button flipButton = (Button) findViewById(R.id.btnEditActivitySave);
        flipButton.setTextSize(20.0f);
        flipButton.setTypeface(tf);

        Button saveButton = (Button) findViewById(R.id.btnEditActivityDelete);
        saveButton.setTextSize(20.0f);
        saveButton.setTypeface(tf);
    }

    private Button getSaveButton() {
        return (Button) findViewById(R.id.btnEditActivitySave);
    }

    private Button getDeleteButton() {
        return (Button) findViewById(R.id.btnEditActivityDelete);
    }

    private void addCallbacksToButtons() {
        final Button saveButton = getSaveButton();
        final Button deleteButton = getDeleteButton();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard();
                doEditThisEvent();
            }
        });


        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EditActivityActivity.this);

                TextView myMsg = new TextView(EditActivityActivity.this);
                myMsg.setText(getResources().getString(R.string.do_you_want_to_remove_silently_or_loudly));
                myMsg.setTextSize(18.0f);
                myMsg.setPadding(5,20,5,20);
                myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
                builder.setView(myMsg);

                builder.setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //don't do anything
                            }
                        })
                        .setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                doDeleteThisEvent(true, saveButton, deleteButton);
                            }
                        })
                        .setNeutralButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doDeleteThisEvent(false, saveButton, deleteButton);
                            }
                        });
                // Create the AlertDialog object and show it
                builder.create().show();
            }
        });
    }

    private void deletingStarts(Button saveButton, Button deleteButton) {
        deleteButton.setText(R.string.deleting);
        deleteButton.setEnabled(false);
        deleteButton.setTextColor(Color.BLACK);

        saveButton.setEnabled(false);
        saveButton.setTextColor(Color.BLACK);
    }

    private void deletingEnds(Button saveButton, Button deleteButton) {
        deleteButton.setText(R.string.delete_event);
        deleteButton.setEnabled(true);
        deleteButton.setTextColor(Color.BLACK);

        saveButton.setEnabled(true);
        saveButton.setTextColor(Color.BLACK);
    }

    private void doDeleteThisEvent(boolean notifyInvitees, final Button saveButton, final Button deleteButton) {
        deletingStarts(saveButton,deleteButton);
        App4ItActivityManager activityManager = new App4ItActivityManagerImpl();
        final App4ItActivityParcel existingActivity = getIntent().getExtras().getParcelable(MessageIdentifiers.ACTIVITY_PARCEL);
        activityManager.deleteActivity(this,notifyInvitees,existingActivity,getDelegate().getLoggedInUserId(), new SuccessOrFailureCallback() {
            @Override
            public void callback(boolean success, String errorMessage) {
                if(success) {
                    UIHelper.showBriefMessage(getApplicationContext(), existingActivity.getTitle() + " " + getResources().getString(R.string.deleted).toLowerCase());
                    finish();
                } else {
                    deletingEnds(saveButton,deleteButton);
                    UIHelper.showBriefMessage(EditActivityActivity.this, errorMessage);
                }
            }
        });
    }

    private List<NewsType> whatChangedInDraftActivity(App4ItActivityParcel existingActivity, App4ItActivity draftActivity) {

        List<NewsType> ret = new ArrayList<>();
        boolean mapLocationsDiffer = mapLocationsDifferent(existingActivity.getMapLocation(),draftActivity.getMapLocation());

        if(!existingActivity.getTitle().equals(draftActivity.getTitle())) ret.add(NewsType.ACTIVITY_TITLE_EDITED);
        if(!existingActivity.getMoreAbout().equals(draftActivity.getMoreAbout())) ret.add(NewsType.ACTIVITY_DESCRIPTION_EDITED);
        if(!existingActivity.getWhenFormat().equals(draftActivity.getWhenFormat()) || !existingActivity.getWhenValue().equals(draftActivity.getWhenValue())) ret.add(NewsType.ACTIVITY_TIME_EDITED);
        if(!existingActivity.getWhereAsString().equals(draftActivity.getWhereAsString()) || mapLocationsDiffer) ret.add(NewsType.ACTIVITY_PLACE_EDITED);
        if(!existingActivity.getType().equals(draftActivity.getType())) ret.add(NewsType.ACTIVITY_TYPE_EDITED);

        return ret;
    }

    private boolean mapLocationsDifferent(App4ItMapLocation a, App4ItMapLocation b) {

        if(a == null && b == null) {
            return false;
        } else if(a != null && b == null) {
            return true;
        } else if (a == null && b != null) {
            return true;
        } else {
            return !MapHelper.coordinatesTheSame(a, b);
        }
    }

    private void savingStarts(Button saveButton, Button deleteButton) {
        saveButton.setText(R.string.saving);
        saveButton.setEnabled(false);
        saveButton.setTextColor(Color.BLACK);

        deleteButton.setEnabled(false);
        deleteButton.setTextColor(Color.BLACK);
    }

    private void savingEnds(Button saveButton, Button deleteButton) {
        saveButton.setText(R.string.save);
        saveButton.setEnabled(true);
        saveButton.setTextColor(Color.BLACK);

        deleteButton.setEnabled(true);
        deleteButton.setTextColor(Color.BLACK);
    }

    private String getWhereValue() {
        return ((EditText)findViewById(R.id.newActivityLayout_where)).getText().toString();
    }

    private void doEditThisEvent() {

        App4ItActivityParcel existingActivity = getExistingActivity();
        String whereValue = getWhereValue().trim();

        if (!whereValue.equals(existingActivity.getWhereAsString()) && !whereValue.equals(detailsFragment.getAddressInsertedThroughMap())) {
            offerUpdatingTheMapLocationToReflectWhereValue();
        } else {
            proceedSaving();
        }

    }

    private void offerUpdatingTheMapLocationToReflectWhereValue() {
        final boolean removingWhereString = getWhereValue().trim().equals("");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.update_the_map_location));

        if(removingWhereString) {
            builder.setMessage(getResources().getString(R.string.because_place_of_the_event_has_been_removed));
        } else {
            builder.setMessage(getResources().getString(R.string.place_of_event_is_changing_to).replace("THIS_LOCATION",getWhereValue()));
        }


        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(removingWhereString) {
                    detailsFragment.setMapLocationInsertedThroughMap(null);
                    proceedSaving();
                } else {
                    userWantsToSearchForTheWhereString();
                }
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                proceedSaving();
            }
        });

        mapDialog = builder.create();
        mapDialog.show();
    }

    private void userWantsToSearchForTheWhereString() {
        List<Address> addresses = MapHelper.findMapAddressForString(EditActivityActivity.this, getWhereValue());
        if(addresses == null || addresses.size() == 0) {
            offerNothingFoundSaveAnyway();
        } else if (addresses.size() > 1) {
            offerTooManyResultsOpenInMap();
        } else {
            Address address = addresses.get(0);
            detailsFragment.setMapLocationInsertedThroughMap(new App4ItMapLocation(address.getLatitude(),address.getLongitude()));
            proceedSaving();
        }
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
                proceedSaving();
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

    private App4ItActivityParcel getExistingActivity() {
        return getIntent().getExtras().getParcelable(MessageIdentifiers.ACTIVITY_PARCEL);
    }

    private void proceedSaving() {
        final Button saveButton = getSaveButton();
        final Button deleteButton = getDeleteButton();

        if(detailsFragment.isEverythingValidAndGoodToSaveEventDetails()) {
            savingStarts(saveButton, deleteButton);

            final App4ItActivity draftActivity = detailsFragment.scrapActivity();
            final App4ItActivityParcel existingActivity = getExistingActivity();


            final List<NewsType> whatChanged = whatChangedInDraftActivity(existingActivity, draftActivity);
            final String currentActivityTitle = existingActivity.getTitle();

            FirebaseGateway firebaseGateway = new FirebaseGateway(this);
            firebaseGateway.updateActivityAttributes(existingActivity.getActivityId(), draftActivity.getTitle(), draftActivity.getMoreAbout(), draftActivity.getWhenFormat(), draftActivity.getWhenValue(), draftActivity.getWhereAsString(), draftActivity.getMapLocation(), draftActivity.getType(), new FirebaseActivityUpdateCallback() {
                @Override
                public void accept(FirebaseError firebaseError) {
                    if(firebaseError != null) {
                        savingEnds(saveButton, deleteButton);
                        UIHelper.showBriefMessage(getApplicationContext(), "Failed updating the event :-(. " + firebaseError.getMessage());
                    } else {
                        //lets post news about this
                        NewsCenter newsCenter = new NewsCenterImpl();
                        newsCenter.postNewsAboutEditedActivity(EditActivityActivity.this, existingActivity.getActivityId(), getDelegate().getLoggedInUserId(), whatChanged, currentActivityTitle, draftActivity.getTitle());

                        //we are done. go back
                        UIHelper.showBriefMessage(getApplicationContext(),getResources().getString(R.string.saved));
                        finish();
                    }
                }
            });
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        View detailsFragmentView = detailsFragment.getView();
        if(detailsFragmentView != null) {
            imm.hideSoftInputFromWindow(detailsFragmentView.getWindowToken(), 0);
        }
    }

    private void putInFragments() {
        if(!retrieveDetailsFragment()) putInDetailsFragment();
    }

    public void fillMeInWithActivityDetails(NewActivityDetailsFragment fragment) {
        fragment.putActivity((App4ItActivityParcel)getIntent().getExtras().getParcelable(MessageIdentifiers.ACTIVITY_PARCEL));

    }

    private boolean retrieveDetailsFragment() {
        detailsFragment = (NewActivityDetailsFragment)getFragmentManager().findFragmentByTag("detailsFragment");
        return detailsFragment != null;
    }

    private void putInDetailsFragment() {
        detailsFragment = new NewActivityDetailsFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.edit_activity_frame_container, detailsFragment, "detailsFragment");
        transaction.commit();
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
