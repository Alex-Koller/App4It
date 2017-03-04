package com.dreambig.app4it.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dreambig.app4it.enums.Format;
import com.dreambig.app4it.enums.InvitationStatus;

public class App4ItActivity {
	private String activityId;
	private String title;
	private String moreAbout;
    private String whereAsString;
    private App4ItMapLocation mapLocation;
    private String whenAsString;
   	private Format whenFormat;
	private String whenValue;
	private String type;
    private Long createdOn;
    private String createdByUserId;
    private String createdByNumber;
	private String createdByName;
	private List<App4ItInvitationItem> invitationList;
    private List<App4ItUser> usersGoing;
    private InvitationStatus loggedInUserStatus;
    private int unseenComments;
    private int unseenWhenSuggestions;
    private int unseenWhereSuggestions;
    private int unseenCommentsRealtime;
    private int unseenWhenSuggestionsRealtime;
    private int unseenWhereSuggestionsRealtime;


    public App4ItActivity(String activityId) {
        this.activityId = activityId;
    }
	
	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setMoreAbout(String moreAbout) {
		this.moreAbout = moreAbout;
	}

	public void setWhenAsString(String whenAsString) {
		this.whenAsString = whenAsString;
	}

	public void setWhenFormat(Format whenFormat) {
		this.whenFormat = whenFormat;
	}

	public void setWhenValue(String whenValue) {
		this.whenValue = whenValue;
	}

	public void setWhereAsString(String whereAsString) {
		this.whereAsString = whereAsString;
	}

    public void setMapLocation(App4ItMapLocation mapLocation) {
        this.mapLocation = mapLocation;
    }

    public void setType(String type) {
		this.type = type;
	}
	
	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public void setCreatedByUserId(String createdByUserId) {
		this.createdByUserId = createdByUserId;
	}

	public void setInvitationList(List<App4ItInvitationItem> invitationList) {
		this.invitationList = invitationList;
	}

    public void setUsersGoing(List<App4ItUser> usersGoing) {
        this.usersGoing = usersGoing;
    }


	public void setCreatedByName(String createdByName) {
		this.createdByName = createdByName;
	}
	
	public void setCreatedByNumber(String createdByNumber) {
		this.createdByNumber = createdByNumber;
	}
	
	public void setUnseenComments(int unseenComments) {
		this.unseenComments = unseenComments;
	}
	
	public int getUnseenComments() {
		return unseenComments;
	}

    public int getUnseenCommentsRealtime() {
        return unseenCommentsRealtime;
    }

    public void setUnseenCommentsRealtime(int unseenCommentsRealtime) {
        this.unseenCommentsRealtime = unseenCommentsRealtime;
    }

    public int getUnseenWhenSuggestions() {
		return unseenWhenSuggestions;
	}


	public void setUnseenWhenSuggestions(int unseenWhenSuggestions) {
		this.unseenWhenSuggestions = unseenWhenSuggestions;
	}


	public int getUnseenWhereSuggestions() {
		return unseenWhereSuggestions;
	}


	public void setUnseenWhereSuggestions(int unseenWhereSuggestions) {
		this.unseenWhereSuggestions = unseenWhereSuggestions;
	}

    public int getUnseenWhenSuggestionsRealtime() {
        return unseenWhenSuggestionsRealtime;
    }

    public void setUnseenWhenSuggestionsRealtime(int unseenWhenSuggestionsRealtime) {
        this.unseenWhenSuggestionsRealtime = unseenWhenSuggestionsRealtime;
    }

    public int getUnseenWhereSuggestionsRealtime() {
        return unseenWhereSuggestionsRealtime;
    }

    public void setUnseenWhereSuggestionsRealtime(int unseenWhereSuggestionsRealtime) {
        this.unseenWhereSuggestionsRealtime = unseenWhereSuggestionsRealtime;
    }

    public String getActivityId() {
		return activityId;
	}
		
	public String getTitle() {
		return title;
	}
	
	public String getMoreAbout() {
		return moreAbout;
	}

	public String getCreatedByName() {
		return createdByName;
	}
	
	public String getCreatedByNumber() {
		return createdByNumber;
	}

	public Format getWhenFormat() {
		return whenFormat;
	}

	public String getWhenValue() {
		return whenValue;
	}

	public String getWhenAsString() {
		return whenAsString;
	}

	public String getWhereAsString() {
		return whereAsString;
	}

    public App4ItMapLocation getMapLocation() {
        return mapLocation;
    }

    public String getType() {
		return type;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public String getCreatedByUserId() {
		return createdByUserId;
	}
	
	public List<App4ItInvitationItem> getInvitationList() {
		return invitationList;
	}

    public void addInvitationItemKeepSorted(App4ItInvitationItem invitationItem) {
        for(int i = 0;i < invitationList.size();i++) {
            App4ItInvitationItem existing = invitationList.get(i);
            if(invitationItem.compareTo(existing) <= 0) {
                invitationList.add(i,invitationItem);
                return;
            }
        }

        //if the new one is not lower than any existing
        invitationList.add(invitationItem);
    }

    public void ensureInvitationItemsAreSorted() {
        Collections.sort(invitationList);
    }

    public List<App4ItUser> getUsersGoing() {
        return usersGoing;
    }

    public InvitationStatus getLoggedInUserStatus() {
        return loggedInUserStatus;
    }

    public void setLoggedInUserStatus(InvitationStatus loggedInUserStatus) {
        this.loggedInUserStatus = loggedInUserStatus;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((activityId == null) ? 0 : activityId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		App4ItActivity other = (App4ItActivity) obj;
		if (activityId == null) {
			if (other.activityId != null)
				return false;
		} else if (!activityId.equals(other.activityId))
			return false;
		return true;
	}
	
	
}
