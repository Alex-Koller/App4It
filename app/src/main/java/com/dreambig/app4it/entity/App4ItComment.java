package com.dreambig.app4it.entity;

public class App4ItComment {

    private String identifier;
	private String createdBy;
	private String createdByNumber;
    private String createdByName;
	private long createdOn;
	private String text;

    public App4ItComment(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getCreatedBy() {
		return createdBy;
	}
	public long getCreatedOn() {
		return createdOn;
	}
	public String getText() {
		return text;
	}
	public String getCreatedByNumber() {
		return createdByNumber;
	}

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedByNumber(String createdByNumber) {
        this.createdByNumber = createdByNumber;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public void setCreatedOn(long createdOn) {
        this.createdOn = createdOn;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        App4ItComment that = (App4ItComment) o;

        if (!identifier.equals(that.identifier)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }
}
