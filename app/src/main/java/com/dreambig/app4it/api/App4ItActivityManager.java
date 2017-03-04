package com.dreambig.app4it.api;

import android.app.Activity;

import com.dreambig.app4it.entity.App4ItActivityParcel;

/**
 * Created by Alexandr on 17/03/2015.
 */
public interface App4ItActivityManager {
    void deleteActivity(final Activity owningActivity, final boolean notifyInvitees, final App4ItActivityParcel existingActivity, final String loggedInUserIdentifier, SuccessOrFailureCallback successOrFailureCallback);
}
