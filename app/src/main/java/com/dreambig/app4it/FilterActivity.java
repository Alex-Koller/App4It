package com.dreambig.app4it;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import com.dreambig.app4it.entity.FilterSettings;

import java.util.logging.Filter;


public class FilterActivity extends Activity {

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //L og.d("FilterActivity", "onResume() called");
        getDelegate().activityStarts(null);

    }



    @Override
    protected void onPause() {
        super.onPause();
        //L og.d("FilterActivity","onPause() called");
        storeFilterSettings();
        getDelegate().activityStops();
    }
	
    private void storeFilterSettings() {
        FilterSettings filterSettings = readFilterSettingsFromScreen();
        getDelegate().saveFilterSettings(filterSettings);
    }

    private FilterSettings readFilterSettingsFromScreen() {
        boolean showCatchup = getView(R.id.activity_filter_catchup).getAlpha() > 0.5f;
        boolean showCultural = getView(R.id.activity_filter_cultural).getAlpha() > 0.5f;
        boolean showNightout = getView(R.id.activity_filter_nightout).getAlpha() > 0.5f;
        boolean showSport = getView(R.id.activity_filter_sport).getAlpha() > 0.5f;
        boolean showFoodAndDrink = getView(R.id.activity_filter_foodanddrink).getAlpha() > 0.5f;
        boolean showUndisclosed = getView(R.id.activity_filter_undisclosed).getAlpha() > 0.5f;

        boolean showGoing = ((ToggleButton)getView(R.id.activity_filter_going)).isChecked();
        boolean showNotGoing = ((ToggleButton)getView(R.id.activity_filter_not_going)).isChecked();
        boolean showUnanswered = ((ToggleButton)getView(R.id.activity_filter_no_answer)).isChecked();
        boolean showCreatedByMe = ((ToggleButton)getView(R.id.activity_filter_created_by_me)).isChecked();

        return new FilterSettings(showUndisclosed,showCatchup,showCultural,showNightout,showSport,showFoodAndDrink,showGoing,showNotGoing,showUnanswered,showCreatedByMe);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_filter);
        FilterSettings currentFilterSettings = getDelegate().loadFilterSettings();

        createState(currentFilterSettings);
        addUserActionCallbacks();
	}

    private void createState(FilterSettings filterSettings) {
        getView(R.id.activity_filter_catchup).setAlpha(filterSettings.showCatchUp() ? 1.0f : 0.2f);
        getView(R.id.activity_filter_cultural).setAlpha(filterSettings.showCultural() ? 1.0f : 0.2f);
        getView(R.id.activity_filter_nightout).setAlpha(filterSettings.showNightOut() ? 1.0f : 0.2f);
        getView(R.id.activity_filter_sport).setAlpha(filterSettings.showSport() ? 1.0f : 0.2f);
        getView(R.id.activity_filter_foodanddrink).setAlpha(filterSettings.showFoodAndDrink() ? 1.0f : 0.2f);
        getView(R.id.activity_filter_undisclosed).setAlpha(filterSettings.showUndisclosed() ? 1.0f : 0.2f);

        setToggleButtonAccordingly(getView(R.id.activity_filter_going),filterSettings.showGoing());
        setToggleButtonAccordingly(getView(R.id.activity_filter_not_going),filterSettings.showNotGoing());
        setToggleButtonAccordingly(getView(R.id.activity_filter_no_answer),filterSettings.showUnanswered());
        setToggleButtonAccordingly(getView(R.id.activity_filter_created_by_me),filterSettings.showCreatedByMe());
    }

    private void setToggleButtonAccordingly(View view, boolean checkIt) {
        ToggleButton toggleButton = (ToggleButton)view;

        if(checkIt) {
            toggleButton.setTextColor(Color.parseColor("#000000"));
            toggleButton.setChecked(true);
        } else {
            toggleButton.setTextColor(Color.parseColor("#33000000"));
            toggleButton.setChecked(false);
        }
    }

    private void addUserActionCallbacks() {
        getView(R.id.activity_filter_catchup).setOnClickListener(new TypeClickListener());
        getView(R.id.activity_filter_cultural).setOnClickListener(new TypeClickListener());
        getView(R.id.activity_filter_nightout).setOnClickListener(new TypeClickListener());
        getView(R.id.activity_filter_sport).setOnClickListener(new TypeClickListener());
        getView(R.id.activity_filter_foodanddrink).setOnClickListener(new TypeClickListener());
        getView(R.id.activity_filter_undisclosed).setOnClickListener(new TypeClickListener());
    }

    private View getView(int id) {
        return findViewById(id);
    }

    private class TypeClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            float alphaLevel = v.getAlpha();
            if(alphaLevel > 0.5) {
                v.setAlpha(0.2f);
            } else {
                v.setAlpha(1.0f);
            }
        }
    }


    public void onToggleButtonClicked(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        if(toggleButton.isChecked()) {
           toggleButton.setTextColor(Color.parseColor("#000000"));
        } else {
            toggleButton.setTextColor(Color.parseColor("#33000000"));
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }



}
