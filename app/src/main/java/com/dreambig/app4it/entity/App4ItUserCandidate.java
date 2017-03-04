package com.dreambig.app4it.entity;

/**
 * Created by Alexandr on 07/08/2015.
 */
public class App4ItUserCandidate {

    private String name;
    private String number;
    private boolean invited;

    public App4ItUserCandidate(String name, String number) {
        this.name = name;
        this.number = number;
    }

    public boolean isInvited() {
        return invited;
    }

    public void setInvited(boolean invited) {
        this.invited = invited;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }
}
