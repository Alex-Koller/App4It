package com.dreambig.app4it.fragment;

import android.app.DialogFragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dreambig.app4it.R;
import com.dreambig.app4it.api.FirebaseUserProfileCallback;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.firebase.client.FirebaseError;

/**
 * Created by Alexandr on 21/11/2015.
 */
public class TheirProfileDialogFragment extends DialogFragment {

    private String userId;
    private String userName;

    public static TheirProfileDialogFragment newInstance(String userId, String userName) {
        TheirProfileDialogFragment f = new TheirProfileDialogFragment();

        Bundle args = new Bundle();
        args.putString(MessageIdentifiers.USER_ID, userId);
        args.putString(MessageIdentifiers.USER_NAME, userName);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userId = getArguments().getString(MessageIdentifiers.USER_ID);
        userName = getArguments().getString(MessageIdentifiers.USER_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_their_profile, container, false);

        TextView textUnder = (TextView)v.findViewById(R.id.theirProfileFragmentTextUnderPicture);
        textUnder.setTypeface(UIHelper.getOrCreateOurFont(getActivity()), Typeface.BOLD);

        getDialog().setTitle(userName);

        return v;
    }

    private void prepareForLandingPicture() {
        getPictureView().setVisibility(View.VISIBLE);

        //getTopShadowView().setVisibility(View.VISIBLE);
        //getLeftShadowView().setVisibility(View.VISIBLE);
        getRightShadowView().setVisibility(View.VISIBLE);
        getBottomShadowView().setVisibility(View.VISIBLE);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        putWaitASecVeilOn();

        FirebaseGateway firebaseGateway = new FirebaseGateway(getActivity());
        firebaseGateway.downloadFullProfileForUserId(userId, new FirebaseUserProfileCallback() {
            @Override
            public void acceptUserProfile(App4ItUserProfile userProfile, FirebaseError error) {

                if(getView() == null) {
                    //just in case the view no longer exists
                    return;
                }

                putWaitASecVeilOff();

                if(error != null) {
                    //bad luck. error.
                    showStickyErrorMessage("Failed downloading the profile :-(");
                } else if (userProfile != null) {
                    //there is a profile saved!
                    prepareForLandingPicture();

                    //do picture
                    if(userProfile.getPicture() != null) {
                        getPictureView().setImageBitmap(userProfile.getPicture());
                    } else {
                        getPictureView().setImageResource(R.drawable.noprofilephotohere);
                    }

                    //do name
                    if(userProfile.getName() != null && !userProfile.getName().trim().equals("")) {
                        setTextUnderPictureWithName(userProfile.getName());
                    } else {
                        setTextUnderPicture("The user didn't save a profile name :-(");
                    }

                } else {
                    //there's just no profile saved
                    prepareForLandingPicture();
                    getPictureView().setImageResource(R.drawable.noprofilephotohere);
                    setTextUnderPicture("And no name saved either :-(");
                }
            }
        });
    }

    private void setTextUnderPictureWithName(String name) {
        getTextUnderPicture().setText(Html.fromHtml("On BeApp4It known as " + "<font color=\"red\">" + name + "</font>"));
    }

    private void setTextUnderPicture(String text) {
        getTextUnderPicture().setText(text);
    }

    private void showStickyErrorMessage(String errorText) {
        ImageView imageView = getPictureView();
        TextView errorTextView = getErrorTextView();

        getLeftShadowView().setVisibility(View.GONE);
        getTopShadowView().setVisibility(View.GONE);
        getRightShadowView().setVisibility(View.GONE);
        getBottomShadowView().setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        errorTextView.setVisibility(View.VISIBLE);
        errorTextView.setText(errorText);
    }

    private View getBottomShadowView() {
        return getView().findViewById(R.id.theirProfileFragmentShadowBottom);
    }

    private View getRightShadowView() {
        return getView().findViewById(R.id.theirProfileFragmentShadowRight);
    }

    private View getTopShadowView() {
        return getView().findViewById(R.id.theirProfileFragmentShadowTop);
    }

    private View getLeftShadowView() {
        return getView().findViewById(R.id.theirProfileFragmentShadowLeft);
    }

    private ImageView getPictureView() {
        return (ImageView)getView().findViewById(R.id.theirProfileFragmentPicture);
    }

    private TextView getErrorTextView() {
        return (TextView)getView().findViewById(R.id.theirProfileFragmentErrorText);
    }

    private TextView getTextUnderPicture() {
        return (TextView)getView().findViewById(R.id.theirProfileFragmentTextUnderPicture);
    }

    private void putWaitASecVeilOn() {
        if(getView() != null) {
            getView().findViewById(R.id.theirProfileFragmentLoadingNotice).setVisibility(View.VISIBLE);
        }
    }

    private void putWaitASecVeilOff() {
        getView().findViewById(R.id.theirProfileFragmentLoadingNotice).setVisibility(View.GONE);
    }
}
