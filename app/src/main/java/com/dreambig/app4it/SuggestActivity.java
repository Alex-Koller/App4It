package com.dreambig.app4it;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.dreambig.app4it.api.FirebaseSuggestionsCallback;
import com.dreambig.app4it.api.NewsCenter;
import com.dreambig.app4it.entity.App4ItActivityParcel;
import com.dreambig.app4it.entity.App4ItAddressAndMapLocation;
import com.dreambig.app4it.entity.App4ItMapLocation;
import com.dreambig.app4it.entity.App4ItSuggestion;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.enums.Format;
import com.dreambig.app4it.enums.SuggestionType;
import com.dreambig.app4it.fragment.LoadingFragment;
import com.dreambig.app4it.fragment.SuggestPreferencesFragment;
import com.dreambig.app4it.helper.A4ItHelper;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.impl.NewsCenterImpl;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.DateUtil;
import com.dreambig.app4it.util.MapHelper;
import com.dreambig.app4it.util.MessageIdentifiers;

public class SuggestActivity extends Activity {

    private static final int REQUEST_FOR_MAP_INPUT = 1;

    private String addressInsertedThroughMap;
    private App4ItMapLocation mapLocationInsertedThroughMap;

	private boolean areWeLive;
	private SuggestionType type;
    private App4ItActivityParcel activityParcel;
    private String loggedInUserId;
    private String loggedInUserNumber;
    private Format currentlySelected;

    private int selectedFormatColor = Color.parseColor("#009DC0");
    private int notSelectedFormatColor = Color.parseColor("#000000");
    private DateFormat dateFormat = new SimpleDateFormat("EEEE dd MMM yyyy");
    private DateFormat dateTimeFormat = new SimpleDateFormat("EEEE dd MMM yyyy HH:mm");

    //views
    private SuggestPreferencesFragment preferencesFragment;
    private Dialog dateDialog;
    private Dialog dateTimeDialog;
    private Dialog mapDialog;

    private App4ItApplication getDelegate() {
        return ((App4ItApplication)getApplication());
    }

    private void addActionsToButtons() {

        if(type.equals(SuggestionType.TIME)) {

            (findViewById(R.id.btnFreetext)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    userSelected(Format.FREETEXT);
                }
            });

            (findViewById(R.id.btnDate)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    userSelected(Format.DATE);
                }
            });

            (findViewById(R.id.btnDateTime)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    userSelected(Format.DATE_TIME);
                }
            });

            findViewById(R.id.editSuggestionField).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Format.DATE.equals(currentlySelected)) {
                        hideKeyboard((EditText) v);
                        userSelected(Format.DATE);
                    } else if (Format.DATE_TIME.equals(currentlySelected)) {
                        hideKeyboard((EditText) v);
                        userSelected(Format.DATE_TIME);
                    }
                }
            });

        }

        if(type.equals(SuggestionType.PLACE)) {
            findViewById(R.id.btnMapSuggestion).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigateToWriteMap();
                }
            });
        }


        findViewById(R.id.btnSaveSuggestion).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //note that preferencesFragment should be in place now because the save button is disabled until the suggestions are downloaded
                String inputValue = getSuggestionValue();

                if(inputValue != null && !inputValue.trim().equals("")) {
                    if(preferencesFragment.isThereAlreadySuchSuggestionFormat(currentlySelected,getRawSuggestionValue(inputValue))) {
                        UIHelper.showBriefMessage(getApplicationContext(),"This suggestion is already there");
                    } else {
                        hideKeyboard(getEditField());

                        if(SuggestionType.PLACE.equals(type)) {
                            if(areMapQuestionsSorted()) {
                                proceedWithSaving();
                            }
                        } else {
                            proceedWithSaving();
                        }
                    }
                }
            }
        });
    }

    private boolean areMapQuestionsSorted() {
        if(getSuggestionValue().equals(addressInsertedThroughMap) && mapLocationInsertedThroughMap != null) {
            return true;
        } else if (mapLocationInsertedThroughMap == null) {
            offerAddingMapLocation();
            return false;
        } else if (!getSuggestionValue().equals(addressInsertedThroughMap)) {
            offerUpdatingTheMapLocationToReflectWhereValue();
            return false;
        } else {
            return true; //seems reasonable default
        }
    }

    private void offerUpdatingTheMapLocationToReflectWhereValue() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.update_the_map_location));
        builder.setMessage(getResources().getString(R.string.suggestion_changed).replace("THIS_LOCATION",getSuggestionValue()));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                userWantsToSearchForTheWhereString();
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                proceedWithSaving();
            }
        });

        mapDialog = builder.create();
        mapDialog.show();
    }

    private void offerAddingMapLocation() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.do_you_want_to_add_map_location));
        builder.setMessage(getResources().getString(R.string.for_this).replace("THIS_LOCATION",getSuggestionValue()));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                userWantsToSearchForTheWhereString();
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                proceedWithSaving();
            }
        });

        mapDialog = builder.create();
        mapDialog.show();
    }

    private void userWantsToSearchForTheWhereString() {
        List<Address> addresses = MapHelper.findMapAddressForString(this, getSuggestionValue());
        if(addresses == null || addresses.size() == 0) {
            offerNothingFoundSaveAnyway();
        } else if (addresses.size() > 1) {
            offerTooManyResultsOpenInMap();
        } else {
            Address address = addresses.get(0);
            addressInsertedThroughMap = getSuggestionValue();
            mapLocationInsertedThroughMap = new App4ItMapLocation(address.getLatitude(),address.getLongitude());
            proceedWithSaving();
        }
    }

    private void offerTooManyResultsOpenInMap() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.more_than_one_result));
        builder.setMessage(getResources().getString(R.string.do_you_want_to_open_map));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                navigateToWriteMap();
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //nothing to do
            }
        });

        mapDialog = builder.create();
        mapDialog.show();
    }

    private void offerNothingFoundSaveAnyway() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.nothing_found_on_the_map));
        builder.setMessage(getResources().getString(R.string.save_anyway));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mapLocationInsertedThroughMap = null;
                addressInsertedThroughMap = null;
                proceedWithSaving();
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //nothing to do
            }
        });

        mapDialog = builder.create();
        mapDialog.show();
    }

    private void proceedWithSaving() {
        String inputValue = getSuggestionValue();

        App4ItUser tempUser = new App4ItUser(loggedInUserId);
        tempUser.setNumber(loggedInUserNumber);


        FirebaseGateway firebaseGateway = new FirebaseGateway(getApplicationContext());
        App4ItSuggestion suggestion = firebaseGateway.saveSuggestionForActivityId(activityParcel.getActivityId(),type,currentlySelected,getRawSuggestionValue(inputValue),tempUser, mapLocationInsertedThroughMap);

        //post news about it out to the world. could potentially be a callback in the above firebase method
        NewsCenter newsCenter = new NewsCenterImpl();
        newsCenter.postNewsAboutSuggestion(getApplicationContext(),activityParcel.getActivityId(),activityParcel.getTitle(),loggedInUserId,type);

        //stick it in. as this is not being updated real time
        preferencesFragment.addSuggestion(suggestion);

        //empty the input field
        setSuggestionValue("");

        mapLocationInsertedThroughMap = null;
        addressInsertedThroughMap = null;
    }



    private String getRawSuggestionValue(String inputValue) {

        String trimmedInputValue = inputValue.trim();

        try {

            if (Format.FREETEXT.equals(currentlySelected)) {
                return trimmedInputValue;
            } else if (Format.DATE.equals(currentlySelected)) {
                //parse the string to date
                Date date = dateFormat.parse(trimmedInputValue);
                return String.valueOf(date.getTime());
            } else {
                //must be DATE TIME format
                Date date = dateTimeFormat.parse(trimmedInputValue);
                return String.valueOf(date.getTime());
            }

        } catch(ParseException e) {
            throw new RuntimeException(e);
        }

    }

    private void showKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText,0);

        editText.setSelection(editText.getText().length());
    }

    private void hideKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(),0);
    }

    private void setTextColorsOfButton(Button selectedButton, Button ...unselectedButtons) {
        for(Button unselectedItem : unselectedButtons) {
            unselectedItem.setTextColor(notSelectedFormatColor);
        }

        selectedButton.setTextColor(selectedFormatColor);
    }

    private void userSelected(Format format) {
        currentlySelected = format;
        setButtonsColorsBasedOnSelectedFormat(format);

        if(Format.FREETEXT.equals(format)) {
            showKeyboard((EditText)findViewById(R.id.editSuggestionField));
        } else if (Format.DATE.equals(format)) {
            createDatePickerDialog().show();
        } else if (Format.DATE_TIME.equals(format)) {
            createDateTimePickerDialog().show();
        }

    }

    private DatePickerDialog createDatePickerDialog() {

        int[] yearMonthDay = A4ItHelper.provideYearMonthDay(((EditText)findViewById(R.id.editSuggestionField)),dateFormat,dateTimeFormat);

        dateDialog = new DatePickerDialog(this,getDateChangeListener(),yearMonthDay[0],yearMonthDay[1],yearMonthDay[2]);

        return (DatePickerDialog)dateDialog;
    }

    private DatePickerDialog.OnDateSetListener getDateChangeListener() {
        return new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                ((EditText)findViewById(R.id.editSuggestionField)).setText(dateFormat.format(DateUtil.yearMonthDayToDate(year,monthOfYear,dayOfMonth)));
            }
        };
    }

    private Dialog createDateTimePickerDialog() {

        if(dateTimeDialog != null) {
            return dateTimeDialog;
        } else {
            View contentView = getLayoutInflater().inflate(R.layout.date_time_dialog,null);

            AlertDialog.Builder db = new AlertDialog.Builder(this);
            db.setView(contentView);
            final DatePicker datePicker = (DatePicker) contentView.findViewById(R.id.datePickerPart);
            final TimePicker timePicker = (TimePicker) contentView.findViewById(R.id.timePickerPart);
            db.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((EditText)findViewById(R.id.editSuggestionField)).setText(dateTimeFormat.format(A4ItHelper.getDateFromDatePickerAndTimePicker(datePicker,timePicker)));
                    dateTimeDialog.dismiss();
                }
            });
            dateTimeDialog = db.create();
            timePicker.setIs24HourView(true);

            int[] yearMonthDay = A4ItHelper.provideYearMonthDay(((EditText)findViewById(R.id.editSuggestionField)),dateFormat,dateTimeFormat);
            datePicker.init(yearMonthDay[0],yearMonthDay[1],yearMonthDay[2],new DatePicker.OnDateChangedListener() {
                @Override
                public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    dateTimeDialog.setTitle(A4ItHelper.getDateTimeDialogTitle(datePicker,timePicker,dateFormat));
                    ((EditText)findViewById(R.id.editSuggestionField)).setText(dateTimeFormat.format(A4ItHelper.getDateFromDatePickerAndTimePicker(datePicker,timePicker)));
                }
            });

            timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                    dateTimeDialog.setTitle(A4ItHelper.getDateTimeDialogTitle(datePicker,timePicker,dateFormat));
                    ((EditText)findViewById(R.id.editSuggestionField)).setText(dateTimeFormat.format(A4ItHelper.getDateFromDatePickerAndTimePicker(datePicker,timePicker)));
                }
            });
            dateTimeDialog.setTitle(A4ItHelper.getDateTimeDialogTitle(datePicker,timePicker,dateFormat));
            return dateTimeDialog;
        }

    }


    private void setButtonsColorsBasedOnSelectedFormat(Format format) {
        Button freetext = (Button)findViewById(R.id.btnFreetext);
        Button date = (Button)findViewById(R.id.btnDate);
        Button dateTime = (Button)findViewById(R.id.btnDateTime);

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

    private void displayLoadingView() {
        LoadingFragment loadingFragment = new LoadingFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.suggest_activity_frame_container, loadingFragment);
        fragmentTransaction.commit();
    }

    private void displaySuggestions(List<App4ItSuggestion> suggestions) {
	    SuggestPreferencesFragment preferencesFragment = new SuggestPreferencesFragment();
        preferencesFragment.setArguments(getArgumentsForPreferencesFragment(type));
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.suggest_activity_frame_container, preferencesFragment);
		fragmentTransaction.commit();
        preferencesFragment.setSuggestions(suggestions);
        this.preferencesFragment = preferencesFragment;
    }

    private Bundle getArgumentsForPreferencesFragment(SuggestionType type) {
		Bundle arguments = new Bundle();
		arguments.putString(MessageIdentifiers.SUGGEST_TYPE, type.toString());
        arguments.putParcelable(MessageIdentifiers.ACTIVITY_PARCEL, activityParcel);
        arguments.putBoolean(MessageIdentifiers.IS_HOME_USER_OWNER, loggedInUserId.equals(activityParcel.getCreatedByUserId()));
        return arguments;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(MessageIdentifiers.SELECTED_FORMAT, currentlySelected.toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dateTimeDialog != null) {
            dateTimeDialog.dismiss();
            dateTimeDialog = null;
        }

        if(dateDialog != null) {
            dateDialog.dismiss();
            dateDialog = null;
        }

        if(mapDialog != null) {
            mapDialog.dismiss();
            mapDialog = null;
        }

    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//L og.d("SuggestActivity","onCreate()...");

        populateInstanceVariables(getIntent().getExtras());

        //define view
        if(SuggestionType.TIME.equals(type)) {
            setContentView(R.layout.activity_suggest_time);
            getActionBar().setTitle(R.string.title_activity_suggest_time);
        }
        else {
            setContentView(R.layout.activity_suggest_place);
            getActionBar().setTitle(R.string.title_activity_suggest_place);
        }

        //stick in the 'loading...' sign
        displayLoadingView();

        //a format has to be selected
        createState(savedInstanceState);

        //add callbacks to buttons
        addActionsToButtons();

        //it would be awkward to let the user save new suggestion when the existing ones are still loading
        findViewById(R.id.btnSaveSuggestion).setEnabled(false);
        //also if we leave to map whilst it's still loading then when we come back and it loaded in the meantime it will never display
        if(SuggestionType.PLACE.equals(type)) {
            findViewById(R.id.btnMapSuggestion).setEnabled(false);
        }
		
		//feed the data in
        FirebaseGateway firebaseGateway = new FirebaseGateway(this);
        firebaseGateway.downloadSuggestionsForActivity(activityParcel.getActivityId(),loggedInUserId,type,getDelegate(),new FirebaseSuggestionsCallback() {
            @Override
            public void suggestionsDownloaded(List<App4ItSuggestion> suggestions) {
                //L og.d("SuggestActivity","Suggestions downloaded");
                if(areWeLive) {
                    displaySuggestions(suggestions);
                }
                findViewById(R.id.btnSaveSuggestion).setEnabled(true);
                if(SuggestionType.PLACE.equals(type)) {
                    findViewById(R.id.btnMapSuggestion).setEnabled(true);
                }
            }
        });
		
	}

    private void populateInstanceVariables(Bundle extras) {
        this.type = SuggestionType.valueOf(extras.getString(MessageIdentifiers.SUGGEST_TYPE));
        this.activityParcel = extras.getParcelable(MessageIdentifiers.ACTIVITY_PARCEL);

        this.loggedInUserId = getDelegate().getLoggedInUserId();
        this.loggedInUserNumber = getDelegate().getLoggedInUserNumber();
    }

    private void createState(Bundle savedInstanceState) {
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

        if(type.equals(SuggestionType.TIME)) {
            setButtonsColorsBasedOnSelectedFormat(currentlySelected);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        areWeLive = true;
        getDelegate().activityStarts(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        areWeLive = false;
        getDelegate().activityStops();
    }

    private EditText getEditField() {
        return ((EditText)findViewById(R.id.editSuggestionField));
    }

    private String getSuggestionValue() {
        EditText editField = getEditField();
        return editField.getText().toString();
    }

    private void setSuggestionValue(String value) {
        EditText editField = getEditField();
        editField.setText(value);
    }


    private void navigateToWriteMap() {
        Intent intent = new Intent(this, MapWriteActivity.class);
        String currentWhereValue = getSuggestionValue();

        intent.putExtra(MessageIdentifiers.ACTIVITY_MAP_ADDRESS, currentWhereValue);

        startActivityForResult(intent, REQUEST_FOR_MAP_INPUT);
    }

    public void navigateToReadMap() {
        ArrayList<App4ItAddressAndMapLocation> toSend = new ArrayList<>();
        for(App4ItSuggestion suggestion : preferencesFragment.getSuggestions()) {
            if(suggestion.getMapLocation() != null) {
                App4ItAddressAndMapLocation item = new App4ItAddressAndMapLocation(suggestion.getValue(),suggestion.getMapLocation());
                toSend.add(item);
            }
        }

        if(toSend.size() > 0) {
            if(toSend.size() < preferencesFragment.getSuggestions().size()) {
                Toast.makeText(this, "Some of the suggested places are not on the map", Toast.LENGTH_LONG).show();
            }

            Intent intent = new Intent(this, MapReadActivity.class);
            intent.putParcelableArrayListExtra(MessageIdentifiers.ADDRESSES_AND_LOCATIONS,toSend);
            intent.putExtra(MessageIdentifiers.MAP_READ_TITLE,getResources().getString(R.string.suggestions_on_the_map));
            startActivity(intent);
        } else {
            Toast.makeText(this, "None of the suggested places has a map location saved with it.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_FOR_MAP_INPUT) {
            if(resultCode == MapWriteActivity.ACTION_DONE) {
                String addressString = data.getStringExtra(MessageIdentifiers.ACTIVITY_MAP_ADDRESS);
                setSuggestionValue(addressString);
                addressInsertedThroughMap = addressString;
                mapLocationInsertedThroughMap = data.getParcelableExtra(MessageIdentifiers.ACTIVITY_MAP_LOCATION);
            }
        }
    }
}
