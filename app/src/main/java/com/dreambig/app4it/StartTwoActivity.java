package com.dreambig.app4it;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dreambig.app4it.api.FirebaseCallCallback;
import com.dreambig.app4it.api.LogInGoHomeUser;
import com.dreambig.app4it.helper.LogInGoHomeHelper;
import com.dreambig.app4it.helper.PasswordNegotiationHelper;
import com.dreambig.app4it.helper.RegistrationHelper;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.firebase.client.FirebaseError;

public class StartTwoActivity extends Activity implements LogInGoHomeUser {

    private String phonePrefix;
    private String phoneMainPart;
    private Long claimId;

    private void freeze() {
        Button goButton = (Button)findViewById(R.id.btnConfirm);
        TextView bottomLabel = (TextView)findViewById(R.id.bottomLine);

        goButton.setEnabled(false);
        bottomLabel.setTypeface(UIHelper.getOrCreateOurFont(this));
        bottomLabel.setVisibility(View.VISIBLE);
    }

    public void defreeze() {
        Button goButton = (Button)findViewById(R.id.btnConfirm);
        TextView bottomLabel = (TextView)findViewById(R.id.bottomLine);

        if(goButton != null) goButton.setEnabled(true);
        if(bottomLabel != null ) bottomLabel.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start_two);
        parseIntentIntoInstanceVariables(getIntent());
        addActionToGoButton();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

        ((App4ItApplication)getApplication()).activityStops();
    }

    private void addActionToGoButton() {
        Button goButton = (Button)findViewById(R.id.btnConfirm);
        goButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                EditText confirmationCodeView = (EditText)findViewById(R.id.confirmationCode);
                String confirmationCode = confirmationCodeView.getText().toString().trim();

                if(confirmationCode.equals("")) {
                    UIHelper.showBriefMessage(getApplicationContext(),"Please insert the code you received in the text");
                } else {
                    hideKeyboard(confirmationCodeView);
                    claimPasswordAndRegister(confirmationCode);
                }

            }
        });
    }

    private void claimPasswordAndRegister(String code) {
        freeze();
        PasswordNegotiationHelper.retrievePassword(claimId,code,new PasswordNegotiationHelper.PasswordRequestCallback() {
            @Override
            public void success(String password) {
                registerInFirebaseLogInAndGoHome(phonePrefix, phoneMainPart, password);
            }

            @Override
            public void failure(String reason) {
                defreeze();
                UIHelper.showLongMessage(getApplicationContext(),mapPasswordClaimCodeToErrorMessage(reason));
            }
        });

    }

    private String mapPasswordClaimCodeToErrorMessage(String code) {
        if("CLAIM_NOT_FOUND".equalsIgnoreCase(code)) return "Something went wrong on the server side, please ask for another text";
        else if("CODE_DOESNT_MATCH".equalsIgnoreCase(code)) return "This code doesn't match the one in the message";
        else if("PASSWORD_FAILED_CREATING".equalsIgnoreCase(code)) return "Something went wrong with creating your account, please try again or contact the support team";
        else return code; //could be directly an error message
    }

    private void registerInFirebaseLogInAndGoHome(final String phonePrefix, String phoneMainPart, final String password) {
        final String fullPhoneNumber = phonePrefix + phoneMainPart;
        final String registrationEmail = RegistrationHelper.createRegistrationEmail(fullPhoneNumber);
        final FirebaseGateway firebaseGateway = new FirebaseGateway(this);
        firebaseGateway.registerNewAccount(registrationEmail, password, new FirebaseCallCallback() {
            @Override
            public void onSuccess() {
                LogInGoHomeHelper.logInAndGoHome(StartTwoActivity.this,registrationEmail, password, fullPhoneNumber, phonePrefix, true);
            }

            @Override
            public void onFailure(FirebaseError firebaseError) {
                if(firebaseError.getCode() == FirebaseError.EMAIL_TAKEN) {
                    //this happens in the scenario when user reinstalls the app. they are already registered. the password stays the same so we should be able to use it to just log in
                    LogInGoHomeHelper.logInAndGoHome(StartTwoActivity.this,registrationEmail, password, fullPhoneNumber, phonePrefix, true);
                } else {
                    defreeze();
                    UIHelper.showBriefMessage(getApplicationContext(),"Registration failed :-( " + firebaseError.getMessage());
                }
            }
        });


    }

    private void parseIntentIntoInstanceVariables(Intent intent) {
        phonePrefix = intent.getStringExtra(MessageIdentifiers.PHONE_PREFIX);
        phoneMainPart = intent.getStringExtra(MessageIdentifiers.PHONE_MAIN_PART);
        claimId = intent.getLongExtra(MessageIdentifiers.PASSWORD_CLAIM_ID, 0);
    }

    private void hideKeyboard(EditText editText) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(),0);
    }

}
