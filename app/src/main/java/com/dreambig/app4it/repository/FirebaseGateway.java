package com.dreambig.app4it.repository;

import android.content.Context;
import android.graphics.Bitmap;

import com.dreambig.app4it.App4ItApplication;
import com.dreambig.app4it.CommentsActivity;
import com.dreambig.app4it.adapter.ActivitiesAdapter;
import com.dreambig.app4it.adapter.CommentsAdapter;
import com.dreambig.app4it.api.FirebaseActivitySaveCallback;
import com.dreambig.app4it.api.FirebaseActivityUpdateCallback;
import com.dreambig.app4it.api.FirebaseAuthenticationCallback;
import com.dreambig.app4it.api.FirebaseCallCallback;
import com.dreambig.app4it.api.FirebaseCommentSavedCallback;
import com.dreambig.app4it.api.FirebaseHandleAcceptor;
import com.dreambig.app4it.api.FirebaseSnapshotCallback;
import com.dreambig.app4it.api.FirebaseStringProcessor;
import com.dreambig.app4it.api.FirebaseSuggestionsCallback;
import com.dreambig.app4it.api.FirebaseTransactionCallback;
import com.dreambig.app4it.api.FirebaseUserProfileCallback;
import com.dreambig.app4it.api.SuccessOrFailureCallback;
import com.dreambig.app4it.entity.App4ItActivity;
import com.dreambig.app4it.entity.App4ItComment;
import com.dreambig.app4it.entity.App4ItInvitationItem;
import com.dreambig.app4it.entity.App4ItMapLocation;
import com.dreambig.app4it.entity.App4ItNewsItem;
import com.dreambig.app4it.entity.App4ItSuggestion;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.entity.FirebaseUser;
import com.dreambig.app4it.enums.Format;
import com.dreambig.app4it.enums.InvitationStatus;
import com.dreambig.app4it.enums.NewsStatus;
import com.dreambig.app4it.enums.Preference;
import com.dreambig.app4it.enums.SuggestionType;
import com.dreambig.app4it.impl.App4ItUserProfileManager;
import com.dreambig.app4it.util.DateUtil;
import com.dreambig.app4it.util.ImageUtil;
import com.dreambig.app4it.util.Settings;
import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.ServerValue;
import com.firebase.client.Transaction;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alexandr on 25/12/2014.
 */
public class FirebaseGateway {

    private static final String FIREBASE_DATA_NOT_SET = "NOT_SET";
    private static boolean initialized = false;
    private static boolean prioritizedAttemptInProgress = false;
    private int numberOfInitiallyDownloadedActivities;
    private int numberOfProcessedActivities;

    public FirebaseGateway(Context context) {
        if(!initialized) {
            Firebase.setAndroidContext(context);
            initialized = true;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public void registerNewAccount(String email, String password, final FirebaseCallCallback callCallback) {
        Firebase ref = new Firebase(Settings.getFirebaseUrl());
        ref.createUser(email,password,new Firebase.ResultHandler() {
            @Override
            public void onSuccess() {
                callCallback.onSuccess();
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                callCallback.onFailure(firebaseError);
            }
        });
    }

    public void logIn(String email, String password, final boolean prioritisedAttempt, final FirebaseAuthenticationCallback callback) {

        if(prioritisedAttempt) {
            prioritizedAttemptInProgress = true;
        } else {
            if(prioritizedAttemptInProgress) {
                //we don't want this less important log in attempt to interfere
                callback.onWeIgnoredIt();
                return;
            }
        }

        Firebase ref = new Firebase(Settings.getFirebaseUrl());

        ref.authWithPassword(email,password,new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                if(prioritisedAttempt) {
                    prioritizedAttemptInProgress = false;
                }
                callback.onSuccess(authData);
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                if(prioritisedAttempt) {
                    prioritizedAttemptInProgress = false;
                }
                callback.onFailure(firebaseError);
            }
        });
    }

    public void watchAuthenticationState(Firebase.AuthStateListener listener) {
        Firebase ref = new Firebase(Settings.getFirebaseUrl());

        ref.addAuthStateListener(listener);
    }

    public void storeNews(App4ItNewsItem newsItem) {
        Firebase newNewsRef = new Firebase(Settings.getFirebaseUrl()).child("news").push();

        Map<String,Object> newsData = new HashMap<>();
        newsData.put("createdBy", newsItem.getCreatedByUserId());
        newsData.put("createdForUserId", newsItem.getCreatedForUserId());
        newsData.put("createdOn", ServerValue.TIMESTAMP);
        newsData.put("newsType", newsItem.getNewsType().toString());
        newsData.put("subjectId", newsItem.getSubjectId());
        newsData.put("subjectTitle", newsItem.getSubjectTitle());
        newsData.put("additionalValue", newsItem.getAdditionalValue());
        newsData.put("status", newsItem.getStatus().toString());

        newNewsRef.setValue(newsData);
    }

    public void logOutFromFirebase() {
        Firebase ref = new Firebase(Settings.getFirebaseUrl());
        ref.unauth();
    }

    public void createBasicEntries(FirebaseUser user, String email) {

        Map<String,Object> attributes = new HashMap<>();
        attributes.put("email",email);
        attributes.put("createdOn",new Date().getTime());

        Firebase usersRef = new Firebase(Settings.getFirebaseUrl()).child("users").child(user.getUserId()).child("attributes");
        usersRef.setValue(attributes);

        //add index number to user id so others can find them
        Firebase refToIndex1 = new Firebase(Settings.getFirebaseUrl()).child("index").child("numberToUserId").child(user.getUserNumber());
        refToIndex1.setValue(user.getUserId());

        //add index user id to number so users can be mapped to names on people's phones
        Firebase refToIndex2 = new Firebase(Settings.getFirebaseUrl()).child("index").child("userIdToNumber").child(user.getUserId());
        refToIndex2.setValue(user.getUserNumber());
    }

    public void restartFirebaseFeed(final FirebaseUser loggedInUser, final ActivitiesAdapter activitiesAdapter) {
        //L og.d("FirebaseGateway","Restarting Firebase Activities feed");

        removeObservers(); //this doesn't do anything now

        //download everything the user is now involved in
        Firebase currentRef = new Firebase(Settings.getFirebaseUrl()).child("users").child(loggedInUser.getUserId()).child("involvedIn");
        currentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<App4ItActivity> allCurrentActivities = parseInvolvedInSnapshotToActivities(dataSnapshot);
                //now record how many of these initital activities there are
                numberOfInitiallyDownloadedActivities = allCurrentActivities.size();
                numberOfProcessedActivities = 0;
                //L og.d("FirebaseGateway","Expecting " + numberOfInitiallyDownloadedActivities + " activities initially");

                startRealTimeFeedIfSuitable(false,activitiesAdapter,loggedInUser); //the real time feed may start if there are not activities now

                for(App4ItActivity activity : allCurrentActivities) {
                    if(activitiesAdapter.containsActivity(activity)) {
                        //restart firebase feed is called only once so the activity should never already be in the view
                        startRealTimeFeedIfSuitable(true,activitiesAdapter,loggedInUser);
                    } else {
                        addNewActivity(activity,activitiesAdapter,loggedInUser,true);
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


    }

    public void setUserAsGoing(String activityId, String userId) {
        Firebase currentRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityId).child("invitationList").child(userId).child("status");
        currentRef.setValue(InvitationStatus.GOING);
    }

    public void setUserAsNotGoing(String activityId, String userId) {
        Firebase currentRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityId).child("invitationList").child(userId).child("status");

        currentRef.setValue(InvitationStatus.NOT_GOING);
    }

    public void setUserAsHavingDeletedTheInvitation(String activityId, String userId) {
        Firebase currentRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityId).child("invitationList").child(userId).child("status");
        currentRef.setValue(InvitationStatus.DELETED);
    }

    public String storeCommentForActivity(String activityIdentifier, String loggedInUserIdentifier, String loggedInUserPhoneNumber, String text, final FirebaseCommentSavedCallback afterSaveCallback) {

        Firebase newCommentRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("comments").push();

        long createdOn = new Date().getTime();

        Map<String,Object> commentAttributes = new HashMap<>();
        commentAttributes.put("createdBy", loggedInUserIdentifier);
        commentAttributes.put("createdByNumber", loggedInUserPhoneNumber);
        commentAttributes.put("createdOn", createdOn);
        commentAttributes.put("text", text);

        Map<String,Object> seen = new HashMap<>();
        seen.put(loggedInUserIdentifier, NewsStatus.NOTIFIED_ABOUT.toString());

        Map<String,Object> wholeRecord = new HashMap<>();
        wholeRecord.put("attributes", commentAttributes);
        wholeRecord.put("seen", seen);

        newCommentRef.setValue(wholeRecord, ServerValue.TIMESTAMP, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if(firebase != null) {
                    String commentId = firebase.getKey();
                    if(firebaseError == null) afterSaveCallback.accept(commentId, CommentsActivity.PostStatus.SENT);
                    else afterSaveCallback.accept(commentId, CommentsActivity.PostStatus.FAILED);
                }
            }
        }); //server side timestamp

        return newCommentRef.getKey();
    }

    private void removeObservers() {
        //it doesn't do anything
    }

    private List<App4ItActivity> parseInvolvedInSnapshotToActivities(DataSnapshot input) {
        List<App4ItActivity> ret = new ArrayList<>();

        if(input != null) {
            List<App4ItActivity> invitedTo = parseInvolvedInChildToActivities(input.child("invitedTo"));
            List<App4ItActivity> userCreated = parseInvolvedInChildToActivities(input.child("userCreated"));

            ret.addAll(invitedTo);
            ret.addAll(userCreated);
        }

        return ret;
    }

    private List<App4ItActivity> parseInvolvedInChildToActivities(DataSnapshot input) {
        List<App4ItActivity> ret = new ArrayList<>();

        if(input != null) {
            for (DataSnapshot activitySnapshot : input.getChildren()) {
                App4ItActivity activity = new App4ItActivity(activitySnapshot.getKey());
                ret.add(activity);
            }
        }

        return ret;
    }

    private void addNewActivity(final App4ItActivity activity, final ActivitiesAdapter activitiesAdapter, final FirebaseUser loggedInUser, final boolean isInitialLoad) {

        //the activity object needs to be enriched before saved for displaying
        Firebase activityRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activity.getActivityId());

        activityRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                App4ItApplication delegate = activitiesAdapter.getDelegate();
                enrichWithAttributes(activity,dataSnapshot.child("attributes"),delegate);
                enrichWithInvitationList(activity,dataSnapshot.child("invitationList"),delegate,loggedInUser);
                enrichWithComments(activity,dataSnapshot.child("comments"),loggedInUser);

                enrichWithSuggestions(activity,dataSnapshot.child("whenSuggestions"),loggedInUser,SuggestionType.TIME);
                enrichWithSuggestions(activity,dataSnapshot.child("whereSuggestions"),loggedInUser,SuggestionType.PLACE);

                enrichWithAttributesChangeListener(activity,activitiesAdapter);
                enrichWithInvitationListChangeListener(activity,activitiesAdapter,delegate,loggedInUser);
                enrichWithCommentsChangeListener(activity,loggedInUser,activitiesAdapter);

                enrichWithSuggestionsChangeListener(activity,loggedInUser,activitiesAdapter,delegate,SuggestionType.TIME);
                enrichWithSuggestionsChangeListener(activity,loggedInUser,activitiesAdapter,delegate,SuggestionType.PLACE);

                activitiesAdapter.addActivity(activity);

                if(isInitialLoad) {
                    //there will be potentialy more activities coming down so refresh view only 'conditionally'
                    startRealTimeFeedIfSuitable(true,activitiesAdapter,loggedInUser);
                } else {
                    //nothing to wait for after activity is added. go and refresh view
                    App4ItUserProfileManager.addToCacheUsersInActivities(activitiesAdapter.getContext(), Arrays.asList(activity),loggedInUser.getUserId(),new App4ItUserProfileManager.AddingToCacheCallback() {
                        @Override
                        public void completed() {
                            activitiesAdapter.reloadDisplay();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    private void enrichWithInvitationListChangeListener(final App4ItActivity activity, final ActivitiesAdapter activitiesAdapter, final App4ItApplication delegate, final FirebaseUser loggedInUser) {


        Firebase invitationListRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activity.getActivityId()).child("invitationList");

        //like in attributes listener, this may cause potentialy a multiple reload. if a people are answer to invites at same time
        invitationListRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                processInvitation(activity,dataSnapshot,activitiesAdapter,delegate,loggedInUser);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                processInvitation(activity,dataSnapshot,activitiesAdapter,delegate,loggedInUser);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    //mind that becase this is called for FEventTypeChildAdded it will be called for already stored invitations too
    //@todo here we don't have to reload all display. it would be enough to target the one grid view
    private void processInvitation(App4ItActivity activity, DataSnapshot snapshot, final ActivitiesAdapter activitiesAdapter,App4ItApplication delegate, FirebaseUser loggedInUser) {

        //L og.d("FirebaseGateway","Firebase, processing invitation");

        InvitationStatus currentStatus = InvitationStatus.valueOf((String)snapshot.child("status").getValue());
        App4ItInvitationItem storedInvitation = findItemForUser(snapshot.getKey(),activity.getInvitationList());

        if(storedInvitation == null) {
            App4ItUser user = new App4ItUser(snapshot.getKey());
            user.setNumber((String)snapshot.child("userNumber").getValue());
            user.setName(delegate.getNameFromPhoneNumber(user.getNumber()));

            storedInvitation = new App4ItInvitationItem();
            storedInvitation.setUser(user);
            storedInvitation.setStatus(currentStatus);

            activity.addInvitationItemKeepSorted(storedInvitation);
        } else {
            if(storedInvitation.getStatus().equals(currentStatus)) {
                //we already have the invitation and it has the same status. so nothing to do
                return;
            }
            storedInvitation.setStatus(currentStatus);
            activity.ensureInvitationItemsAreSorted();
        }

        //make sure the usersGoing array reflects current state
        if(InvitationStatus.GOING.equals(currentStatus)) {
            if(!activity.getUsersGoing().contains(storedInvitation.getUser())) {
                activity.getUsersGoing().add(storedInvitation.getUser());
            }
        } else {
            //removing non existing object is fine
            activity.getUsersGoing().remove(storedInvitation.getUser());
        }

        //change loggedInUser status if applicable
        if(storedInvitation.getUser().getUserId().equals(loggedInUser.getUserId())) {
            activity.setLoggedInUserStatus(storedInvitation.getStatus());
        }

        //make sure all in user profiles cache and reload
        App4ItUserProfileManager.addToCacheUsersInActivities(activitiesAdapter.getContext(),Arrays.asList(activity),loggedInUser.getUserId(),new App4ItUserProfileManager.AddingToCacheCallback() {
            @Override
            public void completed() {
                activitiesAdapter.reloadDisplay();
            }
        });

    }

    private App4ItInvitationItem findItemForUser(String userIdentifier, List<App4ItInvitationItem> invitationItems) {

        for(App4ItInvitationItem item : invitationItems) {
            if(item.getUser().getUserId().equals(userIdentifier)) {
                return item;
            }
        }

        return null;
    }

    private void enrichWithAttributesChangeListener(final App4ItActivity activity, final ActivitiesAdapter activitiesAdapter) {

        Firebase attributesRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activity.getActivityId()).child("attributes");

        //note that the table will get reloaded multiple times if there's a change on multiple attributes. but it doesn't seem to be visible at all
        //and don't worry, it's not called when the activity is being removed
        attributesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String subjectOfChange = dataSnapshot.getKey();

                if("title".equals(subjectOfChange)) {
                    activity.setTitle((String)dataSnapshot.getValue());
                    activitiesAdapter.reloadDisplay();
                } else if ("description".equals(subjectOfChange)) {
                    activity.setMoreAbout((String)dataSnapshot.getValue());
                    activitiesAdapter.reloadDisplay();
                } else if ("when".equals(subjectOfChange)) {
                    activity.setWhenAsString(whenSnapshotToString(dataSnapshot));
                    activity.setWhenFormat(Format.valueOf((String)dataSnapshot.child("format").getValue()));
                    activity.setWhenValue((String)dataSnapshot.child("value").getValue());
                    activitiesAdapter.reloadDisplay();
                } else if ("where".equals(subjectOfChange)) {
                    activity.setWhereAsString((String)dataSnapshot.getValue());
                    activitiesAdapter.reloadDisplay();
                } else if ("whereCoordinates".equals(subjectOfChange)) {
                    activity.setMapLocation(whereCoordinatesSnapshotToMapLocation(dataSnapshot));
                    activitiesAdapter.reloadDisplay();
                } else if ("type".equals(subjectOfChange)) {
                    activity.setType((String)dataSnapshot.getValue());
                    activitiesAdapter.reloadDisplay();
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


    }

    private void enrichWithAttributes(App4ItActivity activity, DataSnapshot input, App4ItApplication delegate) {
        activity.setTitle((String)input.child("title").getValue());
        activity.setMoreAbout((String)input.child("description").getValue());
        activity.setWhereAsString((String)input.child("where").getValue());
        activity.setMapLocation(whereCoordinatesSnapshotToMapLocation(input.child("whereCoordinates")));
        activity.setWhenAsString(whenSnapshotToString(input.child("when")));
        activity.setWhenFormat(Format.valueOf((String)input.child("when/format").getValue()));
        activity.setWhenValue((String)input.child("when/value").getValue());
        activity.setType((String) input.child("type").getValue());
        activity.setCreatedOn((Long) input.child("createdOn").getValue());
        activity.setCreatedByUserId((String) input.child("createdBy").getValue());
        activity.setCreatedByNumber((String) input.child("createdByNumber").getValue());
        activity.setCreatedByName(delegate.getNameFromPhoneNumber(activity.getCreatedByNumber()));
    }

    private void enrichWithInvitationList(App4ItActivity activity, DataSnapshot input, App4ItApplication delegate, FirebaseUser loggedInUser) {
        activity.setInvitationList(new ArrayList<App4ItInvitationItem>());
        activity.setUsersGoing(new ArrayList<App4ItUser>());

        if(input != null) {
            for(DataSnapshot userInvitation : input.getChildren()) {
                App4ItInvitationItem invitationItem = new App4ItInvitationItem();

                App4ItUser user = new App4ItUser(userInvitation.getKey());
                user.setNumber((String) userInvitation.child("userNumber").getValue());
                user.setName(delegate.getNameFromPhoneNumber(user.getNumber()));

                invitationItem.setUser(user);
                invitationItem.setStatus(InvitationStatus.valueOf((String) userInvitation.child("status").getValue()));

                activity.addInvitationItemKeepSorted(invitationItem);

                if(InvitationStatus.GOING.equals(invitationItem.getStatus())) {
                    activity.getUsersGoing().add(user);
                }

                if(invitationItem.getUser().getUserId().equals(loggedInUser.getUserId()) ) {
                    activity.setLoggedInUserStatus(invitationItem.getStatus());
                }
            }
        }
    }

    private List<App4ItSuggestion> parseSuggestionsFromSnapshot(DataSnapshot input, App4ItApplication delegate) {

        List<App4ItSuggestion> ret = new ArrayList<>();

        if(input != null) {
            for(DataSnapshot suggestionSnapshot : input.getChildren()) {
                App4ItSuggestion suggestion = parseSuggestionSnapshot(suggestionSnapshot, delegate);

                ret.add(suggestion);
            }
        }

        return ret;
    }

    private void enrichWithCommentsChangeListener(final App4ItActivity activity, final FirebaseUser loggedInUser, final ActivitiesAdapter activitiesAdapter) {
        Firebase commentsRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activity.getActivityId()).child("comments");

        commentsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String s) {
                boolean hasLoggedInUserSeenThis = true;

                DataSnapshot seenBy = snapshot.child("seen");
                if(seenBy != null && seenBy.exists()) {
                    DataSnapshot seenByLoggedInUserStatus = seenBy.child(loggedInUser.getUserId());

                    if(seenByLoggedInUserStatus == null || !NewsStatus.NOTIFIED_ABOUT.toString().equals(seenByLoggedInUserStatus.getValue())) {
                        hasLoggedInUserSeenThis = false;
                    }
                } else {
                    hasLoggedInUserSeenThis=false;
                }

                if(!hasLoggedInUserSeenThis) {
                    activity.setUnseenCommentsRealtime(activity.getUnseenCommentsRealtime() + 1);

                    if(activity.getUnseenCommentsRealtime() > activity.getUnseenComments()) {
                        //ok, we got over the initialy downloaded number
                        activity.setUnseenComments(activity.getUnseenCommentsRealtime());
                        //L og.d("FirebaseGateway","Reloading activities due to new comment");
                        activitiesAdapter.reloadDisplay();
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    private void enrichWithSuggestionsChangeListener(final App4ItActivity activity, final FirebaseUser loggedInUser, final ActivitiesAdapter activitiesAdapter, App4ItApplication delegate, final SuggestionType suggestionType) {

        String referenceBucket;
        if(suggestionType.equals(SuggestionType.TIME)) {
            referenceBucket = "whenSuggestions";
        } else {
            referenceBucket = "whereSuggestions";
        }

        Firebase suggestionsRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activity.getActivityId()).child(referenceBucket);

        suggestionsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String s) {
                boolean hasLoggedInUserSeenThis = true;

                DataSnapshot seenBy = snapshot.child("seen");
                if(seenBy != null && seenBy.exists()) {
                    DataSnapshot seenByLoggedInUserStatus = seenBy.child(loggedInUser.getUserId());

                    if(seenByLoggedInUserStatus == null || !NewsStatus.NOTIFIED_ABOUT.toString().equals(seenByLoggedInUserStatus.getValue())) {
                        hasLoggedInUserSeenThis = false;
                    }
                } else {
                    hasLoggedInUserSeenThis=false;
                }

                if(!hasLoggedInUserSeenThis) {

                    if(suggestionType.equals(SuggestionType.TIME)) {
                        activity.setUnseenWhenSuggestionsRealtime(activity.getUnseenWhenSuggestionsRealtime() + 1);

                        if(activity.getUnseenWhenSuggestionsRealtime() > activity.getUnseenWhenSuggestions()) {
                            //ok, we got over the initialy downloaded number
                            activity.setUnseenWhenSuggestions(activity.getUnseenWhenSuggestionsRealtime());
                            //L og.d("FirebaseGateway","Reloading activities due to new time suggestion");
                            activitiesAdapter.reloadDisplay();
                        }
                    } else {
                        activity.setUnseenWhereSuggestionsRealtime(activity.getUnseenWhereSuggestionsRealtime() + 1);

                        if(activity.getUnseenWhereSuggestionsRealtime() > activity.getUnseenWhereSuggestions()) {
                            //ok, we got over the initialy downloaded number
                            activity.setUnseenWhereSuggestions(activity.getUnseenWhereSuggestionsRealtime());
                            //L og.d("FirebaseGateway","Reloading activities due to new place suggestion");
                            activitiesAdapter.reloadDisplay();
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    private void enrichWithComments(App4ItActivity activity, DataSnapshot input, FirebaseUser loggedInUser) {

        int nUnseenComments = 0;

        if(input != null) {
            for(DataSnapshot comment : input.getChildren()) {
                DataSnapshot seenBy = comment.child("seen");
                if(seenBy != null && seenBy.exists()) {
                    DataSnapshot seenByLoggedInUserStatus = seenBy.child(loggedInUser.getUserId());
                    if(seenByLoggedInUserStatus == null || !NewsStatus.NOTIFIED_ABOUT.toString().equals(seenByLoggedInUserStatus.getValue())) {
                        nUnseenComments++;
                    }
                } else {
                    nUnseenComments++;
                }
            }
        }

        activity.setUnseenComments(nUnseenComments);
    }

    private void enrichWithSuggestions(App4ItActivity activity, DataSnapshot input, FirebaseUser loggedInUser, SuggestionType suggestionType) {

        int nUnseenSuggestions = 0;

        if(input != null) {
            for(DataSnapshot suggestion : input.getChildren()) {
                DataSnapshot seenBy = suggestion.child("seen");
                if(seenBy != null && seenBy.exists()) {
                    DataSnapshot seenByLoggedInUserStatus = seenBy.child(loggedInUser.getUserId());
                    if(seenByLoggedInUserStatus == null || !NewsStatus.NOTIFIED_ABOUT.toString().equals(seenByLoggedInUserStatus.getValue())) {
                        nUnseenSuggestions++;
                    }
                } else {
                    nUnseenSuggestions++;
                }
            }
        }

        if(suggestionType.equals(SuggestionType.TIME)) {
            activity.setUnseenWhenSuggestions(nUnseenSuggestions);
        } else {
            activity.setUnseenWhereSuggestions(nUnseenSuggestions);
        }
    }

    private void startRealTimeFeedIfSuitable(boolean activityProcessed, final ActivitiesAdapter activitiesAdapter, final FirebaseUser loggedInUser) {
        if(activityProcessed) {
            numberOfProcessedActivities++;
        }

        activitiesAdapter.downloadedXoutOfTotal(numberOfProcessedActivities, numberOfInitiallyDownloadedActivities);

        if(numberOfProcessedActivities >= numberOfInitiallyDownloadedActivities) {

            App4ItUserProfileManager.addToCacheUsersInActivities(activitiesAdapter.getContext(),activitiesAdapter.getActivities(),loggedInUser.getUserId(),new App4ItUserProfileManager.AddingToCacheCallback() {
                @Override
                public void completed() {
                    //L og.d("FirebaseGateway","It's suitable to start the feed. Number of initial activities: " + numberOfInitiallyDownloadedActivities);
                    activitiesAdapter.sortActivities();
                    activitiesAdapter.doneWithInitialLoad();
                    activitiesAdapter.reloadDisplay();
                    startRealTimeActivitiesFeed(loggedInUser,activitiesAdapter);
                }
            });

        }
    }

    private void startRealTimeActivitiesFeed(FirebaseUser loggedInUser, ActivitiesAdapter activitiesAdapter) {
        Firebase invitedToRef = new Firebase(Settings.getFirebaseUrl()).child("users").child(loggedInUser.getUserId()).child("involvedIn").child("invitedTo");
        Firebase userCreatedRef = new Firebase(Settings.getFirebaseUrl()).child("users").child(loggedInUser.getUserId()).child("involvedIn").child("userCreated");

        startRealTimeActivitiesFeedForReference(invitedToRef,activitiesAdapter,loggedInUser);
        startRealTimeActivitiesFeedForReference(userCreatedRef,activitiesAdapter,loggedInUser);
    }

    private void startRealTimeActivitiesFeedForReference(Firebase firebaseRef, final ActivitiesAdapter activitiesAdapter, final FirebaseUser loggedInUser) {

        firebaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                //L og.d("FirebaseGateway","Activity downloaded realtime");

                App4ItActivity activity = new App4ItActivity(dataSnapshot.getKey());

                if(!activitiesAdapter.containsActivity(activity)) {
                    //L og.d("FirebaseGateway", "And it's new!");
                    addNewActivity(activity,activitiesAdapter,loggedInUser,false);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //L og.d("FirebaseGateway","Activity has been removed");

                App4ItActivity activity = new App4ItActivity(dataSnapshot.getKey());

                if(activitiesAdapter.containsActivity(activity)) {
                    //L og.d("FirebaseGateway", "And it's being displayed");
                    activitiesAdapter.removeActivity(activity);
                    activitiesAdapter.reloadDisplay();
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public void restartCommentsFeed(final String activityIdentifier, final CommentsAdapter commentsAdapter, final App4ItApplication delegate, final String loggedInUserIdentifier, final FirebaseHandleAcceptor handleAcceptor) {

        Firebase commentsRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("comments");

        commentsRef.orderByPriority().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot != null && snapshot.exists()) {
                    for(DataSnapshot commentSnapshot : snapshot.getChildren()) {
                        App4ItComment comment = parseCommentSnapshot(commentSnapshot, delegate);

                        if(comment != null) {
                            commentsAdapter.addComment(comment);
                        }

                        //mark it as seen. although it may be already marked. using snapshot.getKey here to avoid using comment object (may be null)
                        markCommentAsSeen(activityIdentifier,commentSnapshot.getKey(),loggedInUserIdentifier);
                    }
                }
                //whether there was anything or not, reload the view. after you get user profiles.
                App4ItUserProfileManager.addToCacheUsersInComments(commentsAdapter.getContext(),commentsAdapter.getComments(),loggedInUserIdentifier, new App4ItUserProfileManager.AddingToCacheCallback() {
                    @Override
                    public void completed() {
                        commentsAdapter.reloadDisplay();
                        commentsAdapter.doneWithInitialLoad();

                        //and start the realtime comments feed
                        ChildEventListener handle = startRealTimeCommentsFeed(activityIdentifier, commentsAdapter, delegate, loggedInUserIdentifier);


                        if(handleAcceptor != null) {
                            handleAcceptor.accept(handle);
                        }
                    }
                });

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                //L og.e("FirebaseGateway", "Restart comments feed cancelled");
            }
        });

    }

    public void stopRealTimeCommentsFeed(String activityIdentifier, ChildEventListener listener) {
        Firebase commentsRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("comments");
        commentsRef.removeEventListener(listener);
    }

    private ChildEventListener startRealTimeCommentsFeed(final String activityIdentifier, final CommentsAdapter commentsAdapter, final App4ItApplication delegate, final String loggedInUserIdentifier) {

        Firebase commentsRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("comments");

        return commentsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String s) {
                final App4ItComment comment = parseCommentSnapshot(snapshot, delegate);

                //we do the check on contains because this will loop through the comments fed initially
                if(comment != null && !commentsAdapter.containsComment(comment)) {
                    commentsAdapter.addComment(comment);
                    //mark it as seen. although it may already be marked. after user profile presence ensured
                    App4ItUserProfileManager.addToCacheUsersInComments(commentsAdapter.getContext(),Arrays.asList(comment),loggedInUserIdentifier,new App4ItUserProfileManager.AddingToCacheCallback() {
                        @Override
                        public void completed() {
                            markCommentAsSeen(activityIdentifier, comment.getIdentifier(), loggedInUserIdentifier);
                            commentsAdapter.reloadDisplay();
                        }
                    });
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    public void retrieveInvitationListForActivity(String activityIdentifier, final FirebaseSnapshotCallback onComplete) {

        Firebase invitationListRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("invitationList");

        invitationListRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                onComplete.processSnapshot(true,snapshot,null);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                onComplete.processSnapshot(false,null,firebaseError.getMessage());
            }
        });

    }

    private void markCommentAsSeen(String activityIdentifier, String commentIdentifier, String loggedInUserIdentifier) {

        Firebase commentRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("comments").child(commentIdentifier).child("seen").child(loggedInUserIdentifier);
        commentRef.setValue(NewsStatus.NOTIFIED_ABOUT);

    }

    private void markSuggestionAsSeen(String activityIdentifier, String suggestionIdentifier, String loggedInUserIdentifier, SuggestionType suggestionType) {

        String referenceBucket;
        if(suggestionType.equals(SuggestionType.TIME)) {
            referenceBucket = "whenSuggestions";
        } else {
            referenceBucket = "whereSuggestions";
        }

        Firebase suggestionRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child(referenceBucket).child(suggestionIdentifier).child("seen").child(loggedInUserIdentifier);

        suggestionRef.setValue(NewsStatus.NOTIFIED_ABOUT);
    }

    public void saveResponseToSuggestion(SuggestionType type, String loggedInUserId, String loggedInUserNumber, String activityId, Preference preference, String suggestionId) {
        String suggestionPath = (type == SuggestionType.TIME ? "whenSuggestions" : "whereSuggestions");
        Firebase firebase = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityId).child(suggestionPath).child(suggestionId).child("responses").child(loggedInUserId);
        Map<String,Object> toSet = new HashMap<String,Object>();
        toSet.put("answer",preference.toString());
        toSet.put("userNumber",loggedInUserNumber);
        firebase.setValue(toSet);
    }

    private App4ItSuggestion parseSuggestionSnapshot(DataSnapshot snapshot, App4ItApplication delegate) {

        DataSnapshot attributes = snapshot.child("attributes");

        //look at parseCommentSnapshot for why this is here
        if(attributes != null && attributes.getChildrenCount() > 0) {
            App4ItSuggestion suggestion = new App4ItSuggestion(snapshot.getKey());

            suggestion.setFormat(Format.valueOf((String)attributes.child("format").getValue()));
            suggestion.setValue((String)attributes.child("value").getValue());
            suggestion.setMapLocation(whereCoordinatesSnapshotToMapLocation(attributes.child("whereCoordinates")));


            //read responses
            DataSnapshot responses = snapshot.child("responses");

            if(responses != null) {

                for(DataSnapshot response : responses.getChildren()) {
                    String userIdentifier = response.getKey();
                    Preference answer = Preference.valueOf((String)response.child("answer").getValue());

                    App4ItUser user = new App4ItUser(userIdentifier);
                    user.setNumber((String) response.child("userNumber").getValue());
                    user.setName(delegate.getNameFromPhoneNumber(user.getNumber()));

                    suggestion.addResponseForUser(user,answer);
                }

            }

            return suggestion;
        } else {
            return null;
        }

    }

    private App4ItComment parseCommentSnapshot(DataSnapshot snapshot, App4ItApplication delegate) {

        DataSnapshot attributes = snapshot.child("attributes");

        //strange stuff here happened as the attributes were sometimes empty as the real time feed started
        if(attributes != null && attributes.getChildrenCount() > 0) {
            App4ItComment comment = new App4ItComment(snapshot.getKey());

            comment.setCreatedBy((String)attributes.child("createdBy").getValue());
            comment.setCreatedByNumber((String) attributes.child("createdByNumber").getValue());
            comment.setCreatedByName(delegate.getNameFromPhoneNumber(comment.getCreatedByNumber()));
            comment.setCreatedOn((Long)attributes.child("createdOn").getValue());
            comment.setText((String)attributes.child("text").getValue());

            return comment;
        } else {
            return null;
        }

    }

    //this is to be used in conjunction with "addToUsersInvitedToBucket" method
    public void inviteUserToActivity(String activityIdentifier, String userIdentifier, final String userNumber, final FirebaseTransactionCallback onComplete) {


        Firebase userInvitationRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("invitationList").child(userIdentifier);

        //the NO for the immediate states... could be perhaps YES too
        userInvitationRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null || !(mutableData.getValue() instanceof Map) || ((Map)mutableData.getValue()).size() == 0) {
                    //ok, we can invite them
                    Map<String,Object> dataToSet = new HashMap<String, Object>();
                    dataToSet.put("status", InvitationStatus.INVITED.toString());
                    dataToSet.put("userNumber", userNumber);


                    mutableData.setValue(dataToSet);
                    return Transaction.success(mutableData);
                } else {
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(FirebaseError firebaseError, boolean committed, DataSnapshot snapshot) {
                //delegate to the callback method
                onComplete.transactionEnded(firebaseError, committed, snapshot);
            }
        });

    }

    public void inviteUserToActivityNoTransaction(String activityIdentifier, String userIdentifier, final String userNumber) {
        Firebase userInvitationRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("invitationList").child(userIdentifier);

        //ok, we can invite them
        Map<String,Object> dataToSet = new HashMap<>();
        dataToSet.put("status", InvitationStatus.INVITED);
        dataToSet.put("userNumber", userNumber);


        userInvitationRef.setValue(dataToSet);
    }

    public void addToUsersInvitedToBucket(String activityIdentifier, String invitedUserIdentifier, String activityByUserIdentifier, String invitedByUserIdentifier) {
        Firebase userInvitedToRef = new Firebase(Settings.getFirebaseUrl()).child("users").child(invitedUserIdentifier).child("involvedIn").child("invitedTo").child(activityIdentifier);

        long createdOn = new Date().getTime();

        Map<String,Object> attributes = new HashMap<>();
        attributes.put("invitedBy",invitedByUserIdentifier);
        attributes.put("activityBy",activityByUserIdentifier);
        attributes.put("createdOn",createdOn);

        Map<String,Object> dataToSet = new HashMap<>();
        dataToSet.put("attributes", attributes);

        userInvitedToRef.setValue(dataToSet, createdOn);
    }

    public void removeFromUsersInvitedToBucket(String activityIdentifier, String userIdentifier) {

        Firebase userInvitedToRef = new Firebase(Settings.getFirebaseUrl()).child("users").child(userIdentifier).child("involvedIn").child("invitedTo").child(activityIdentifier);
        userInvitedToRef.removeValue();
    }

    public void addToUsersUserCreatedBucket(String userIdentifier, String activityIdentifier) {

        Firebase userCreatedBucket = new Firebase(Settings.getFirebaseUrl()).child("users").child(userIdentifier).child("involvedIn").child("userCreated").child(activityIdentifier);

        Map<String,Object> data = new HashMap<>();
        data.put("createdOn", (new Date().getTime()));

        userCreatedBucket.setValue(data);
    }

    public void removeFromUserCreatedBucket(String userIdentifier, String activityIdentifier) {

        Firebase userCreatedBucket = new Firebase(Settings.getFirebaseUrl()).child("users").child(userIdentifier).child("involvedIn").child("userCreated").child(activityIdentifier);

        userCreatedBucket.removeValue();
    }

    public void removeNotificationPreference(String activityIdentifier) {
        Firebase preferenceRef = new Firebase(Settings.getFirebaseUrl()).child("notificationPrefs").child(activityIdentifier);
        preferenceRef.removeValue();
    }

    public void removeActivity(String activityIdentifier) {
        Firebase activityToRemoveRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier);
        activityToRemoveRef.removeValue();
    }

    public void addUserToInvitationListAndSetGoing(String userIdentifier, String userNumber, String activityIdentifier) {

        Firebase invitationListSpot = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("invitationList").child(userIdentifier);

        Map<String, Object> data = new HashMap<>();
        data.put("status", InvitationStatus.GOING);
        data.put("userNumber", userNumber);

        invitationListSpot.setValue(data);
    }

    private Object getWhereCoordinatesObject(App4ItMapLocation mapLocation) {
        if(mapLocation == null) {
            return FIREBASE_DATA_NOT_SET;
        } else {
            Map<String,Double> latitudeLongitude = new HashMap<>();
            latitudeLongitude.put("latitude",mapLocation.getLatitude());
            latitudeLongitude.put("longitude",mapLocation.getLongitude());
            return latitudeLongitude;
        }
    }

    public void saveNewActivity(String title, String description, Format whenType, String whenValue, String whereValue, App4ItMapLocation mapLocation, String type, String createdByUserIdentifier, String createdByUserNumber, final FirebaseActivitySaveCallback onComplete) {

        final Firebase activityRef = new Firebase(Settings.getFirebaseUrl()).child("activities").push();

        Map<String, Object> whenConstruct = new HashMap<>();
        whenConstruct.put("format", whenType.toString());
        whenConstruct.put("value", whenValue);

        Object whereCoordinates = getWhereCoordinatesObject(mapLocation);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("createdBy", createdByUserIdentifier);
        attributes.put("createdByNumber", createdByUserNumber);
        attributes.put("createdOn", ServerValue.TIMESTAMP);
        attributes.put("description", description);
        attributes.put("title", title);
        attributes.put("type", type);
        attributes.put("when", whenConstruct);
        attributes.put("where", whereValue);
        attributes.put("whereCoordinates", whereCoordinates);

        activityRef.child("attributes").setValue(attributes, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if(firebaseError != null) onComplete.accept(firebaseError,null);
                else onComplete.accept(null,activityRef.getKey());
            }
        });
    }

    public void getUserIdentifierForNumber(String userNumber, final FirebaseStringProcessor onComplete) {

        Firebase userNumberRef = new Firebase(Settings.getFirebaseUrl()).child("index").child("numberToUserId").child(userNumber);

        userNumberRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if(snapshot == null) {
                    onComplete.process(null);
                } else if (!snapshot.exists() || snapshot.getValue() == null) {
                    onComplete.process(null);
                } else {
                    onComplete.process((String)snapshot.getValue());
                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                onComplete.process(null);
            }
        });

    }

    public void getNotificationsPreference(String activityId, String userIdentifier, String preferenceType, final FirebaseStringProcessor onComplete) {
        Firebase preferenceRef = new Firebase(Settings.getFirebaseUrl()).child("notificationPrefs").child(activityId).child(preferenceType).child(userIdentifier);

        preferenceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot == null || snapshot.getValue() == null) {
                    onComplete.process("N");
                } else {
                    onComplete.process(snapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                //well we will default to positive answer
                onComplete.process("N");
            }
        });
    }

    public void setNotificationPreference(String activityId, String userIdentifier, String preferenceType, String setTo) {
        Firebase preferenceRef = new Firebase(Settings.getFirebaseUrl()).child("notificationPrefs").child(activityId).child(preferenceType).child(userIdentifier);

        preferenceRef.setValue(setTo);
    }

    public void downloadSuggestionsForActivity(final String activityIdentifier, final String loggedInUserIdentifier, final SuggestionType type, final App4ItApplication delegate, final FirebaseSuggestionsCallback onComplete) {

        Firebase suggestionsRef;

        if(type.equals(SuggestionType.TIME)) {
            suggestionsRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("whenSuggestions");
        } else {
            suggestionsRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("whereSuggestions");
        }

        suggestionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<App4ItSuggestion> ret = parseSuggestionsFromSnapshot(snapshot,delegate);

                markSuggestionsAsSeen(ret,activityIdentifier,loggedInUserIdentifier,type);
                onComplete.suggestionsDownloaded(ret);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    public void updateActivityAttributes(String activityIdentifier, String title, String description, Format whenType, String whenValue, String whereValue, App4ItMapLocation mapLocation, String type, final FirebaseActivityUpdateCallback onComplete) {

        Firebase activityAttributesRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityIdentifier).child("attributes");

        Map<String,Object> whenConstruct = new HashMap<>();
        whenConstruct.put("format", whenType.toString());
        whenConstruct.put("value", whenValue);

        Object whereCoordinates = getWhereCoordinatesObject(mapLocation);

        Map<String,Object> attributes = new HashMap<>();
        attributes.put("description", description);
        attributes.put("title", title);
        attributes.put("type", type);
        attributes.put("when", whenConstruct);
        attributes.put("where", whereValue);
        attributes.put("whereCoordinates", whereCoordinates);

        activityAttributesRef.updateChildren(attributes, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                onComplete.accept(firebaseError);
            }
        });


    }

    private void markSuggestionsAsSeen(List<App4ItSuggestion> suggestions, String activityId, String loggedInUserIdentifier, SuggestionType suggestionType) {

        for(App4ItSuggestion suggestion : suggestions) {
            markSuggestionAsSeen(activityId,suggestion.getSuggestionId(),loggedInUserIdentifier,suggestionType);
        }

    }

    public App4ItSuggestion saveSuggestionForActivityId(String activityId, SuggestionType type, Format format, String value, App4ItUser user, App4ItMapLocation mapLocation) {

        String variablePathPart;
        if(type.equals(SuggestionType.TIME)) {
            variablePathPart = "whenSuggestions";
        } else {
            variablePathPart = "whereSuggestions";
        }

        Firebase suggestionRef = new Firebase(Settings.getFirebaseUrl()).child("activities").child(activityId).child(variablePathPart).push();

        Map<String,Object> attributes = new HashMap<>();
        attributes.put("format", format.toString());
        attributes.put("value", value);
        attributes.put("createdBy", user.getUserId());
        if(type.equals(SuggestionType.PLACE)) {
            attributes.put("whereCoordinates",getWhereCoordinatesObject(mapLocation));
        }

        Map<String,Object> userResponse = new HashMap<>();
        userResponse.put("userNumber", user.getNumber());
        userResponse.put("answer", Preference.FINE);

        Map<String,Object> responses = new HashMap<>();
        responses.put(user.getUserId(),userResponse);

        Map<String,Object> seen = new HashMap<>();
        seen.put(user.getUserId(), NewsStatus.NOTIFIED_ABOUT);

        Map<String,Object> dataToSet = new HashMap<>();
        dataToSet.put("attributes",attributes);
        dataToSet.put("responses",responses);
        dataToSet.put("seen",seen);

        suggestionRef.setValue(dataToSet);

        //now we could call this only after the data is trully saved to firebase but that creates (unnecessary) lag
        App4ItSuggestion suggestion = new App4ItSuggestion(suggestionRef.getKey());
        suggestion.setValue(value);
        suggestion.setFormat(format);
        suggestion.setMapLocation(mapLocation);

        suggestion.addResponseForUser(user,Preference.FINE);

        return suggestion;
    }

    public static String whenSnapshotToString(DataSnapshot input) {
        String format = (String)input.child("format").getValue();
        String value =  (String)input.child("value").getValue();

        if("FREETEXT".equalsIgnoreCase(format)) {
            return value;
        } else if ("DATE".equalsIgnoreCase(format)) {
            return DateUtil.printAsDate(value);
        } else {
            //must be DATE_TIME format
            return DateUtil.printAsDateTime(value);
        }
    }

    public static App4ItMapLocation whereCoordinatesSnapshotToMapLocation(DataSnapshot input) {
        if(input == null || input.getValue() == null) {
            //old code not saving whereCoordinates
            return null;
        } else if (input.getValue() instanceof String) {
            //saving whereCoordinates but value is NOT SET
            return null;
        } else if (input.getValue() instanceof Map) {
            Map coordinates = (Map)input.getValue();
            return new App4ItMapLocation(Double.valueOf(coordinates.get("latitude").toString()),Double.valueOf(coordinates.get("longitude").toString()));
        } else {
            //not sure when this could reach here
            return null;
        }

    }

    public static List<String> snapshotToListOfUserIds(DataSnapshot input, boolean includeDeleted) {
        List<String> ret = new ArrayList<>();

        for(DataSnapshot child : input.getChildren()) {
            InvitationStatus status = InvitationStatus.valueOf((String)child.child("status").getValue());

            if(includeDeleted || !status.equals(InvitationStatus.DELETED)) {
                ret.add(child.getKey());
            }
        }

        return ret;
    }


    //start profiles code
    public void updateProfileForUserId(String userId, String name, Bitmap bigImage, Bitmap smallImage, final SuccessOrFailureCallback callback) {

        final Firebase profileRefFull = new Firebase(Settings.getFirebaseUrl()).child("profiles").child(userId).child("full");
        Firebase profileRefQuick = new Firebase(Settings.getFirebaseUrl()).child("profiles").child(userId).child("quick");

        final Map<String,Object> dataToSetFull = new HashMap<>();
        dataToSetFull.put("name",name != null ? name : FIREBASE_DATA_NOT_SET);
        dataToSetFull.put("bigPicture", bigImage != null ? ImageUtil.imageToBase64StringQualityHigh(bigImage) : FIREBASE_DATA_NOT_SET);
        dataToSetFull.put("updatedOn", ServerValue.TIMESTAMP);

        Map<String,Object> dataToSetQuick = new HashMap<>();
        dataToSetQuick.put("name", name != null ? name : FIREBASE_DATA_NOT_SET);
        dataToSetQuick.put("smallPicture", smallImage != null ? ImageUtil.imageToBase64StringQualityLow(smallImage) : FIREBASE_DATA_NOT_SET);
        dataToSetQuick.put("updatedOn", ServerValue.TIMESTAMP);

        profileRefQuick.setValue(dataToSetQuick, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                profileRefFull.setValue(dataToSetFull, new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        if(firebaseError != null) {
                            callback.callback(false, firebaseError.getMessage());
                        } else {
                            callback.callback(true, null);
                        }
                    }
                });
            }
        });

    }

    public void downloadFullProfileForUserId(String userId, final FirebaseUserProfileCallback callback) {

        Firebase profileRefFull = new Firebase(Settings.getFirebaseUrl()).child("profiles").child(userId).child("full");

        profileRefFull.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.acceptUserProfile(snapshotToUserProfile(snapshot,"bigPicture"),null);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                callback.acceptUserProfile(null,firebaseError);
            }
        });

    }

    public void downloadQuickProfileForUserId(String userId, final FirebaseUserProfileCallback callback) {

        Firebase profileRefQuick = new Firebase(Settings.getFirebaseUrl()).child("profiles").child(userId).child("quick");

        profileRefQuick.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.acceptUserProfile(snapshotToUserProfile(snapshot,"smallPicture"),null);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                callback.acceptUserProfile(null,firebaseError);
            }
        });

    }

    private App4ItUserProfile snapshotToUserProfile(DataSnapshot snapshot, String pictureNode) {

        if(snapshot != null && snapshot.exists() && snapshot.getValue() != null && snapshot.getValue() instanceof Map) {

            Map<String,Object> data = (Map)snapshot.getValue();
            String name = (String)getValueConsiderNotSet(data,"name");
            String pictureString = (String)getValueConsiderNotSet(data,pictureNode);
            Bitmap picture = ImageUtil.base64StringToBitmap(pictureString);

            return new App4ItUserProfile(name,picture,picture != null);

        } else {

            return null;

        }

    }

    public void keepDownloadingChangesOnQuickProfileForUserId(String userId, final FirebaseUserProfileCallback callback) {

        Firebase profileRefQuick = new Firebase(Settings.getFirebaseUrl()).child("profiles").child(userId).child("quick");

        profileRefQuick.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.acceptUserProfile(snapshotToUserProfile(snapshot,"smallPicture"),null);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                callback.acceptUserProfile(null,firebaseError);
            }
        });

    }

    //end profiles code

    private Object getValueConsiderNotSet(Map<String,Object> data, String key) {
        Object value = data.get(key);
        if(FIREBASE_DATA_NOT_SET.equals(value)) {
            return null;
        } else {
            return value;
        }
    }
}
