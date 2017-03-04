package com.dreambig.app4it.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.dreambig.app4it.enums.Format;

public class App4ItActivityParcel implements Parcelable {
	private String activityId;
	private String title;
	private String moreAbout;
    private String whereAsString;
    private App4ItMapLocation mapLocation;
   	private Format whenFormat;
	private String whenValue;
	private String type;
    private Long createdOn;
    private String createdByUserId;
    private String createdByNumber;

    public App4ItActivityParcel() {

    }

    public App4ItActivityParcel(App4ItActivity activity) {
        this.activityId = activity.getActivityId();
        this.title = activity.getTitle();
        this.moreAbout = activity.getMoreAbout();
        this.whereAsString = activity.getWhereAsString();
        this.mapLocation = activity.getMapLocation();
        this.whenFormat = activity.getWhenFormat();
        this.whenValue = activity.getWhenValue();
        this.type = activity.getType();
        this.createdOn = activity.getCreatedOn();
        this.createdByUserId = activity.getCreatedByUserId();
        this.createdByNumber = activity.getCreatedByNumber();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(activityId);
        parcel.writeString(title);
        parcel.writeString(moreAbout);
        parcel.writeString(whereAsString);
        parcel.writeParcelable(mapLocation,0);
        parcel.writeString(whenFormat.toString());
        parcel.writeString(whenValue);
        parcel.writeString(type);
        parcel.writeLong(createdOn);
        parcel.writeString(createdByUserId);
        parcel.writeString(createdByNumber);
    }

    public static final Parcelable.Creator<App4ItActivityParcel> CREATOR = new Creator<App4ItActivityParcel>() {
        public App4ItActivityParcel createFromParcel(Parcel source) {
            App4ItActivityParcel ret = new App4ItActivityParcel();

            ret.activityId = source.readString();
            ret.title = source.readString();
            ret.moreAbout = source.readString();
            ret.whereAsString = source.readString();
            ret.mapLocation = source.readParcelable(App4ItMapLocation.class.getClassLoader());
            ret.whenFormat = Format.valueOf(source.readString());
            ret.whenValue = source.readString();
            ret.type = source.readString();
            ret.createdOn = source.readLong();
            ret.createdByUserId = source.readString();
            ret.createdByNumber = source.readString();

            return ret;
        }

        @Override
        public App4ItActivityParcel[] newArray(int size) {
            return new App4ItActivityParcel[size];
        }
    };
	
	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setMoreAbout(String moreAbout) {
		this.moreAbout = moreAbout;
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
	
	public void setCreatedByNumber(String createdByNumber) {
		this.createdByNumber = createdByNumber;
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
	
	public String getCreatedByNumber() {
		return createdByNumber;
	}

	public Format getWhenFormat() {
		return whenFormat;
	}

	public String getWhenValue() {
		return whenValue;
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
		App4ItActivityParcel other = (App4ItActivityParcel) obj;
		if (activityId == null) {
			if (other.activityId != null)
				return false;
		} else if (!activityId.equals(other.activityId))
			return false;
		return true;
	}
	
	
}
