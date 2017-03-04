package com.dreambig.app4it.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Alexandr on 28/09/2015.
 */
public class App4ItAddressAndMapLocation implements Parcelable {

    private String address;
    private App4ItMapLocation mapLocation;

    public App4ItAddressAndMapLocation() {
    }

    public App4ItAddressAndMapLocation(String address, App4ItMapLocation mapLocation) {
        this.address = address;
        this.mapLocation = mapLocation;
    }

    public String getAddress() {
        return address;
    }

    public App4ItMapLocation getMapLocation() {
        return mapLocation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(address);
        parcel.writeParcelable(mapLocation,0);
    }

    public static final Creator<App4ItAddressAndMapLocation> CREATOR = new Creator<App4ItAddressAndMapLocation>() {
        public App4ItAddressAndMapLocation createFromParcel(Parcel source) {
            App4ItAddressAndMapLocation ret = new App4ItAddressAndMapLocation();

            ret.address = source.readString();
            ret.mapLocation = source.readParcelable(App4ItMapLocation.class.getClassLoader());

            return ret;
        }

        @Override
        public App4ItAddressAndMapLocation[] newArray(int size) {
            return new App4ItAddressAndMapLocation[size];
        }
    };
}
