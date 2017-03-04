package com.dreambig.app4it;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;

import com.dreambig.app4it.adapter.ActivitiesAdapter;
import com.dreambig.app4it.api.PhonebookCallback;
import com.dreambig.app4it.entity.BehaviourOverrides;
import com.dreambig.app4it.entity.FilterSettings;
import com.dreambig.app4it.entity.FirebaseUser;
import com.dreambig.app4it.fragment.ActivitiesListFragment;
import com.dreambig.app4it.helper.LogInGoHomeHelper;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.service.GcmIntentService;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.dreambig.app4it.helper.UIHelper;

import java.util.Map;

public class HomeActivity extends Activity {

    public static final int OPEN_COMMENTS_ACTIVITY_REQUEST = 1;
    public static final int OPEN_FILTER_ACTIVITY_REQUEST = 2;

    private boolean currentlyLoading;
    private ActivitiesListFragment activitiesListFragment;

    private String activityIdWhoseCommentsHaveBeenOpen; //this is a dirty work around for when comments activity gets shut without propagating the result (when the activity is open with FLAG_ACTIVITY_CLEAR_TOP)


    public void setActivityIdWhoseCommentsHaveBeenOpen(String activityId) {
        this.activityIdWhoseCommentsHaveBeenOpen = activityId;
    }

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK) {
            if (requestCode == OPEN_COMMENTS_ACTIVITY_REQUEST) {
                //L og.d("HomeActivity", "onResult from comments");
                String activityIdInQuestion = data.getStringExtra(MessageIdentifiers.ACTIVITY_ID);
                //L og.d("HomeActivity", "the activity id is: " + activityIdInQuestion);
                flatCountOnComments(activityIdInQuestion);
            } else if (requestCode == OPEN_FILTER_ACTIVITY_REQUEST) {
                //L og.d("HomeActivity", "onResult from filter");
                resetFilter();
            }
        } else if (resultCode == RESULT_CANCELED && requestCode == OPEN_COMMENTS_ACTIVITY_REQUEST) {
            //L og.d("HomeActivity", "onResult from comments was cancelled. Trying to use saved activity id");
            if(activityIdWhoseCommentsHaveBeenOpen != null) {
                flatCountOnComments(activityIdWhoseCommentsHaveBeenOpen);
            }
        } else if (resultCode == RESULT_CANCELED && requestCode == OPEN_FILTER_ACTIVITY_REQUEST) {
            //L og.d("HomeActivity","onResult from filter was cancelled. Still doing same thing as if it wasn't");
            resetFilter();
        }
    }

    private void resetFilter() {
        if(activitiesListFragment != null && activitiesListFragment.getListAdapter() != null) {
            ActivitiesAdapter activitiesAdapter = (ActivitiesAdapter)activitiesListFragment.getListAdapter();
            FilterSettings filterSettings = getDelegate().loadFilterSettings();
            checkUncheckFilterButtonBasedOnFilterSettings(filterSettings);
            activitiesAdapter.setFilterSettings(filterSettings);
            activitiesAdapter.reloadDisplay();
        }
    }

    private void checkUncheckFilterButtonBasedOnFilterSettings(FilterSettings filterSettings) {
        Button filterButton = (Button)findViewById(R.id.btnFilter);
        //somehow we once crashed here with filter button being null. see bugreport-2015-02-23-00-03-05.txt in downloads
        if(filterButton != null) {
            if(filterSettings.showingEverything()) {
                filterButton.setText(R.string.filter);
            } else {
                filterButton.setText(R.string.filter_checked);
            }
        } else {
            UIHelper.showBriefMessage(this,"Reconnecting..."); //this is here to tell us when it happens again...
        }
    }

    private void flatCountOnComments(String activityId) {
        if(activitiesListFragment != null && activitiesListFragment.getListAdapter() != null) {
            ((ActivitiesAdapter)activitiesListFragment.getListAdapter()).flatCountOnComments(activityId);
        }
    }

    public void activitiesAdapterStartsLoading() {
		findViewById(R.id.activities_container_layout).setVisibility(View.GONE);
		findViewById(R.id.activities_loading_notice).setVisibility(View.VISIBLE);
        currentlyLoading = true;
	}
	
	public void activitiesAdapterStopsLoading() {
        findViewById(R.id.activities_loading_notice).setVisibility(View.GONE);
        findViewById(R.id.activities_container_layout).setVisibility(View.VISIBLE);
        currentlyLoading = false;
	}

    public void downloadedXoutOfTotal(int x, int total) {
        float totalToHundredProportion = ((float)total)/100f;
        int progress = Math.round((total == 0 ? 100 : ((float)x) / totalToHundredProportion));
        ((ProgressBar)findViewById(R.id.activities_loading_notice_bar)).setProgress(Math.min(progress,100));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        protectFromBeingLoggedOut();
        //L og.d("HomeActivity", "onCreate()...");


        if(areWeFlowingThroughToStart() || areWeFlowingThroughToComments()) {
            setContentView(R.layout.activity_home_bounce);
        } else {
            bauOnCreate();
        }
    }

    //this will create potentially many listeners on the auth state. but it shouldn't break anything
    private void protectFromBeingLoggedOut() {
        App4ItApplication.PersistedInfo persistedInfo = getDelegate().getPersistedInfo();
        //shouldn't be null at this stage ever
        if(persistedInfo != null) {
            LogInGoHomeHelper.makeSureWeAreAlwaysLoggedIn(this,persistedInfo.getEmail(),persistedInfo.getPassword());
        }
    }

    private void bauOnCreate() {
        setContentView(R.layout.activity_home);
        prettyUpTopButtons();

        Button newActivityBtn = (Button)findViewById(R.id.btnNewActivity);
        newActivityBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), NewActivityActivity.class);
                startActivity(intent);
            }

        });

        Button filterBtn = (Button)findViewById(R.id.btnFilter);
        filterBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), FilterActivity.class);
                startActivityForResult(intent, OPEN_FILTER_ACTIVITY_REQUEST);
            }});

        Button contactsBtn = (Button)findViewById(R.id.btnContacts);
        contactsBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ContactsActivity.class);
                startActivity(intent);
            }
        });

        Button myProfileBtn = (Button)findViewById(R.id.btnMyProfile);
        myProfileBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MyProfileActivity.class);
                startActivity(intent);
            }
        });

        startActivityFeed();
    }

    private void prettyUpTopButtons() {
        Typeface tf = UIHelper.getOrCreateOurFont(this);

        Button contactsButton = (Button) findViewById(R.id.btnContacts);
        Button myProfileButton = (Button) findViewById(R.id.btnMyProfile);
        Button newActivityButton = (Button) findViewById(R.id.btnNewActivity);
        Button filterButton = (Button) findViewById(R.id.btnFilter);

        styleTopHomeButtonUp(contactsButton, tf);
        styleTopHomeButtonUp(myProfileButton, tf);
        styleTopHomeButtonUp(newActivityButton, tf);
        styleTopHomeButtonUp(filterButton, tf);
    }

    private void styleTopHomeButtonUp(Button button, Typeface tf) {
        button.setTextSize(17.0f);
        button.setTypeface(tf, Typeface.BOLD);
    }

    private boolean areWeFlowingThroughToStart() {
        Bundle extras = getIntent().getExtras();
        if(extras == null) {
            return false;
        } else {
            String defaultBehaviourOverride = extras.getString(MessageIdentifiers.OVERRIDE_DEFAULT_APP4IT_BEHAVIOUR);
            return BehaviourOverrides.GO_TO_START_ACTIVITY.equals(defaultBehaviourOverride);
        }
    }

    private boolean areWeFlowingThroughToComments() {
        Bundle extras = getIntent().getExtras();
        if(extras == null) {
            return false;
        } else {
            String defaultBehaviourOverride = extras.getString(MessageIdentifiers.OVERRIDE_DEFAULT_APP4IT_BEHAVIOUR);
            return BehaviourOverrides.GO_TO_COMMENTS.equals(defaultBehaviourOverride);
        }
    }

    private void startActivityFeed() {
        activitiesAdapterStartsLoading();
        //stick the list of activities in the view
        ActivitiesListFragment activitiesListFragment = new ActivitiesListFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.activities_container_layout, activitiesListFragment);
        transaction.commit();
        this.activitiesListFragment = activitiesListFragment;
    }

    public void activitiesAdapterIsReady(final ActivitiesAdapter activitiesAdapter) {
        //kick off feed of activities
        FilterSettings filterSettings = getDelegate().loadFilterSettings();
        checkUncheckFilterButtonBasedOnFilterSettings(filterSettings);
        activitiesAdapter.setFilterSettings(filterSettings);
        FirebaseGateway firebaseGateway = new FirebaseGateway(this);
        firebaseGateway.restartFirebaseFeed(new FirebaseUser(getDelegate().getLoggedInUserId(),getDelegate().getLoggedInUserNumber()), activitiesAdapter);
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		//L og.d("HomeActivity", "onResume()...");

        Bundle extras = getIntent().getExtras();
        if(areWeFlowingThroughToStart()) {
            goToStartActivity(extras);
        } else if (areWeFlowingThroughToComments()) {
            goToComments(extras);
        } else {
            //refresh of phonebook here
            getDelegate().activityStarts(new PhonebookCallback() {
                @Override
                public void phoneBookContacts(boolean refreshed, Map<String, String> numberToName) {
                    //refreshed or not, let's do a display reload
                    //a) button counters may have been flattened previously
                    if(activitiesListFragment != null && activitiesListFragment.getListAdapter() != null && !currentlyLoading) {
                        //L og.i("HomeActivity","Phone book refreshed: " + refreshed + ", reloading list anyway");
                        ((ActivitiesAdapter)activitiesListFragment.getListAdapter()).reloadDisplay();
                    }
                }
            });
        }
	}

    private void goToStartActivity(Bundle extras) {
        //L og.d("HomeActivity", "Flowing through to Start Activity...");
        Intent intent = new Intent(getApplicationContext(), StartActivity.class);
        if(isThereIntentToGoToComments(extras)) {
            //tell the start activity to go to comments rather than home
            intent.putExtra(MessageIdentifiers.OVERRIDE_DEFAULT_APP4IT_BEHAVIOUR,BehaviourOverrides.GO_TO_COMMENTS);
            intent.putExtra(MessageIdentifiers.ACTIVITY_ID, extras.getString(MessageIdentifiers.ACTIVITY_ID));
            intent.putExtra(MessageIdentifiers.ACTIVITY_TITLE, extras.getString(MessageIdentifiers.ACTIVITY_TITLE));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getDelegate().activityStops();
    }

    /*note that onPause will be called before and onResume after this method*/
    /*further: if this is called when this activity was used to fly-through only then we need special treatment*/
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();

        if(isThisEmptyHomeActivity() && !isThereIntentToGoToComments(extras)) {
            //well we need special treatment
            //getIntent().removeExtra(MessageIdentifiers.OVERRIDE_DEFAULT_APP4IT_BEHAVIOUR);
            //bauOnCreate(); - the above 2 lines were an alternative but was causing problems when the activity was recreated (after being stale for a while). as the override flag was still there
            Intent intentForNewHomeActivity = new Intent(getApplicationContext(), HomeActivity.class);
            intentForNewHomeActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intentForNewHomeActivity);
        } else {
            openCommentsIfApplicable(extras);
            //onResume will be now called so clear the flag saying it should do something differently in onResume then normal onResume. otherwise we could move to some other comments etc
            getIntent().removeExtra(MessageIdentifiers.OVERRIDE_DEFAULT_APP4IT_BEHAVIOUR); //this one should be ok as it shouldn't be in the intent used for recreation
        }
    }

    private void openCommentsIfApplicable(Bundle extras) {
        if(isThereIntentToGoToComments(extras)) {
            goToComments(extras);
        }
    }

    private void goToComments(Bundle extras) {
        Intent intentToRedirect = new Intent(this, CommentsActivity.class);
        intentToRedirect.putExtra(MessageIdentifiers.ACTIVITY_ID, extras.getString(MessageIdentifiers.ACTIVITY_ID));
        intentToRedirect.putExtra(MessageIdentifiers.ACTIVITY_TITLE, extras.getString(MessageIdentifiers.ACTIVITY_TITLE));

        if(isThisEmptyHomeActivity()) {
            //means this activity has been created as a flow through. so it has to be fully recreated on the way back
            intentToRedirect.putExtra(MessageIdentifiers.OVERRIDE_DEFAULT_APP4IT_BEHAVIOUR,BehaviourOverrides.CREATE_NEXT_ACTIVITY_IN_NEW_TASK);
            startActivity(intentToRedirect);
        } else {
            startActivityForResult(intentToRedirect, HomeActivity.OPEN_COMMENTS_ACTIVITY_REQUEST);
        }
    }

    private boolean isThereIntentToGoToComments(Bundle extras) {
        if(extras != null) {
            String additionalInstruction = extras.getString(MessageIdentifiers.ADDITIONAL_INSTRUCTION);
            return GcmIntentService.INSTRUCTION_NAVIGATE_TO_COMMENT.equals(additionalInstruction);
        } else {
            return false;
        }
    }

    private boolean isThisEmptyHomeActivity() {
        return findViewById(R.id.activity_home_bounce_emptiness) != null;
    }

}
