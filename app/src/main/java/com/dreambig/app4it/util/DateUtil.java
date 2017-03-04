package com.dreambig.app4it.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public static String convertDateToMillisecondsSince1970(Date input) {
        return String.valueOf(input.getTime());
    }

    public static Date convertToDate(String input, DateFormat dateFormat) throws ParseException {
        return dateFormat.parse(input);
    }

	private static Date toDate(long dateInMiliseconds) {
		return new Date(dateInMiliseconds);
	}
	
	private static Date toDate(String dateInMiliseconds) {
		long dateInMil = Long.valueOf(dateInMiliseconds);
		return new Date(dateInMil);
	}

    public static String printAsDateInFormat(String dateInMiliseconds, DateFormat format) {
        return format.format(toDate(dateInMiliseconds));
    }

    public static String printAsDateInFormat(Long dateInMiliseconds, DateFormat format) {
        return format.format(toDate(dateInMiliseconds));
    }
	
	public static String printAsDate(String dateInMiliseconds) {
		DateFormat df = new SimpleDateFormat("EEEE dd MMM");		
		return df.format(toDate(dateInMiliseconds));
	}

    public static String printAsFormat(String dateInMiliseconds, String format) {
        DateFormat df = new SimpleDateFormat(format);
        return df.format(toDate(dateInMiliseconds));
    }
	
	public static String printAsDateTime(String dateInMiliseconds) {
		DateFormat df = new SimpleDateFormat("EEEE dd MMM HH:mm");
		return df.format(toDate(dateInMiliseconds));
	}
	
	public static String printAsDate(long dateInMiliseconds) {
		DateFormat df = new SimpleDateFormat("EEEE dd MMM");		
		return df.format(toDate(dateInMiliseconds));
	}
	
	public static String printAsDateTime(long dateInMiliseconds) {
		DateFormat df = new SimpleDateFormat("EEEE dd MMM HH:mm");
		return df.format(toDate(dateInMiliseconds));
	}

    public static int[] getYearMonthDay() {
        Calendar calendar = Calendar.getInstance();

        return new int[]{calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)};
    }

    public static int[] getYearMonthDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return new int[]{calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)};
    }

    public static Date yearMonthDayToDate(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR,year);
        calendar.set(Calendar.MONTH,month);
        calendar.set(Calendar.DAY_OF_MONTH,day);

        return calendar.getTime();
    }

    public static boolean isOlderThanXDays(Date date, int x) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH,x * -1);
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        Date xDaysAgo = calendar.getTime();

        return date.before(xDaysAgo);
    }
}
