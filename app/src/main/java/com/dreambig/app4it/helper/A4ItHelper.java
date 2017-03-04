package com.dreambig.app4it.helper;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.dreambig.app4it.R;
import com.dreambig.app4it.enums.Format;
import com.dreambig.app4it.enums.InvitationStatus;
import com.dreambig.app4it.fragment.NewActivityDetailsFragment;
import com.dreambig.app4it.util.DateUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Alexandr on 03/01/2015.
 */
public class A4ItHelper {


    public static int compareInvitationStatus(InvitationStatus first, InvitationStatus second) {

        //treat DELETED just like NOT_GOING
        if(first == InvitationStatus.DELETED) first = InvitationStatus.NOT_GOING;
        if(second == InvitationStatus.DELETED) second = InvitationStatus.NOT_GOING;

        //defensive
        if(first == null && second == null) {
            return 0;
        } else if (first == null) {
            return 1;
        } else if (second == null) {
            return -1;
        } else if(first.equals(second)) {
            return 0;
        } else if (InvitationStatus.GOING.equals(first)) {
            return -1;
        } else if (InvitationStatus.GOING.equals(second)) {
            return 1;
        } else if (InvitationStatus.NOT_GOING.equals(first)) {
            return -1;
        } else if (InvitationStatus.NOT_GOING.equals(second)) {
            return 1;
        } else {
            //none of them is going, none of them is not going, there must be both invited
            //never should get here
            return 0;
        }
    }

    public static int[] provideYearMonthDay(EditText editText, DateFormat dateFormat, DateFormat dateTimeFormat) {
        String currentText = editText.getText().toString().trim();
        if(currentText.equals("")) {
            return DateUtil.getYearMonthDay();
        } else {
            try {
                Date date = dateFormat.parse(currentText);
                return DateUtil.getYearMonthDay(date);
            } catch (ParseException e) {
                try {
                    Date date = dateTimeFormat.parse(currentText);
                    return DateUtil.getYearMonthDay(date);
                } catch (ParseException e2) {
                    return DateUtil.getYearMonthDay();
                }
            }
        }
    }

    public static Date getDateFromDatePickerAndTimePicker(DatePicker datePicker, TimePicker timePicker) {
        Date date = DateUtil.yearMonthDayToDate(datePicker.getYear(),datePicker.getMonth(),datePicker.getDayOfMonth());
        int hour = timePicker.getCurrentHour();
        int minute = timePicker.getCurrentMinute();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY,hour);
        calendar.set(Calendar.MINUTE,minute);

        return calendar.getTime();
    }

    public static String getDateTimeDialogTitle(DatePicker datePicker, TimePicker timePicker, DateFormat dateFormat) {
        Date date = getDateFromDatePickerAndTimePicker(datePicker,timePicker);

        return dateFormat.format(date) + " at " + (new SimpleDateFormat("HH:mm")).format(date);
    }

    public static void setTextToView(View contextView, int resourceId, String text) {
        ((EditText)contextView.findViewById(resourceId)).setText(text);
    }

    public static String getTextFromView(View contextView, int resourceId) {
        return ((EditText)contextView.findViewById(resourceId)).getText().toString().trim();
    }

    public static String getWhenValueForType(Format whenType, View contextView) throws ParseException {

        String contentOfTextField = getTextFromView(contextView, R.id.newActivityLayout_when);

        if(Format.FREETEXT.equals(whenType)) {
            return contentOfTextField;
        } else if (Format.DATE.equals(whenType)) {
            //parse the string to date
            Date date = DateUtil.convertToDate(contentOfTextField, NewActivityDetailsFragment.dateFormat);
            return DateUtil.convertDateToMillisecondsSince1970(date);
        } else {
            //must be date time
            Date date = DateUtil.convertToDate(contentOfTextField, NewActivityDetailsFragment.dateTimeFormat);
            return DateUtil.convertDateToMillisecondsSince1970(date);
        }

    }

    public static void hideKeyboard(Context context, EditText editText) {
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(),0);
    }

}
