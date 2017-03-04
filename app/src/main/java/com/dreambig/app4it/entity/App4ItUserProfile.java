package com.dreambig.app4it.entity;

import android.graphics.Bitmap;

/**
 * Created by Alexandr on 11/11/2015.
 */
public class App4ItUserProfile {

    private String name;
    private Bitmap picture;
    private boolean isPictureReal;

    public App4ItUserProfile(String name, Bitmap picture, boolean isPictureReal) {
        this.name = name;
        this.picture = picture;
        this.isPictureReal = isPictureReal;
    }

    public String getName() {
        return name;
    }

    public Bitmap getPicture() {
        return picture;
    }

    public boolean isPictureReal() {
        return isPictureReal;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }

    public void setPictureReal(boolean isPictureReal) {
        this.isPictureReal = isPictureReal;
    }
}
