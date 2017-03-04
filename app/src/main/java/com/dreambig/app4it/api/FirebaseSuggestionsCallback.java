package com.dreambig.app4it.api;

import com.dreambig.app4it.entity.App4ItSuggestion;

import java.util.List;

/**
 * Created by Alexandr on 27/12/2014.
 */
public interface FirebaseSuggestionsCallback {

    void suggestionsDownloaded(List<App4ItSuggestion> suggestions);

}
