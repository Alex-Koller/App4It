package com.dreambig.app4it.api;

import java.util.Map;

/**
 * Created by Alexandr on 03/01/2015.
 */
public interface PhonebookCallback {

    void phoneBookContacts(boolean refreshed, Map<String,String> numberToName);

}
