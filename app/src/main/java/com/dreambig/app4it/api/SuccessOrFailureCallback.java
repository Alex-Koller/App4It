package com.dreambig.app4it.api;

/**
 * Created by Alexandr on 17/03/2015.
 */
public interface SuccessOrFailureCallback {

    void callback(boolean success, String errorMessage);
}
