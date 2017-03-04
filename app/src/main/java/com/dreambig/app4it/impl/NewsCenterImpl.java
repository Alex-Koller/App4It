package com.dreambig.app4it.impl;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.dreambig.app4it.api.FirebaseActivityInviteeCallback;
import com.dreambig.app4it.api.FirebaseSnapshotCallback;
import com.dreambig.app4it.api.NewsCenter;
import com.dreambig.app4it.entity.App4ItNewsItem;
import com.dreambig.app4it.enums.InvitationStatus;
import com.dreambig.app4it.enums.NewsStatus;
import com.dreambig.app4it.enums.NewsType;
import com.dreambig.app4it.enums.SuggestionType;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.firebase.client.DataSnapshot;

public class NewsCenterImpl implements NewsCenter {

    public void postNewsAboutEditedActivity(Context context, final String activityId, final String loggedInUserIdentifier, final List<NewsType> whatChanged, final String oldActivityTitle, final String newActivityTitle) {

        executeForEachProvidedStatusOnActivityId(context,Arrays.asList(InvitationStatus.GOING, InvitationStatus.INVITED),activityId,loggedInUserIdentifier,new FirebaseActivityInviteeCallback() {
            @Override
            public void processUser(FirebaseGateway firebaseGateway, String userIdentifier) {

                for(NewsType changeItem : whatChanged) {
                    App4ItNewsItem newsItem;

                    if(changeItem.equals(NewsType.ACTIVITY_TITLE_EDITED)) {
                        newsItem = new App4ItNewsItem(loggedInUserIdentifier,userIdentifier,changeItem,activityId,oldActivityTitle,newActivityTitle,NewsStatus.NEW);
                    } else {
                        newsItem = new App4ItNewsItem(loggedInUserIdentifier,userIdentifier,changeItem,activityId,oldActivityTitle,"",NewsStatus.NEW);
                    }

                    firebaseGateway.storeNews(newsItem);
                }
            }
        });




    }

    @Override
    public void postNewsAboutBeingInvitedToActivity(Context context, String activityId, String activityTitle, String loggedInUserIdentifier, String invitedUserIdentifier) {

        FirebaseGateway firebaseGateway = new FirebaseGateway(context);

        App4ItNewsItem newsItem = new App4ItNewsItem(loggedInUserIdentifier,invitedUserIdentifier,NewsType.INVITED_TO_ACTIVITY,activityId,activityTitle,"",NewsStatus.NEW);

        firebaseGateway.storeNews(newsItem);
    }

    @Override
    public void postNewsAboutNewCommentOnActivity(Context context, final String activityId, final String activityTitle, final String loggedInUserIdentifier) {

        executeForEachProvidedStatusOnActivityId(context,Arrays.asList(InvitationStatus.GOING, InvitationStatus.INVITED, InvitationStatus.NOT_GOING),activityId,loggedInUserIdentifier,new FirebaseActivityInviteeCallback() {
            @Override
            public void processUser(FirebaseGateway firebaseGateway, String userIdentifier) {

                App4ItNewsItem newsItem = new App4ItNewsItem(loggedInUserIdentifier,userIdentifier,NewsType.NEW_COMMENT,activityId,activityTitle,"",NewsStatus.NEW);

                firebaseGateway.storeNews(newsItem);
            }
        });

    }

    @Override
    public void postNewsAboutSuggestion(Context context, final String activityId, final String activityTitle, final String loggedInUserIdentifier, final SuggestionType suggestionType) {

        executeForEachProvidedStatusOnActivityId(context, Arrays.asList(InvitationStatus.GOING, InvitationStatus.INVITED), activityId, loggedInUserIdentifier, new FirebaseActivityInviteeCallback() {
            @Override
            public void processUser(FirebaseGateway firebaseGateway, String userIdentifier) {

                NewsType newsType = suggestionType.equals(SuggestionType.TIME) ? NewsType.NEW_WHEN_SUGGESTION : NewsType.NEW_WHERE_SUGGESTION;

                App4ItNewsItem newsItem = new App4ItNewsItem(loggedInUserIdentifier, userIdentifier, newsType, activityId, activityTitle, "", NewsStatus.NEW);

                firebaseGateway.storeNews(newsItem);

            }
        });

    }

    public void postNewsAboutActivityRemoved(Context context, String activityId, String activityTitle, String loggedInUserIdentifier, String forUserIdentifier)
    {
        FirebaseGateway firebaseGateway = new FirebaseGateway(context);
        App4ItNewsItem newsItem = new App4ItNewsItem(loggedInUserIdentifier, forUserIdentifier, NewsType.ACTIVITY_CANCELLED, activityId, activityTitle, "", NewsStatus.NEW);


        firebaseGateway.storeNews(newsItem);
    }

    private void executeForEachProvidedStatusOnActivityId(Context context,final List<InvitationStatus> statusList, String activityId, final String userIdentifierToSkip, final FirebaseActivityInviteeCallback firebaseActivityInviteeCallback) {

        final FirebaseGateway firebaseGateway = new FirebaseGateway(context);

        firebaseGateway.retrieveInvitationListForActivity(activityId, new FirebaseSnapshotCallback() {
            @Override
            public void processSnapshot(boolean success, DataSnapshot snapshot, String errorMessage) {
                if(!success) {
                    //L og.e("NewsCenterImpl","Error occurred retrieving invitation list " + errorMessage);
                } else {

                    if(snapshot != null) {
                        //loop through the invitation list
                        for(DataSnapshot invitationSnapshot : snapshot.getChildren()) {

                            //the name of the invitation snapshot is the invited user id
                            String invitedUserIdentifier = invitationSnapshot.getKey();

                            if(userIdentifierToSkip != null && invitedUserIdentifier.equals(userIdentifierToSkip)) {
                                continue;
                            }

                            InvitationStatus invitationStatus = InvitationStatus.valueOf((String) invitationSnapshot.child("status").getValue());

                            if(statusList.contains(invitationStatus)) {
                                firebaseActivityInviteeCallback.processUser(firebaseGateway, invitedUserIdentifier);
                            }
                        }
                    }

                }
            }
        });

    }

}
