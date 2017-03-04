package com.dreambig.app4it.impl;

import android.app.Activity;
import android.content.Context;

import com.dreambig.app4it.R;
import com.dreambig.app4it.api.App4ItActivityManager;
import com.dreambig.app4it.api.FirebaseSnapshotCallback;
import com.dreambig.app4it.api.NewsCenter;
import com.dreambig.app4it.api.SuccessOrFailureCallback;
import com.dreambig.app4it.entity.App4ItActivity;
import com.dreambig.app4it.entity.App4ItActivityParcel;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.firebase.client.DataSnapshot;

import java.util.List;

/**
 * Created by Alexandr on 17/03/2015.
 */
public class App4ItActivityManagerImpl implements App4ItActivityManager {

    public void deleteActivity(final Activity owningActivity, final boolean notifyInvitees, final App4ItActivityParcel existingActivity, final String loggedInUserIdentifier, final SuccessOrFailureCallback successOrFailureCallback) {
        //1) get all people invited
        final FirebaseGateway firebaseGateway = new FirebaseGateway(owningActivity);
        firebaseGateway.retrieveInvitationListForActivity(existingActivity.getActivityId(), new FirebaseSnapshotCallback() {
            @Override
            public void processSnapshot(boolean success, DataSnapshot snapshot, String errorMessage) {
                if(success) {
                    if(snapshot != null && snapshot.exists()) {
                        NewsCenter newsCenter = new NewsCenterImpl();
                        //1) get people invited
                        List<String> userIdsFromInvitationList = FirebaseGateway.snapshotToListOfUserIds(snapshot,false);

                        //2) remove from their invitedTo bucket and 3) tell them (notice everyone is told, even those not going). perhaps this could be changed
                        for(String userId : userIdsFromInvitationList) {
                            firebaseGateway.removeFromUsersInvitedToBucket(existingActivity.getActivityId(), userId);

                            if(!loggedInUserIdentifier.equals(userId) && notifyInvitees) {
                                //no need to news the logged in user himself
                                newsCenter.postNewsAboutActivityRemoved(owningActivity.getApplicationContext(),existingActivity.getActivityId(),existingActivity.getTitle(),loggedInUserIdentifier,userId);
                            }
                        }
                    }

                    //4) get it out from userCreted bucket
                    firebaseGateway.removeFromUserCreatedBucket(loggedInUserIdentifier, existingActivity.getActivityId());

                    //5) remove the notification preferences for the activity (has to come before removing the activity)
                    firebaseGateway.removeNotificationPreference(existingActivity.getActivityId());

                    //6) remove the activity itself
                    firebaseGateway.removeActivity(existingActivity.getActivityId());

                    successOrFailureCallback.callback(true,null);
                } else {
                    if(errorMessage != null) {
                        successOrFailureCallback.callback(false,errorMessage);
                    } else {
                        successOrFailureCallback.callback(false,"Failed to download invitation list for this event :-(");
                    }
                }
            }
        });

    }

}
