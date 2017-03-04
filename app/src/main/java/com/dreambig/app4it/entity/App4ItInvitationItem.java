package com.dreambig.app4it.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.dreambig.app4it.enums.InvitationStatus;

/**
 * Created by Alexandr on 26/12/2014.
 */
public class App4ItInvitationItem implements Parcelable, Comparable<App4ItInvitationItem> {

    private App4ItUser user;
    private InvitationStatus status;

    public App4ItInvitationItem() {
    }

    public App4ItInvitationItem(App4ItUser user, InvitationStatus status) {
        this.user = user;
        this.status = status;
    }

    @Override
    public int compareTo(App4ItInvitationItem another) {
        if(status.equals(another.status)) {
            return 0;
        } else if (status.equals(InvitationStatus.GOING)) {
            return -1;
        } else if (another.status.equals(InvitationStatus.GOING)) {
            return 1;
        } else if (status.equals(InvitationStatus.INVITED)) {
            return -1;
        } else if (another.status.equals(InvitationStatus.INVITED)) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(user, flags);
        parcel.writeString(status.toString());
    }

    public static final Parcelable.Creator<App4ItInvitationItem> CREATOR = new Creator<App4ItInvitationItem>() {
        public App4ItInvitationItem createFromParcel(Parcel source) {
            App4ItUser user = source.readParcelable(App4ItUser.class.getClassLoader());
            InvitationStatus status = InvitationStatus.valueOf(source.readString());

            return new App4ItInvitationItem(user,status);
        }

        @Override
        public App4ItInvitationItem[] newArray(int size) {
            return new App4ItInvitationItem[size];
        }
    };

    public App4ItUser getUser() {
        return user;
    }

    public void setUser(App4ItUser user) {
        this.user = user;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }
}
