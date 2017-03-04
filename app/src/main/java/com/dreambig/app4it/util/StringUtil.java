package com.dreambig.app4it.util;

import java.util.Collection;

/**
 * Created by Alexandr on 28/02/2015.
 */
public class StringUtil {

    public static int compareStringsPhoneNumbersLast(String a, String b) {
        if(a.startsWith("00") && !b.startsWith("00")) {
            return 1;
        } else if (!a.startsWith("00") && b.startsWith("00")) {
            return -1;
        } else {
            return a.compareTo(b);
        }
    }

    public static String toListOfValuesCommaSeparated(Collection<String> in) {
        StringBuilder sb = new StringBuilder("");
        for(Object o : in) {
            sb.append(o).append(", ");
        }

        if(sb.toString().endsWith(", ")) return sb.substring(0,sb.lastIndexOf(","));
        return sb.toString();
    }

    public static String emptyStringForNull(String in) {
        return in == null ? "" : in;
    }

    public static String getRidOfMultipleSpaces(String in) {
        return in.replaceAll("\\s+"," ");
    }

}
