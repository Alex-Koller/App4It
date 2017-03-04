package com.dreambig.app4it.entity;

import com.dreambig.app4it.enums.NewsStatus;
import com.dreambig.app4it.enums.NewsType;

/**
 * Created by Alexandr on 15/01/2015.
 */
public class App4ItNewsItem {

    private String createdByUserId;
    private String createdForUserId;
    private NewsType newsType;
    private String subjectId;
    private String subjectTitle;
    private String additionalValue;
    private NewsStatus status;

    public App4ItNewsItem() {
    }

    public App4ItNewsItem(String createdByUserId, String createdForUserId, NewsType newsType, String subjectId, String subjectTitle, String additionalValue, NewsStatus status) {
        this.createdByUserId = createdByUserId;
        this.createdForUserId = createdForUserId;
        this.newsType = newsType;
        this.subjectId = subjectId;
        this.subjectTitle = subjectTitle;
        this.additionalValue = additionalValue;
        this.status = status;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getCreatedForUserId() {
        return createdForUserId;
    }

    public void setCreatedForUserId(String createdForUserId) {
        this.createdForUserId = createdForUserId;
    }

    public NewsType getNewsType() {
        return newsType;
    }

    public void setNewsType(NewsType newsType) {
        this.newsType = newsType;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectTitle() {
        return subjectTitle;
    }

    public void setSubjectTitle(String subjectTitle) {
        this.subjectTitle = subjectTitle;
    }

    public String getAdditionalValue() {
        return additionalValue;
    }

    public void setAdditionalValue(String additionalValue) {
        this.additionalValue = additionalValue;
    }

    public NewsStatus getStatus() {
        return status;
    }

    public void setStatus(NewsStatus status) {
        this.status = status;
    }
}
