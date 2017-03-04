package com.dreambig.app4it;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dreambig.app4it.api.FirebaseUserProfileCallback;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.firebase.client.FirebaseError;

public class TheirProfileActivity extends Activity  {

    private String userId;
    private String userName;

    private void parseIntentIntoInstanceVariables(Intent intent) {
        userId = intent.getStringExtra(MessageIdentifiers.USER_ID);
        userName = intent.getStringExtra(MessageIdentifiers.USER_NAME);
    }

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();
    }

    private void prettify() {
        getTheirProfileTextUnderPicture().setTypeface(UIHelper.getOrCreateOurFont(this), Typeface.BOLD);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_their_profile);
        prettify();
        parseIntentIntoInstanceVariables(getIntent());

        if(userName != null && !userName.trim().equals("")) {
            setTitle("User's profile: " + userName);
        } else {
            setTitle("User's profile");
        }

        putWaitASecVeilOn();
        FirebaseGateway firebaseGateway = new FirebaseGateway(this);
        firebaseGateway.downloadFullProfileForUserId(userId, new FirebaseUserProfileCallback() {
            @Override
            public void acceptUserProfile(App4ItUserProfile userProfile, FirebaseError error) {

                putWaitASecVeilOff();

                if(error != null) {
                    //bad luck. error.
                    showStickyErrorMessage("Failed downloading :-(");
                    Toast.makeText(TheirProfileActivity.this,"Failed to download the profile :-(",Toast.LENGTH_SHORT).show();
                } else if (userProfile != null) {
                    //there is a profile saved!

                    //do picture
                    if(userProfile.getPicture() != null) {
                        getTheirProfilePictureView().setImageBitmap(userProfile.getPicture());
                    } else {
                        getTheirProfilePictureView().setImageResource(R.drawable.nophotoheredessert);
                    }

                    //do name
                    if(userProfile.getName() != null && !userProfile.getName().trim().equals("")) {
                        setTextUnderPictureWithName(userProfile.getName());
                    } else {
                        setTextUnderPicture("The user didn't save a profile name :-(");
                    }



                } else {
                    //there's just no profile saved
                    getTheirProfilePictureView().setImageResource(R.drawable.nophotoheredessert);
                    setTextUnderPicture("And no name saved either :-(");
                }
            }
        });
    }

    private void setTextUnderPictureWithName(String name) {
        getTheirProfileTextUnderPicture().setText(Html.fromHtml("On BeApp4It known as " + "<font color=\"red\">" + name + "</font>"));
    }

    private void setTextUnderPicture(String text) {
        getTheirProfileTextUnderPicture().setText(text);
    }

    private void showStickyErrorMessage(String errorText) {
        ImageView imageView = getTheirProfilePictureView();
        TextView errorTextView = getTheirProfileErrorTextView();

        imageView.setVisibility(View.GONE);
        errorTextView.setVisibility(View.VISIBLE);
        errorTextView.setText(errorText);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getDelegate().activityStops();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDelegate().activityStarts(null);
    }

    private ImageView getTheirProfilePictureView() {
        return (ImageView)findViewById(R.id.theirProfilePicture);
    }

    private TextView getTheirProfileErrorTextView() {
        return (TextView)findViewById(R.id.theirProfileErrorText);
    }

    private TextView getTheirProfileTextUnderPicture() {
        return (TextView)findViewById(R.id.theirProfileTextUnderPicture);
    }

    private void putWaitASecVeilOn() {
        findViewById(R.id.theirProfileLoadingNotice).setVisibility(View.VISIBLE);
    }

    private void putWaitASecVeilOff() {
        findViewById(R.id.theirProfileLoadingNotice).setVisibility(View.GONE);
    }
}
