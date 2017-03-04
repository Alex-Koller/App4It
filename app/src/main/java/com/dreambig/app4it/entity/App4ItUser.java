package com.dreambig.app4it.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class App4ItUser implements Parcelable {
	private String name;
	private String number;
	private String userId;
	
	public App4ItUser() {
		
	}

    public App4ItUser(String userId) {
        this.userId = userId;
    }

    public App4ItUser(String name, String number, String userId) {
		this.name = name;
		this.number = number;
		this.userId = userId;
	}

    public void setName(String name) {
        this.name = name;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
		return name;
	}

	public String getNumber() {
		return number;
	}

	public String getUserId() {
		return userId;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(name);
		parcel.writeString(number);
		parcel.writeString(userId);		
	}	
	
	public static final Parcelable.Creator<App4ItUser> CREATOR = new Creator<App4ItUser>() { 
    	public App4ItUser createFromParcel(Parcel source) { 
    		App4ItUser ret = new App4ItUser(); 
    		ret.name = source.readString(); 
    		ret.number = source.readString(); 
    		ret.userId = source.readString();
    		return ret; 
    	}

		@Override
		public App4ItUser[] newArray(int size) {
			return new App4ItUser[size];
		}
    };

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		App4ItUser other = (App4ItUser) obj;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	} 	
	
	
}
