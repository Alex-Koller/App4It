package com.dreambig.app4it.entity;

import com.dreambig.app4it.enums.InvitationStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexandr on 31/01/2015.
 */
public class FilterSettings {

    private boolean showUndisclosed;
    private boolean showCatchUp;
    private boolean showCultural;
    private boolean showNightOut;
    private boolean showSport;
    private boolean showFoodAndDrink;
    private boolean showGoing;
    private boolean showNotGoing;
    private boolean showUnanswered;
    private boolean showCreatedByMe;

    public FilterSettings(boolean showUndisclosed, boolean showCatchUp, boolean showCultural, boolean showNightOut, boolean showSport, boolean showFoodAndDrink, boolean showGoing, boolean showNotGoing, boolean showUnanswered, boolean showCreatedByMe) {
        this.showUndisclosed = showUndisclosed;
        this.showCatchUp = showCatchUp;
        this.showCultural = showCultural;
        this.showNightOut = showNightOut;
        this.showSport = showSport;
        this.showFoodAndDrink = showFoodAndDrink;
        this.showGoing = showGoing;
        this.showNotGoing = showNotGoing;
        this.showUnanswered = showUnanswered;
        this.showCreatedByMe = showCreatedByMe;
    }

    public boolean showingEverything() {
        return showUndisclosed && showCatchUp && showCultural && showNightOut && showSport && showFoodAndDrink && showGoing && showNotGoing && showUnanswered && showCreatedByMe;
    }

    public boolean showUndisclosed() {
        return showUndisclosed;
    }

    public void setShowUndisclosed(boolean showUndisclosed) {
        this.showUndisclosed = showUndisclosed;
    }

    public boolean showCatchUp() {
        return showCatchUp;
    }

    public void setShowCatchUp(boolean showCatchUp) {
        this.showCatchUp = showCatchUp;
    }

    public boolean showCultural() {
        return showCultural;
    }

    public void setShowCultural(boolean showCultural) {
        this.showCultural = showCultural;
    }

    public boolean showNightOut() {
        return showNightOut;
    }

    public void setShowNightOut(boolean showNightOut) {
        this.showNightOut = showNightOut;
    }

    public boolean showSport() {
        return showSport;
    }

    public void setShowSport(boolean showSport) {
        this.showSport = showSport;
    }

    public boolean showFoodAndDrink() {
        return showFoodAndDrink;
    }

    public void setShowFoodAndDrink(boolean showFoodAndDrink) {
        this.showFoodAndDrink = showFoodAndDrink;
    }

    public boolean showGoing() {
        return showGoing;
    }

    public void setShowGoing(boolean showGoing) {
        this.showGoing = showGoing;
    }

    public boolean showNotGoing() {
        return showNotGoing;
    }

    public void setShowNotGoing(boolean showNotGoing) {
        this.showNotGoing = showNotGoing;
    }

    public boolean showUnanswered() {
        return showUnanswered;
    }

    public void setShowUnanswered(boolean showUnanswered) {
        this.showUnanswered = showUnanswered;
    }

    public boolean showCreatedByMe() {
        return showCreatedByMe;
    }

    public void setShowCreatedByMe(boolean showCreatedByMe) {
        this.showCreatedByMe = showCreatedByMe;
    }

    private boolean doesTheActivityPassTheFilter(String homeUserId, App4ItActivity activity) {
        boolean ret = true;

        //yes, the strings should be a constant
        if (activity.getType().equalsIgnoreCase("catch up")) {
            ret = showCatchUp();
        } else if(activity.getType().equalsIgnoreCase("cultural")) {
            ret = showCultural();
        } else if (activity.getType().equalsIgnoreCase("night out")) {
            ret = showNightOut();
        } else if(activity.getType().equalsIgnoreCase("sport")) {
            ret = showSport();
        } else if (activity.getType().equalsIgnoreCase("food and drink")) {
            ret = showFoodAndDrink();
        } else if (activity.getType().equalsIgnoreCase("undisclosed")) {
            ret = showUndisclosed();
        }

        if(ret) {

            if(activity.getCreatedByUserId().equals(homeUserId) && showCreatedByMe()) {
                ret = true;
            } else {
                if(activity.getLoggedInUserStatus().equals(InvitationStatus.GOING)) {
                    ret = showGoing();
                } else if (activity.getLoggedInUserStatus().equals(InvitationStatus.NOT_GOING)) {
                    ret = showNotGoing();
                } else if (activity.getLoggedInUserStatus().equals(InvitationStatus.INVITED)) {
                    ret = showUnanswered();
                }
            }
        }

        return ret;
    }

    public List<App4ItActivity> filterActivities(String homeUserId, final List<App4ItActivity> activities) {
        List<App4ItActivity> ret = new ArrayList<>();

        for(App4ItActivity app4ItActivity : activities) {
            if(doesTheActivityPassTheFilter(homeUserId,app4ItActivity)) {
                ret.add(app4ItActivity);
            }
        }

        return ret;
    }
}
