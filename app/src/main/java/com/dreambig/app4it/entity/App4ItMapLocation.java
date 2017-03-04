package com.dreambig.app4it.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Alexandr on 28/09/2015.
 */
public class App4ItMapLocation implements Parcelable {

    private double latitude;
    private double longitude;

    public App4ItMapLocation() {
    }

    public App4ItMapLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public LatLng toLatLng() {
        return new LatLng(getLatitude(),getLongitude());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
    }

    public static final Parcelable.Creator<App4ItMapLocation> CREATOR = new Creator<App4ItMapLocation>() {
        public App4ItMapLocation createFromParcel(Parcel source) {
            App4ItMapLocation ret = new App4ItMapLocation();

            ret.latitude = source.readDouble();
            ret.longitude = source.readDouble();

            return ret;
        }

        @Override
        public App4ItMapLocation[] newArray(int size) {
            return new App4ItMapLocation[size];
        }
    };
}
