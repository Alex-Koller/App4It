package com.dreambig.app4it.fragment;


import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;

import com.dreambig.app4it.EditActivityActivity;
import com.dreambig.app4it.MapReadActivity;
import com.dreambig.app4it.MapWriteActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.entity.App4ItActivity;
import com.dreambig.app4it.entity.App4ItActivityParcel;
import com.dreambig.app4it.entity.App4ItMapLocation;
import com.dreambig.app4it.enums.Format;
import com.dreambig.app4it.helper.A4ItHelper;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.util.DateUtil;
import com.dreambig.app4it.util.MessageIdentifiers;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class NewActivityDetailsFragment extends Fragment {

    private static final int REQUEST_FOR_MAP_INPUT = 1;
    private Format currentlySelected;
    private int selectedFormatColor = Color.parseColor("#009DC0");
    private int notSelectedFormatColor = Color.parseColor("#000000");
    public static final DateFormat dateFormat = new SimpleDateFormat("EEEE dd MMM yyyy");
    public static final DateFormat dateTimeFormat = new SimpleDateFormat("EEEE dd MMM yyyy HH:mm");
    private Dialog dateDialog;
    private Dialog dateTimeDialog;
    private String addressInsertedThroughMap;
    private App4ItMapLocation mapLocationInsertedThroughMap;

    public String getAddressInsertedThroughMap() {
        return addressInsertedThroughMap;
    }

    public App4ItMapLocation getMapLocationInsertedThroughMap() {
        return mapLocationInsertedThroughMap;
    }

    public void setMapLocationInsertedThroughMap(App4ItMapLocation mapLocationInsertedThroughMap) {
        this.mapLocationInsertedThroughMap = mapLocationInsertedThroughMap;
    }

    @Override
	public void onStart() {
		super.onStart();
	}	


    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {    	        	
    	View ret = inflater.inflate(R.layout.new_activity_details_fragment, container, false);
    	
		feedTheEventTypeSpinner(ret);
        createState(ret,savedInstanceState);

		return ret;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        addActionsToButtons(view);
        fillInFieldsIfApplicable();

    }

    private boolean isThisEdit() {
        return getActivity() instanceof EditActivityActivity;
    }

    private void fillInFieldsIfApplicable() {
        if(isThisEdit()) {
            ((EditActivityActivity)getActivity()).fillMeInWithActivityDetails(this);
        }
    }

    private void feedTheEventTypeSpinner(View view) {
        Spinner spinner = (Spinner) view.findViewById(R.id.newActivityLayout_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.activity_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void createState(View mainView, Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            String currentlySelectedFormat = savedInstanceState.getString(MessageIdentifiers.SELECTED_FORMAT);
            if (currentlySelectedFormat != null && !currentlySelectedFormat.trim().equals("")) {
                currentlySelected = Format.valueOf(currentlySelectedFormat);
            } else {
                currentlySelected = Format.FREETEXT;
            }
        } else {
            currentlySelected = Format.FREETEXT;
        }

        setButtonsColorsBasedOnSelectedFormat(mainView, currentlySelected);

        //otherwise the onclick on the edit text is not called  unless the edit already has focus
        if(currentlySelected != Format.FREETEXT) {
            setEditTextUnfocusable((EditText)mainView.findViewById(R.id.newActivityLayout_when));
        }
    }

    private void setButtonsColorsBasedOnSelectedFormat(View mainView, Format format) {
        Button freetext = (Button)mainView.findViewById(R.id.newActivityLayout_btnFreetext);
        Button date = (Button)mainView.findViewById(R.id.newActivityLayout_btnDate);
        Button dateTime = (Button)mainView.findViewById(R.id.newActivityLayout_btnDateTime);

        switch (format) {
            case FREETEXT:
                setTextColorsOfButton(freetext,date,dateTime);
                break;
            case DATE:
                setTextColorsOfButton(date,freetext,dateTime);
                break;
            case DATE_TIME:
                setTextColorsOfButton(dateTime,freetext,date);
                break;
        }
    }

    private void setTextColorsOfButton(Button selectedButton, Button ...unselectedButtons) {
        for(Button unselectedItem : unselectedButtons) {
            unselectedItem.setTextColor(notSelectedFormatColor);
        }

        selectedButton.setTextColor(selectedFormatColor);
    }

    private void addActionsToButtons(final View mainView) {
        (mainView.findViewById(R.id.newActivityLayout_btnFreetext)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userSelected(mainView, Format.FREETEXT);
            }
        });

        (mainView.findViewById(R.id.newActivityLayout_btnDate)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userSelected(mainView, Format.DATE);
            }
        });

        (mainView.findViewById(R.id.newActivityLayout_btnDateTime)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userSelected(mainView, Format.DATE_TIME);
            }
        });

        mainView.findViewById(R.id.newActivityLayout_when).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Format.DATE.equals(currentlySelected)) {
                    hideKeyboard((EditText) v);
                    userSelected(mainView, Format.DATE);
                } else if (Format.DATE_TIME.equals(currentlySelected)) {
                    hideKeyboard((EditText) v);
                    userSelected(mainView, Format.DATE_TIME);
                }
            }
        });

        (mainView.findViewById(R.id.newActivityLayout_btnMap)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToWriteMap();
            }
        });
    }

    public void navigateToWriteMap() {
        Intent intent = new Intent(getActivity(), MapWriteActivity.class);
        String currentWhereValue = A4ItHelper.getTextFromView(getView(), R.id.newActivityLayout_where);

        if(isThisEdit()) {
            intent.putExtra(MessageIdentifiers.ACTIVITY_MAP_ADDRESS, currentWhereValue);
        } else {
            intent.putExtra(MessageIdentifiers.ACTIVITY_MAP_ADDRESS, currentWhereValue);
        }

        startActivityForResult(intent, REQUEST_FOR_MAP_INPUT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_FOR_MAP_INPUT) {
            if(resultCode == MapWriteActivity.ACTION_DONE) {
                String addressString = data.getStringExtra(MessageIdentifiers.ACTIVITY_MAP_ADDRESS);
                setWhereField(addressString);
                addressInsertedThroughMap = addressString;
                mapLocationInsertedThroughMap = data.getParcelableExtra(MessageIdentifiers.ACTIVITY_MAP_LOCATION);
            }
        }
    }

    private void setWhereField(String value) {
        ((EditText)getView().findViewById(R.id.newActivityLayout_where)).setText(value);
    }

    private void userSelected(View mainView, Format format) {
        currentlySelected = format;
        setButtonsColorsBasedOnSelectedFormat(mainView, format);
        EditText whenEdit = (EditText)mainView.findViewById(R.id.newActivityLayout_when);

        if(Format.FREETEXT.equals(format)) {
            setEditTextFocusableAgain(whenEdit);
            showKeyboard(whenEdit);
        } else if (Format.DATE.equals(format)) {
            setEditTextUnfocusable(whenEdit);
            createDatePickerDialog(mainView).show();
        } else if (Format.DATE_TIME.equals(format)) {
            setEditTextUnfocusable(whenEdit);
            createDateTimePickerDialog(mainView).show();
        }

    }

    private void setEditTextUnfocusable(EditText editText) {
        editText.setFocusable(false);
    }

    private void setEditTextFocusableAgain(EditText editText) {
        //it has to be both of these. otherwise no work!
        editText.setFocusableInTouchMode(true);
        editText.setFocusable(true);
    }

    private DatePickerDialog createDatePickerDialog(View mainView) {

        int[] yearMonthDay = A4ItHelper.provideYearMonthDay((EditText)mainView.findViewById(R.id.newActivityLayout_when),dateFormat,dateTimeFormat);

        dateDialog = new DatePickerDialog(getActivity(),getDateChangeListener(),yearMonthDay[0],yearMonthDay[1],yearMonthDay[2]);

        return (DatePickerDialog)dateDialog;
    }

    private DatePickerDialog.OnDateSetListener getDateChangeListener() {
        return new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                View containingView = getView();
                if(containingView != null) { //this weirdly is called when the dialog is being dismissed in onDestroy
                    EditText editTextView = ((EditText) containingView.findViewById(R.id.newActivityLayout_when));
                    editTextView.setText(dateFormat.format(DateUtil.yearMonthDayToDate(year, monthOfYear, dayOfMonth)));
                }
            }
        };
    }

    private Dialog createDateTimePickerDialog(final View mainView) {

        if(dateTimeDialog != null) {
            return dateTimeDialog;
        } else {
            View contentView = getActivity().getLayoutInflater().inflate(R.layout.date_time_dialog, null);

            AlertDialog.Builder db = new AlertDialog.Builder(getActivity());
            db.setView(contentView);
            final DatePicker datePicker = (DatePicker) contentView.findViewById(R.id.datePickerPart);
            final TimePicker timePicker = (TimePicker) contentView.findViewById(R.id.timePickerPart);
            db.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((EditText)mainView.findViewById(R.id.newActivityLayout_when)).setText(dateTimeFormat.format(A4ItHelper.getDateFromDatePickerAndTimePicker(datePicker, timePicker)));
                    dateTimeDialog.dismiss();
                }
            });
            dateTimeDialog = db.create();
            timePicker.setIs24HourView(true);

            int[] yearMonthDay = A4ItHelper.provideYearMonthDay(((EditText)mainView.findViewById(R.id.newActivityLayout_when)),dateFormat,dateTimeFormat);
            datePicker.init(yearMonthDay[0],yearMonthDay[1],yearMonthDay[2],new DatePicker.OnDateChangedListener() {
                @Override
                public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    dateTimeDialog.setTitle(A4ItHelper.getDateTimeDialogTitle(datePicker, timePicker, dateFormat));
                    ((EditText)getView().findViewById(R.id.newActivityLayout_when)).setText(dateTimeFormat.format(A4ItHelper.getDateFromDatePickerAndTimePicker(datePicker, timePicker)));
                }
            });

            timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                    dateTimeDialog.setTitle(A4ItHelper.getDateTimeDialogTitle(datePicker, timePicker, dateFormat));
                    ((EditText)getView().findViewById(R.id.newActivityLayout_when)).setText(dateTimeFormat.format(A4ItHelper.getDateFromDatePickerAndTimePicker(datePicker, timePicker)));
                }
            });
            dateTimeDialog.setTitle(A4ItHelper.getDateTimeDialogTitle(datePicker, timePicker, dateFormat));
            return dateTimeDialog;
        }

    }

    private void showKeyboard(EditText editText) {
        InputMethodManager imgr = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        editText.requestFocus(); //strangely without this it doesn't work! but only in fragment
        imgr.showSoftInput(editText, 0);
        editText.setSelection(editText.getText().length());
    }

    private void hideKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(),0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(MessageIdentifiers.SELECTED_FORMAT, currentlySelected.toString());
    }

    @Override
    public void onDestroy() {
        if (dateTimeDialog != null) {
            dateTimeDialog.dismiss();
            dateTimeDialog = null;
        }

        if(dateDialog != null) {
            dateDialog.dismiss();
            dateDialog = null;
        }
        super.onDestroy();
    }

    public boolean isEverythingValidAndGoodToSaveEventDetails() {

        EditText titleEdit = (EditText)getView().findViewById(R.id.newActivityLayout_title);

        if(titleEdit.getText() ==  null || titleEdit.getText().toString().trim().equals("")) {
            //empty title we don't like
            UIHelper.showBriefMessage(getActivity(), "Please don't leave the title empty");
            return false;
        }

        return true;
    }

    public App4ItActivity scrapActivity() {

        String title = A4ItHelper.getTextFromView(getView(), R.id.newActivityLayout_title);
        String description = A4ItHelper.getTextFromView(getView(), R.id.newActivityLayout_description);
        Format whenType = currentlySelected;
        String whenValue;
        try {
            whenValue = A4ItHelper.getWhenValueForType(whenType, getView());
        } catch (ParseException e) {
            //well that means that we should store it as string. could happen when: put arbitrary string to when field, click date and rotate screen. then click save
            whenType = Format.FREETEXT;
            whenValue = A4ItHelper.getTextFromView(getView(), R.id.newActivityLayout_when);
        }

        String whereValue = A4ItHelper.getTextFromView(getView(), R.id.newActivityLayout_where);
        String type = ((Spinner)getView().findViewById(R.id.newActivityLayout_type)).getSelectedItem().toString().trim();

        //we are not setting whenAsString here, because it's not necessary
        App4ItActivity ret = new App4ItActivity("-1"); //temporary id. it's a scrap activity only
        ret.setTitle(title);
        ret.setMoreAbout(description);
        ret.setWhenFormat(whenType);
        ret.setWhenValue(whenValue);
        ret.setWhereAsString(whereValue);
        ret.setMapLocation(mapLocationInsertedThroughMap);
        ret.setType(type);

        return ret;
    }

    public void putActivity(App4ItActivityParcel activityParcel) {

        //title
        A4ItHelper.setTextToView(getView(), R.id.newActivityLayout_title,activityParcel.getTitle());
        //description
        A4ItHelper.setTextToView(getView(), R.id.newActivityLayout_description,activityParcel.getMoreAbout());
        //when format
        this.currentlySelected = activityParcel.getWhenFormat();
        //when value
        if(this.currentlySelected == Format.FREETEXT) {
            A4ItHelper.setTextToView(getView(), R.id.newActivityLayout_when, activityParcel.getWhenValue());
        } else if (this.currentlySelected == Format.DATE) {
            A4ItHelper.setTextToView(getView(), R.id.newActivityLayout_when, DateUtil.printAsDateInFormat(activityParcel.getWhenValue(), dateFormat));
        } else {
            A4ItHelper.setTextToView(getView(), R.id.newActivityLayout_when, DateUtil.printAsDateInFormat(activityParcel.getWhenValue(), dateTimeFormat));
        }
        //where value
        A4ItHelper.setTextToView(getView(), R.id.newActivityLayout_where, activityParcel.getWhereAsString());
        //type
        Spinner spinner = ((Spinner)getView().findViewById(R.id.newActivityLayout_type));
        ArrayAdapter spinnerAdapter = (ArrayAdapter)spinner.getAdapter();
        int positionToSet = spinnerAdapter.getPosition(activityParcel.getType());
        spinner.setSelection(positionToSet);

        //sort out UI state
        setButtonsColorsBasedOnSelectedFormat(getView(), currentlySelected);
        EditText whenEdit = (EditText)getView().findViewById(R.id.newActivityLayout_when);
        if(currentlySelected == Format.FREETEXT) {
            setEditTextFocusableAgain(whenEdit);
        } else {
            setEditTextUnfocusable(whenEdit);
        }
    }
}
