package com.dreambig.app4it.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alexandr on 25/12/2014.
 */
public class RegistrationHelper {

    private final static String prefixPattern = "^[A-Z]+\\s\\((\\+[0-9]+)\\)";

    public static boolean validateRegistrationInputPrefixPart(String in) {
        if(in == null) {
            return false;
        }

        Pattern pattern = Pattern.compile(prefixPattern);
        Matcher m = pattern.matcher(in);
        return m.find();
    }

    public static boolean validateRegistrationInputMainPart(String in) {
        in = in.replaceAll(" ", "");
        Pattern pattern = Pattern.compile("^\\+?[0-9]{5,}?$");
        Matcher m = pattern.matcher(in);

        return m.find();
    }

    public static String createRegistrationEmail(String fullPhoneNumber) {
        return fullPhoneNumber + "@dreambig.com";
    }

    public static String fullPhoneNumberFromEmail(String email) {
        int endIndex = email.indexOf("@dreambig.com");
        return email.substring(0,endIndex);
    }

    public static String insertedPhonePrefixToUsable(String selectedPrefix) {
        Pattern pattern = Pattern.compile(prefixPattern);
        Matcher m = pattern.matcher(selectedPrefix);

        if(m.find()) {
            return m.group(1).replace("+","00");
        } else {
            throw new FailedParsingSelectedPrefix(selectedPrefix);
        }
    }

    public static String insertedPhoneMainPartToUsable(String mainPart) {
        mainPart = mainPart.replaceAll(" ","");
        if(mainPart.startsWith("0")) {
            return mainPart.substring(1,mainPart.length());
        } else {
            return mainPart;
        }
    }

    public static class FailedParsingSelectedPrefix extends RuntimeException {
        public FailedParsingSelectedPrefix(String message) {
            super(message);
        }
    }
}
