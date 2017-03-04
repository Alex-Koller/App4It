package com.dreambig.app4it.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.dreambig.app4it.App4ItApplication;
import com.dreambig.app4it.InviteActivity;
import com.dreambig.app4it.MyProfileActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.api.FirebaseTransactionCallback;
import com.dreambig.app4it.api.NewsCenter;
import com.dreambig.app4it.entity.App4ItInvitationItem;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.enums.InvitationStatus;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.impl.App4ItUserProfileManager;
import com.dreambig.app4it.impl.NewsCenterImpl;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;

public class InvitationsAdapter extends BaseAdapter {
	

    private InviteActivity contextActivity;
    private String activityId, activityOwnerId, activityTitle;
    private List<App4ItInvitationItem> invitationItemList = new ArrayList<>();

    private String loggedInUserId;


	public InvitationsAdapter(InviteActivity contextActivity, Bundle bundle) {
        this.contextActivity = contextActivity;

        this.activityId = bundle.getString(MessageIdentifiers.ACTIVITY_ID);
        this.activityOwnerId = bundle.getString(MessageIdentifiers.ACTIVITY_OWNER_ID);
        this.activityTitle = bundle.getString(MessageIdentifiers.ACTIVITY_TITLE);

        App4ItApplication delegate = getDelegate();
        this.loggedInUserId = delegate.getLoggedInUserId();
	}

    private App4ItApplication getDelegate() {
        return (App4ItApplication)contextActivity.getApplication();
    }

    public void setDataToDisplay(List<App4ItInvitationItem> invitationItemList) {
        this.invitationItemList = invitationItemList;
    }
	
	@Override
	public int getCount() {
        return invitationItemList.size() ;
	}

	@Override
	public Object getItem(int position) {
        return invitationItemList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

    private void fillInCell(final View view, App4ItInvitationItem invitationItem) {
        final App4ItUser app4ItUser = invitationItem.getUser();
        final App4ItUserProfile userProfile = App4ItUserProfileManager.getUserProfile(contextActivity,app4ItUser);
        view.setTag(app4ItUser);

        //do image
        ImageView imageView = (ImageView)view.findViewById(R.id.invitation_row_picture);
        imageView.setImageBitmap(userProfile.getPicture());

        //do name
        TextView textView = (TextView)view.findViewById(R.id.invitation_row_name);
        textView.setText(userProfile.getName());

        //do button
        Button button = (Button)view.findViewById(R.id.btnInvitationAction);

        InvitationStatus invitationStatus = invitationItem.getStatus();
        if(invitationStatus == null) {
            button.setText(R.string.invite);
            button.setEnabled(true);
            button.setBackgroundResource(R.drawable.invitation_button_invite);
        } else {
            if (invitationStatus.equals(InvitationStatus.GOING)) {
                button.setText(R.string.going);
                button.setBackgroundResource(R.drawable.invitation_button_going);
            } else if (invitationStatus.equals(InvitationStatus.NOT_GOING) || invitationStatus.equals(InvitationStatus.DELETED)) {
                button.setText(R.string.notGoing);
                button.setBackgroundResource(R.drawable.invitation_button_notgoing);
            }  else if (invitationStatus.equals(InvitationStatus.INVITED)) {
                button.setText(R.string.invited);
                button.setBackgroundResource(R.drawable.invitation_button_invited);
            }
            button.setEnabled(false);
        }

        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View view) {
                //this can only mean we are inviting the person
                final App4ItUser app4ItUser = (App4ItUser)((View)view.getParent()).getTag();
                final FirebaseGateway firebaseGateway = new FirebaseGateway(contextActivity);
                firebaseGateway.inviteUserToActivity(activityId,app4ItUser.getUserId(),app4ItUser.getNumber(), new FirebaseTransactionCallback() {
                    @Override
                    public void transactionEnded(FirebaseError firebaseError, boolean committed, DataSnapshot snapshot) {

                        if(firebaseError != null) {
                            //L og.e("InvitationsAdapter","Error whilst inviting user. " + firebaseError.getMessage());
                            UIHelper.showLongMessage(contextActivity, "Sending the invitation failed... :-( - " + firebaseError.getMessage());
                            return;
                        }

                        //whether committed or not we have to change the button title to what it is now
                        InvitationStatus status = InvitationStatus.valueOf((String) snapshot.child("status").getValue());
                        updateStatusInInternalList(app4ItUser,status);
                        notifyDataSetChanged();

                        if(committed) {
                            //L og.d("InvitationsAdapter","Invitation committed");
                            //that means if the logged in user actually did invite the guest
                            firebaseGateway.addToUsersInvitedToBucket(activityId,app4ItUser.getUserId(),activityOwnerId,loggedInUserId);

                            //send them news about it
                            NewsCenter newsCenter = new NewsCenterImpl();
                            newsCenter.postNewsAboutBeingInvitedToActivity(contextActivity,activityId,activityTitle,loggedInUserId,app4ItUser.getUserId());
                        }

                        //tell mother invite activity so in case of a recreate it reflects the current state
                        contextActivity.addInvitation(app4ItUser,status);
                    }
                });
            }

        });

        //profile action
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //do effect
                view.setBackgroundColor(Color.LTGRAY);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setBackgroundColor(Color.parseColor("#ffffff")); //back to normal
                    }
                },30);

                if(app4ItUser.getUserId().equals(loggedInUserId)) {
                    Intent intent = new Intent(contextActivity, MyProfileActivity.class);
                    contextActivity.startActivity(intent);
                } else {
                    UIHelper.showTheirProfileDialogFragment(contextActivity,app4ItUser.getUserId(),userProfile.getName());
                }

            }
        });
    }

    private void updateStatusInInternalList(App4ItUser user, InvitationStatus status) {
        for(App4ItInvitationItem invitationItem : invitationItemList) {
            if(invitationItem.getUser().equals(user)) {
                invitationItem.setStatus(status);
                break;
            }
        }
    }

    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
        View view = provideView(convertView, parent);
        fillInCell(view, invitationItemList.get(position));
        return view;
	}

    private View provideView(View convertView, ViewGroup parent) {
        View view;
        if(convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = LayoutInflater.from(contextActivity);
            view = inflater.inflate(R.layout.invitation_row, parent, false);
            ((Button)view.findViewById(R.id.btnInvitationAction)).setTypeface(UIHelper.getOrCreateOurFont(contextActivity), Typeface.BOLD);
        }
        return view;
    }

}

