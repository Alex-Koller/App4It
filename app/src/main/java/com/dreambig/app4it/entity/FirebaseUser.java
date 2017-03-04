package com.dreambig.app4it.entity;

/**
 * Created by Alexandr on 25/12/2014.
 */
public class FirebaseUser {

    private String userId;
    private String userNumber;

    public FirebaseUser(String userId, String userNumber) {
        this.userId = userId;
        this.userNumber = userNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserNumber() {
        return userNumber;
    }

    public void setUserNumber(String userNumber) {
        this.userNumber = userNumber;
    }
}
