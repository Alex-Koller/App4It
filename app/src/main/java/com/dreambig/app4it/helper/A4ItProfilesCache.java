package com.dreambig.app4it.helper;

import com.dreambig.app4it.entity.App4ItUserProfile;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexandr on 14/11/2015.
 */
public class A4ItProfilesCache {

    private Map<String,App4ItUserProfile> map = new HashMap<>();

    public App4ItUserProfile getProfileForUserId(String userIdentifier) {
        return map.get(userIdentifier);
    }

    public void addProfileForUserId(App4ItUserProfile userProfile, String userIdentifier) {
        map.put(userIdentifier,userProfile);
    }

    public boolean containsProfileForUserId(String userIdentifier) {
        return map.containsKey(userIdentifier);
    }

}
