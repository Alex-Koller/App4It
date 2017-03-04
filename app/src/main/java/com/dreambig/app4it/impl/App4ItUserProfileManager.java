package com.dreambig.app4it.impl;


import android.content.Context;
import android.graphics.Bitmap;

import com.dreambig.app4it.api.FirebaseUserProfileCallback;
import com.dreambig.app4it.entity.App4ItActivity;
import com.dreambig.app4it.entity.App4ItComment;
import com.dreambig.app4it.entity.App4ItInvitationItem;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.helper.A4ItProfilesCache;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.firebase.client.FirebaseError;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Alexandr on 12/11/2015.
 */
public class App4ItUserProfileManager {

    private static int defaultProfilePictureCounter = 0;
    private static final A4ItProfilesCache profilesCache = new A4ItProfilesCache();

    public interface AddingToCacheCallback {
        void completed();
    }

    public static void useThisUserProfileForUserId(Context context, String userId, App4ItUserProfile userProfile) {

        modifyProfileWhereNecessary(context, userProfile, null, true); //because it's home user the user can be nil
        profilesCache.addProfileForUserId(userProfile, userId);

    }

    public static void addToCacheUsersInComments(Context context, List<App4ItComment> comments, String loggedInUserId, AddingToCacheCallback callback) {

        Set<App4ItUser> distinctiveUsers = getDistinctiveUsersFromComments(comments);
        processDistinctiveUsers(context, distinctiveUsers, loggedInUserId, callback);

    }


    public static void addToCacheUsers(Context context, List<App4ItUser> users, String loggedInUserId, AddingToCacheCallback callback) {
        Set<App4ItUser> distinctiveUsers = new HashSet<>(users);
        processDistinctiveUsers(context,distinctiveUsers,loggedInUserId,callback);
    }

    public static void addToCacheUsersInActivities(Context context, List<App4ItActivity> activities, String loggedInUserId, AddingToCacheCallback callback) {

        Set<App4ItUser> distinctiveUsers = getDistinctiveUsersFromActivities(activities);
        processDistinctiveUsers(context, distinctiveUsers, loggedInUserId, callback);
    }

    public static App4ItUserProfile getUserProfileNoCreate(String userId) {
        return profilesCache.getProfileForUserId(userId);
    }

    public static App4ItUserProfile getUserProfile(Context context, App4ItUser user) {

        App4ItUserProfile userProfile = profilesCache.getProfileForUserId(user.getUserId());

        if(userProfile != null) {
            return userProfile;
        } else {
            //this should not happen as the user profile should be in the cache. "You" won't happen here. good for seeing when it happened.
            String textOnProfile = (user.getName() != null ? user.getName() : user.getNumber());
            Bitmap profileImage = getQuickDummyProfilePicture(context);
            return new App4ItUserProfile(textOnProfile,profileImage,false);
        }

    }

    //guts
    private static void processDistinctiveUsers(final Context context, Set<App4ItUser> distinctiveUsers, final String loggedInUserId, final AddingToCacheCallback callback) {

        if(distinctiveUsers.size() == 0) {
            //not much to do
            callback.completed();
        }

        final Map<String,Integer> downloadsTracker = getDownloadsTrackingMap(distinctiveUsers);

        FirebaseGateway firebaseGateway = new FirebaseGateway(context);

        for(final App4ItUser user : distinctiveUsers) {

            if(profilesCache.containsProfileForUserId(user.getUserId())) {
                //we already have it
                downloadsTracker.put(user.getUserId(),1);
                runIfDownloadsTrackerComplete(downloadsTracker, callback);
            } else {
                firebaseGateway.keepDownloadingChangesOnQuickProfileForUserId(user.getUserId(), new FirebaseUserProfileCallback() {
                    @Override
                    public void acceptUserProfile(App4ItUserProfile userProfile, FirebaseError error) {
                        if(userProfile == null) {
                            userProfile = new App4ItUserProfile(null,null,false);
                        }

                        modifyProfileWhereNecessary(context, userProfile, user, loggedInUserId.equals(user.getUserId()));
                        profilesCache.addProfileForUserId(userProfile, user.getUserId());

                        int currentValue = downloadsTracker.get(user.getUserId());
                        downloadsTracker.put(user.getUserId(),1);

                        if(currentValue == 0) {
                            //means it could have been the missing profile to download
                            runIfDownloadsTrackerComplete(downloadsTracker, callback);
                        }
                    }
                });

            }
        }

    }

    //helpers
    private static Set<App4ItUser> getDistinctiveUsersFromComments(List<App4ItComment> comments) {
        Set<App4ItUser> ret = new HashSet<>();

        if(comments != null) {
            for(App4ItComment comment : comments) {
                ret.add(new App4ItUser(comment.getCreatedByName(),comment.getCreatedByNumber(),comment.getCreatedBy()));
            }
        }

        return ret;
    }

    private static Set<App4ItUser> getDistinctiveUsersFromActivities(List<App4ItActivity> activities) {

        Set<App4ItUser> ret = new HashSet<>();

        for(App4ItActivity activity : activities) {
            if(activity.getInvitationList() != null) {
                for(App4ItInvitationItem invitationItem : activity.getInvitationList()) {
                    ret.add(invitationItem.getUser());
                }
            }
        }

        return ret;
    }

    private static Map<String,Integer> getDownloadsTrackingMap(Set<App4ItUser> distinctiveUsers) {

        Map<String,Integer> ret = new HashMap<>();

        for(App4ItUser user : distinctiveUsers) {
            ret.put(user.getUserId(),0);
        }

        return ret;
    }

    private static void runIfDownloadsTrackerComplete(Map<String,Integer> downloadsTracker, AddingToCacheCallback callback) {

        if(!downloadsTracker.values().contains(0)) {
            callback.completed();
        }

    }

    private static void modifyProfileWhereNecessary(Context context, App4ItUserProfile userProfile, App4ItUser user, boolean isHomeUser) {

        //make sure name is set right
        if(isHomeUser) {
            userProfile.setName("You");
        } else {

            if(user.getName() != null && !user.getName().trim().equals("")) {
                userProfile.setName(user.getName());
            } else {

                if(userProfile.getName() == null || userProfile.getName().trim().equals("")) {
                    //right, name is not filled, give it the phone number
                    userProfile.setName(user.getNumber());
                }

            }

        }

        //set the image right
        if(userProfile.getPicture() == null) {
            userProfile.setPicture(getQuickDummyProfilePicture(context));
        }

    }

    private static Bitmap getQuickDummyProfilePicture(Context context) {

        defaultProfilePictureCounter++;

        if(defaultProfilePictureCounter % 3 == 0) {
            return UIHelper.getQuickDummyProfilePictureOne(context);
        } else if (defaultProfilePictureCounter % 3 == 1) {
            return UIHelper.getQuickDummyProfilePictureTwo(context);
        } else {
            return UIHelper.getQuickDummyProfilePictureThree(context);
        }

    }

}
