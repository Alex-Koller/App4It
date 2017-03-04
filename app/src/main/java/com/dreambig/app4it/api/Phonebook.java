package com.dreambig.app4it.api;

import java.util.List;
import java.util.Map;

public interface Phonebook {
	Map<String,List<String>> getNameAndFullPhoneNumbers(String internationalCode, String homeUserNumber) throws Exception;
	Map<String,String> getFullPhoneNumberToName(Map<String,List<String>> nameToFullNumbers) throws Exception;
    void getApp4ItUsers(Map<String,String> phoneBook, String userIdentifierToSkip, boolean careAboutNonUsers, FirebaseApp4ItUsers onComplete);
}
