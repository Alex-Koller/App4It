package com.dreambig.app4it.customwidget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.dreambig.app4it.InviteActivity;
import com.dreambig.app4it.MyProfileActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.entity.App4ItActivity;
import com.dreambig.app4it.entity.App4ItInvitationItem;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.enums.InvitationStatus;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.impl.App4ItUserProfileManager;
import com.dreambig.app4it.util.MessageIdentifiers;

import java.util.ArrayList;

/**
 * Created by Alexandr on 25/11/2015.
 */
public class PeopleHoldingTableLayout extends TableLayout {

    private static int[]marginsVector;

    public int getNumberOfCells(App4ItActivity activity, int numberOfColumnsVisible) {
        int numberOfRealCells = activity.getInvitationList().size();
        int numberOfRealCellsEvenized = numberOfRealCells + (numberOfRealCells % 2);
        int minNumberOfColumns = numberOfRealCellsEvenized / 2;
        if(minNumberOfColumns < numberOfColumnsVisible) {
            minNumberOfColumns = numberOfColumnsVisible;
        }

        int numberOfColumnsToDraw = minNumberOfColumns + 2 - (numberOfRealCells % 2);
        return numberOfColumnsToDraw * 2;
    }

    private int howManyColumnsVisible() {
        float amountOfDpLeft = UIHelper.getWidthInDp(getContext());
        amountOfDpLeft = amountOfDpLeft - 100; //that's minus the action buttons width
        amountOfDpLeft = amountOfDpLeft - 20; //that's the side margins

        return (int)Math.floor(amountOfDpLeft / 53);
    }

    public PeopleHoldingTableLayout(Context context) {
        super(context);
    }

    public PeopleHoldingTableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void constructTheTable(App4ItActivity activity, String homeUserId, Activity contextActivity) {
        //clear all existing stuff
        removeAllViewsInLayout();

        int nVisibleColumns = howManyColumnsVisible();
        int nCellsTotal = getNumberOfCells(activity, nVisibleColumns);

        addView(createRow(0,nCellsTotal,activity,homeUserId,contextActivity),0);
        addView(createRow(1,nCellsTotal,activity,homeUserId,contextActivity),1);
    }

    private TableRow createRow(int index, int cellsInTotal, App4ItActivity activity, String homeUserId, Activity contextActivity) {

        TableRow row = new TableRow(getContext());

        for(int i = 0;i < cellsInTotal;i++) {
            if(i % 2 == index) {
                View cellView = getTheCellView(i, row, activity, homeUserId, contextActivity);
                modifyTheCellLayoutParameters(cellView,index == 1,index == 0, inLastColumn(cellsInTotal, i)); //last cell on row gets extra right margin to avoid the fading view
                row.addView(cellView);
            }
        }

        return row;
    }

    private boolean inLastColumn(int cellsInTotal, int i) {
        return i == cellsInTotal - 1 || i == cellsInTotal - 2;
    }

    private void modifyTheCellLayoutParameters(View cellView, boolean topMargin, boolean bottomMargin, boolean extraRightMargin) {
        TableRow.LayoutParams lp = (TableRow.LayoutParams)cellView.getLayoutParams();
        int[] marginsVector = getMarginsVector();
        lp.setMargins(marginsVector[0],topMargin ? marginsVector[1] : 0,marginsVector[2] + (extraRightMargin ? marginsVector[4] : 0), bottomMargin ? marginsVector[3] : 0);
        cellView.setLayoutParams(lp);
    }

    private int[] getMarginsVector() {
       if(marginsVector == null) {
           //left, top, right, bottom, extra right margin
           marginsVector = new int[] {UIHelper.convertDpToPixels(2),UIHelper.convertDpToPixels(3),UIHelper.convertDpToPixels(2),UIHelper.convertDpToPixels(3),UIHelper.convertDpToPixels(10)};
       }

        return marginsVector;
    }

    private View getTheCellView(int cellIndex, TableRow rowView, App4ItActivity activity, String homeUserId, Activity contextActivity) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View cellView = inflater.inflate(R.layout.grid_cell, rowView, false);

        if(cellIndex < activity.getInvitationList().size()) {
            fillInCell(cellView, activity.getInvitationList().get(cellIndex), homeUserId, contextActivity);
        } else {
            fillInCellAsPlaceholder(cellView, activity);
        }

        return cellView;
    }

    private void fillInCellAsPlaceholder(View view, final App4ItActivity activity) {
        final ImageView imageView = (ImageView) view.findViewById(R.id.gridCellPicture);
        final View containingView = view.findViewById(R.id.gridCellBackView);
        TextView textView = (TextView) view.findViewById(R.id.gridCellText);

        imageView.setImageBitmap(UIHelper.getGridCellPlaceholderImage(getContext()));
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
                Intent intent = new Intent(getContext(), InviteActivity.class);
                intent.putExtra(MessageIdentifiers.ACTIVITY_ID, activity.getActivityId());
                intent.putExtra(MessageIdentifiers.ACTIVITY_TITLE, activity.getTitle());
                intent.putExtra(MessageIdentifiers.ACTIVITY_OWNER_ID, activity.getCreatedByUserId());
                intent.putParcelableArrayListExtra(MessageIdentifiers.INVITATION_LIST,(ArrayList<? extends Parcelable>)activity.getInvitationList());
                getContext().startActivity(intent);
            }
        });
    }

    private void fillInCell(View view, final App4ItInvitationItem invitationItem, final String homeUserId, final Activity contextActivity) {
        ImageView imageView = (ImageView) view.findViewById(R.id.gridCellPicture);
        final View containingView = view.findViewById(R.id.gridCellBackView);
        TextView textView = (TextView) view.findViewById(R.id.gridCellText);

        final App4ItUserProfile userProfile = App4ItUserProfileManager.getUserProfile(getContext(), invitationItem.getUser());

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
                    Intent intent = new Intent(getContext(), MyProfileActivity.class);
                    getContext().startActivity(intent);
                } else {
                    UIHelper.showTheirProfileDialogFragment(contextActivity,invitationItem.getUser().getUserId(),userProfile.getName());
                }
            }
        });
    }

}
