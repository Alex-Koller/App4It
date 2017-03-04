package com.dreambig.app4it.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.dreambig.app4it.enums.Preference;
import com.dreambig.app4it.enums.Format;

public class App4ItSuggestion {
	
	private String suggestionId;
	private Format format;
	private String value;
    private App4ItMapLocation mapLocation;

    private Set<App4ItUser> responders = new HashSet<>();
    private Map<String,Preference> responses = new HashMap<>();

    public App4ItSuggestion(String suggestionId) {
        this.suggestionId = suggestionId;
    }

    public String getSuggestionId() {
		return suggestionId;
	}

	public Format getFormat() {
		return format;
	}

	public String getValue() {
		return value;
	}

    public void setFormat(Format format) {
        this.format = format;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public App4ItMapLocation getMapLocation() {
        return mapLocation;
    }

    public void setMapLocation(App4ItMapLocation mapLocation) {
        this.mapLocation = mapLocation;
    }

    public Set<App4ItUser> getResponders() {
        return responders;
    }

    public Map<String, Preference> getResponses() {
        return responses;
    }

    public Preference responseForUserId(String userIdentifier) {
        return responses.get(userIdentifier);
    }

    public void addResponseForUser(App4ItUser user, Preference response) {
        responders.add(user);
        responses.put(user.getUserId(),response);
    }

    public void changeResponseForUserIdentifier(String userIdentifier, Preference newResponse) {
        responses.put(userIdentifier, newResponse);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        App4ItSuggestion that = (App4ItSuggestion) o;

        if (!suggestionId.equals(that.suggestionId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return suggestionId.hashCode();
    }
}
