package com.dreambig.app4it.api;

import java.util.List;

import android.content.Context;

import com.dreambig.app4it.enums.NewsType;
import com.dreambig.app4it.enums.SuggestionType;

public interface NewsCenter {

    //refactor start
    void postNewsAboutEditedActivity(Context context, final String activityId, final String loggedInUserIdentifier, final List<NewsType> whatChanged, final String oldActivityTitle, final String newActivityTitle);
    void postNewsAboutBeingInvitedToActivity(Context context, String activityId, String activityTitle, String loggedInUserIdentifier, String invitedUserIdentifier);
    void postNewsAboutNewCommentOnActivity(Context context, final String activityId, final String activityTitle, final String loggedInUserIdentifier);
    void postNewsAboutSuggestion(Context context, final String activityId, final String activityTitle, final String loggedInUserIdentifier, final SuggestionType suggestionType);
    void postNewsAboutActivityRemoved(Context context, String activityId, String activityTitle, String loggedInUserIdentifier, String forUserIdentifier);
    //refactor end

}
