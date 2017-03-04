package com.dreambig.app4it;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.dreambig.app4it.api.LogInGoHomeUser;
import com.dreambig.app4it.helper.GCMSupport;
import com.dreambig.app4it.helper.LogInGoHomeHelper;
import com.dreambig.app4it.helper.PasswordNegotiationHelper;
import com.dreambig.app4it.helper.RegistrationHelper;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.util.MessageIdentifiers;


public class StartActivity extends Activity implements LogInGoHomeUser {


    private App4ItApplication.PersistedInfo persistedInfo;


    //refactored start
    private void freeze() {
        Button startButton = (Button)findViewById(R.id.btnStart);
        TextView bottomLabel = (TextView)findViewById(R.id.bottomLine);

        startButton.setEnabled(false);
        bottomLabel.setTypeface(UIHelper.getOrCreateOurFont(this));
        bottomLabel.setVisibility(View.VISIBLE);
    }

    public void defreeze() {
        Button startButton = (Button)findViewById(R.id.btnStart);
        TextView bottomLabel = (TextView)findViewById(R.id.bottomLine);

        if(startButton != null) startButton.setEnabled(true);
        if(bottomLabel != null ) bottomLabel.setVisibility(View.INVISIBLE);
    }

    private void populateInternationalCodesSpinner() {
        Spinner spinner = (Spinner)findViewById(R.id.phoneNumberPrefix);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.international_phone_prefixes,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(30,false);
    }

    private void addActionToStartButton() {
		Button startButton = (Button)findViewById(R.id.btnStart);
		startButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				//L og.d("StartActivity", "Start button clicked");

				EditText phoneNumberMainPart = (EditText)findViewById(R.id.phoneNumberMainPart);
                String insertedPhoneNumberPrefix = (String)((Spinner)findViewById(R.id.phoneNumberPrefix)).getSelectedItem();
				String insertedPhoneNumberMainPart = phoneNumberMainPart.getText().toString().trim();
                boolean prefixOk = RegistrationHelper.validateRegistrationInputPrefixPart(insertedPhoneNumberPrefix);
                boolean mainPartOk = RegistrationHelper.validateRegistrationInputMainPart(insertedPhoneNumberMainPart);

                if(!prefixOk || !mainPartOk) {
                    UIHelper.showRegistrationInputInvalid(getApplicationContext(), !prefixOk, !mainPartOk);
                } else {
                    hideKeyboard(phoneNumberMainPart);
                    proceedToPasswordConfirmation(RegistrationHelper.insertedPhonePrefixToUsable(insertedPhoneNumberPrefix),RegistrationHelper.insertedPhoneMainPartToUsable(insertedPhoneNumberMainPart));
			    }

		    }
                });
    }

    private void proceedToPasswordConfirmation(final String phonePrefix, final String phoneMainPart) {
        freeze();
        PasswordNegotiationHelper.sendMeATextMessage(phonePrefix + phoneMainPart, new PasswordNegotiationHelper.MessageRequestCreatedCallback() {
            @Override
            public void success(Long claimId) {
                defreeze();
                navigateToConfirmationCodeInsert(phonePrefix,phoneMainPart,claimId);
            }

            @Override
            public void failure(String error) {
                defreeze();
                UIHelper.showLongMessage(getApplicationContext(),"Failed sending you the text :-( " + error);
            }
        });

    }

    private void navigateToConfirmationCodeInsert(final String phonePrefix, String phoneMainPart, Long passwordClaimId) {
        Intent intent = new Intent(getApplicationContext(), StartTwoActivity.class);
        intent.putExtra(MessageIdentifiers.PHONE_PREFIX, phonePrefix);
        intent.putExtra(MessageIdentifiers.PHONE_MAIN_PART, phoneMainPart);
        intent.putExtra(MessageIdentifiers.PASSWORD_CLAIM_ID, passwordClaimId);
        startActivity(intent);
    }

    private void prepareForRegistrationDialog() {
        setContentView(R.layout.activity_start);
        populateInternationalCodesSpinner();
        addActionToStartButton();
    }

    private void prepareForBounceOff() {
        setContentView(R.layout.activity_start_bounce);
    }

    private void showAppNeedsGoogleAppServices() {
        setContentView(R.layout.activity_app_not_supported);
    }

    private void directAlreadyRegisteredUserHome(App4ItApplication.PersistedInfo persistedInfo) {
        LogInGoHomeHelper.logInAndGoHome(this,persistedInfo.getEmail(),persistedInfo.getPassword(),RegistrationHelper.fullPhoneNumberFromEmail(persistedInfo.getEmail()),persistedInfo.getInternationalCode(),false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(GCMSupport.areGoogleAppServicesReady(this)) {
            persistedInfo = ((App4ItApplication)getApplication()).getPersistedInfo();

            if(persistedInfo != null) {
                prepareForBounceOff();
            } else {
                prepareForRegistrationDialog();
            }
        } else {
            showAppNeedsGoogleAppServices();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(persistedInfo != null) {
            directAlreadyRegisteredUserHome(persistedInfo);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        ((App4ItApplication)getApplication()).activityStops();
    }

    private void hideKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(),0);
    }

    //refactored end

}
