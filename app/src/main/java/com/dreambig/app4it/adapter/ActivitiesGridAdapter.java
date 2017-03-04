package com.dreambig.app4it.adapter;


import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dreambig.app4it.InviteActivity;
import com.dreambig.app4it.MyProfileActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.TheirProfileActivity;
import com.dreambig.app4it.entity.App4ItActivity;
import com.dreambig.app4it.entity.App4ItInvitationItem;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.enums.InvitationStatus;
import com.dreambig.app4it.fragment.TheirProfileDialogFragment;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.impl.App4ItUserProfileManager;
import com.dreambig.app4it.util.MessageIdentifiers;

import java.util.ArrayList;


public class ActivitiesGridAdapter extends BaseAdapter {
	
    private Activity context;
    private String homeUserId;
    private App4ItActivity activity;


    public ActivitiesGridAdapter(final Activity context, String homeUserId, final App4ItActivity activity) {
    	super();

        this.context = context;
    	this.homeUserId = homeUserId;
        this.activity = activity;
    }

    public int getCount() {
        int cellsOnRow = estimateHowManyOnRow();
        //just being defensive
        if(cellsOnRow == 0) {
            cellsOnRow = 3;
        }

        int numberOfRealCells = activity.getInvitationList().size();
        int numberOfCellsAtTheTailOfLastRow = cellsOnRow - (numberOfRealCells % cellsOnRow);

        int totalCells = numberOfRealCells + numberOfCellsAtTheTailOfLastRow + cellsOnRow;

        if(totalCells / cellsOnRow == 2) {
            totalCells = totalCells + cellsOnRow; //we don't want two rows...
        }

        return totalCells;
    }

    private int estimateHowManyOnRow() {
        float amountOfDpLeft = UIHelper.getWidthInDp(context);
        amountOfDpLeft = amountOfDpLeft - 100; //that's minus the action buttons width
        amountOfDpLeft = amountOfDpLeft - 20; //that's the side margins

        return (int)Math.floor(amountOfDpLeft / 53);
    }

    public Object getItem(int position) {
        return activity.getUsersGoing().get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View viewToUse;
        if(convertView != null) {
            viewToUse = convertView;
        } else {
            LayoutInflater inflater = LayoutInflater.from(context);
            viewToUse = inflater.inflate(R.layout.grid_cell, parent, false);
        }

        if(position < activity.getInvitationList().size()) {
            fillInCell(viewToUse, activity.getInvitationList().get(position));
        } else {
            fillInCellAsPlaceholder(viewToUse);
        }

        return viewToUse;
    }

    private void fillInCellAsPlaceholder(View view) {
        final ImageView imageView = (ImageView) view.findViewById(R.id.gridCellPicture);
        final View containingView = view.findViewById(R.id.gridCellBackView);
        TextView textView = (TextView) view.findViewById(R.id.gridCellText);

        imageView.setImageBitmap(UIHelper.getGridCellPlaceholderImage(context));
        containingView.setBackgroundResource(R.drawable.grid_cell_background_placeholder);
        textView.setVisibility(View.INVISIBLE);

        //give it action
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //do effect
                containingView.setAlpha(0.0f);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        containingView.setAlpha(1.0f);
                    }
                },20);

                //and go to invitations
                Intent intent = new Intent(context, InviteActivity.class);
                intent.putExtra(MessageIdentifiers.ACTIVITY_ID, activity.getActivityId());
                intent.putExtra(MessageIdentifiers.ACTIVITY_TITLE, activity.getTitle());
                intent.putExtra(MessageIdentifiers.ACTIVITY_OWNER_ID, activity.getCreatedByUserId());
                intent.putParcelableArrayListExtra(MessageIdentifiers.INVITATION_LIST,(ArrayList<? extends Parcelable>)activity.getInvitationList());
                context.startActivity(intent);
            }
        });
    }

    private void fillInCell(View view, final App4ItInvitationItem invitationItem) {
        ImageView imageView = (ImageView) view.findViewById(R.id.gridCellPicture);
        final View containingView = view.findViewById(R.id.gridCellBackView);
        TextView textView = (TextView) view.findViewById(R.id.gridCellText);

        final App4ItUserProfile userProfile = App4ItUserProfileManager.getUserProfile(context, invitationItem.getUser());

        //get the name right is needed
        if(userProfile.isPictureReal()) {
            textView.setVisibility(View.INVISIBLE);
        } else {
            textView.setVisibility(View.VISIBLE);
            textView.setText(userProfile.getName());
        }

        //image
        imageView.setImageBitmap(userProfile.getPicture());

        //get the background right
        if(invitationItem.getStatus().equals(InvitationStatus.GOING)) {
            containingView.setBackgroundResource(R.drawable.grid_cell_background_going);
        } else if (invitationItem.getStatus().equals(InvitationStatus.INVITED)) {
            containingView.setBackgroundResource(R.drawable.grid_cell_background_invited);
        } else {
            containingView.setBackgroundResource(R.drawable.grid_cell_background_notgoing);
        }

        //give it action
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //do effect
                containingView.setAlpha(0.3f);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        containingView.setAlpha(1.0f);
                    }
                },30);

                //go profile
                if(invitationItem.getUser().getUserId().equals(homeUserId)) {
                    Intent intent = new Intent(context, MyProfileActivity.class);
                    context.startActivity(intent);
                } else {
                    UIHelper.showTheirProfileDialogFragment(context,invitationItem.getUser().getUserId(),userProfile.getName());
                }
            }
        });
    }

}
