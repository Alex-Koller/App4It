package com.dreambig.app4it.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Parcelable;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.dreambig.app4it.App4ItApplication;
import com.dreambig.app4it.CommentsActivity;
import com.dreambig.app4it.EditActivityActivity;
import com.dreambig.app4it.HomeActivity;
import com.dreambig.app4it.InviteActivity;
import com.dreambig.app4it.MapReadActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.SuggestActivity;
import com.dreambig.app4it.customwidget.PeopleHoldingTableLayout;
import com.dreambig.app4it.entity.App4ItActivity;
import com.dreambig.app4it.entity.App4ItActivityParcel;
import com.dreambig.app4it.entity.App4ItInvitationItem;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.entity.FilterSettings;
import com.dreambig.app4it.enums.InvitationStatus;
import com.dreambig.app4it.enums.SuggestionType;
import com.dreambig.app4it.helper.SystemHelper;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.impl.App4ItUserProfileManager;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.MessageIdentifiers;

/**
 * The working assumption in this class is that everything runs in the same (UI) thread
 * So concurrency is not a worry
 */

public class ActivitiesAdapter extends BaseAdapter {

    private FilterSettings filterSettings;
	private HomeActivity contextActivity;
    private List<App4ItActivity> activitiesToDisplay = new ArrayList<>();
	private List<App4ItActivity> activities = new ArrayList<>();
	private Map<String,ActivitiesGridAdapter> gridAdapters = new HashMap<String,ActivitiesGridAdapter>();

	private String homeUserId;
	
	public ActivitiesAdapter(final HomeActivity contextActivity) throws Exception {
		super();
		this.contextActivity = contextActivity;

        App4ItApplication application = (App4ItApplication)contextActivity.getApplication();
        this.homeUserId = application.getLoggedInUserId();
        this.filterSettings = new FilterSettings(true,true,true,true,true,true,true,true,true,true); //this shouldn't be used, just in case as a default
	}

    public void setFilterSettings(FilterSettings filterSettings) {
        this.filterSettings = filterSettings;
    }

    public void refreshNamesFromPhonebook() {
        App4ItApplication delegate = getDelegate();

        for(App4ItActivity activity : activities) {
            activity.setCreatedByName(delegate.getNameFromPhoneNumber(activity.getCreatedByNumber()));

            if(activity.getInvitationList() != null) {
                for(App4ItInvitationItem invitationItem : activity.getInvitationList()) {
                    App4ItUser user = invitationItem.getUser();
                    user.setName(delegate.getNameFromPhoneNumber(user.getNumber()));
                }
            }

            if(activity.getUsersGoing() != null) {
                for(App4ItUser user : activity.getUsersGoing()) {
                    user.setName(delegate.getNameFromPhoneNumber(user.getNumber()));
                }
            }
        }

        reloadDisplay();
    }


    private App4ItActivity getActivityById(String activityId) {
        if(activities == null) return null;

        for(App4ItActivity activity : activities) {
            if(activity.getActivityId().equals(activityId)) {
                return activity;
            }
        }

        return null;
    }

    public void flatCountOnComments(String activityId) {
        App4ItActivity activity = getActivityById(activityId);
        if(activity != null) {
            activity.setUnseenCommentsRealtime(0);
            activity.setUnseenComments(0);

            reloadDisplay();
        }
    }

    public void addActivity(App4ItActivity activity) {
        activities.add(0,activity);
    }

    public void removeActivity(App4ItActivity activity) {
        activities.remove(activity);
    }

    public boolean containsActivity(App4ItActivity activity) {
        return activities.contains(activity);
    }

    public Context getContext() {
        return contextActivity;
    }

    public App4ItApplication getDelegate() {
        return (App4ItApplication)contextActivity.getApplication();
    }

    public void reloadDisplay() {
        activitiesToDisplay = filterSettings.filterActivities(homeUserId,activities);
        notifyDataSetChanged();
    }

    public List<App4ItActivity> getActivities() {
        return activities;
    }

    public void sortActivities() {
        List<App4ItActivity> temp = new ArrayList<>(activities);
        Collections.sort(temp,new Comparator<App4ItActivity>() {
            @Override
            public int compare(App4ItActivity lhs, App4ItActivity rhs) {
                return rhs.getCreatedOn().compareTo(lhs.getCreatedOn());
            }
        });

        activities = temp;
    }

    public void doneWithInitialLoad() {
        contextActivity.activitiesAdapterStopsLoading();
    }

    public void downloadedXoutOfTotal(int x, int total) {
        contextActivity.downloadedXoutOfTotal(x, total);
    }
	
	@Override
	public int getCount() {
		return activitiesToDisplay.size();
	}

	@Override
	public Object getItem(int position) {
		return activitiesToDisplay.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

    private View provideView(View convertView, ViewGroup parent) {
        View view;
        if(convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = LayoutInflater.from(contextActivity);
            view = SystemHelper.areWePre21() ? inflater.inflate(R.layout.activity_row_pre_loli, parent, false) : inflater.inflate(R.layout.activity_row_loli_onwards, parent, false);
            giveButtonsOurFont(view);
        }
        return view;
    }

    private void giveButtonsOurFont(View view) {

        styleUpFourButtons(((Button)view.findViewById(R.id.btnActivityApp4It)));
        styleUpFourButtons(((Button)view.findViewById(R.id.btnActivityNoThanks)));
        styleUpFourButtons(((Button)view.findViewById(R.id.btnActivityWhen)));
        styleUpFourButtons(((Button)view.findViewById(R.id.btnActivityWhere)));

    }

    private void styleUpFourButtons(Button button) {
        button.setTypeface(UIHelper.getOrCreateOurFont(contextActivity), Typeface.BOLD);
    }

    private void setTitleDetailsIcon(Controls controls, App4ItActivity app4ItActivity) {
        controls.title.setText(app4ItActivity.getTitle());
        controls.details.setText(getDetailsText(app4ItActivity));

        if (app4ItActivity.getType().equalsIgnoreCase("catch up")) {
            controls.icon.setImageResource(R.drawable.catchup);
        } else if(app4ItActivity.getType().equalsIgnoreCase("cultural")) {
            controls.icon.setImageResource(R.drawable.culture);
        } else if (app4ItActivity.getType().equalsIgnoreCase("night out")) {
            controls.icon.setImageResource(R.drawable.nightout);
        } else if(app4ItActivity.getType().equalsIgnoreCase("sport")) {
            controls.icon.setImageResource(R.drawable.sport);
        } else if (app4ItActivity.getType().equalsIgnoreCase("food and drink")) {
            controls.icon.setImageResource(R.drawable.foodanddrink);
        } else {
            //this is for undisclosed
            controls.icon.setImageResource(android.R.color.transparent);
        }
    }

    private void considerStates(final App4ItActivity app4ItActivity, Controls controls) {
        if(app4ItActivity.getLoggedInUserStatus() != null) {
            if(app4ItActivity.getLoggedInUserStatus().equals(InvitationStatus.GOING)) {
                controls.app4ItButton.setEnabled(false);
                controls.noThanksButton.setEnabled(true);
                controls.title.setPaintFlags(controls.title.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
            } else if (app4ItActivity.getLoggedInUserStatus().equals(InvitationStatus.NOT_GOING)) {
                controls.noThanksButton.setEnabled(false);
                controls.app4ItButton.setEnabled(true);
                controls.title.setPaintFlags(controls.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                controls.app4ItButton.setEnabled(true);
                controls.noThanksButton.setEnabled(true);
                controls.title.setPaintFlags(controls.title.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
            }
        }

        //enable disable 'edit' option depending on whether they own this activity or not
        if(app4ItActivity.getCreatedByUserId().equals(homeUserId)) {
            controls.edit.setVisibility(View.VISIBLE);
        } else {
            controls.edit.setVisibility(View.GONE);
        }

        //enable disable 'map' button depending on whether there's any location whatsoever
        if(app4ItActivity.getMapLocation() != null || (app4ItActivity.getWhereAsString() != null && !app4ItActivity.getWhereAsString().trim().equals(""))) {
            controls.map.setEnabled(true);
        } else {
            controls.map.setEnabled(false);
        }

        //enable disable 'more' based on description or not
        if("".equals(app4ItActivity.getMoreAbout().trim())) {
            controls.more.setEnabled(false);
        } else {
            controls.more.setEnabled(true);
        }

        //counter on comments
        if(app4ItActivity.getUnseenComments() > 0) {
            controls.comments.setText(Html.fromHtml("<b>Chat (" + app4ItActivity.getUnseenComments() + ")</b>"));
        } else {
            controls.comments.setText(Html.fromHtml("<b>Chat</b>"));
        }

        //add unseen numbers of when and where suggestions
        if(app4ItActivity.getUnseenWhenSuggestions() > 0) {
            controls.suggestWhenButton.setText(contextActivity.getResources().getString(R.string.say_when) + " (" + app4ItActivity.getUnseenWhenSuggestions() + ")");
        } else {
            controls.suggestWhenButton.setText(contextActivity.getResources().getString(R.string.say_when));
        }

        if(app4ItActivity.getUnseenWhereSuggestions() > 0) {
            controls.suggestWhereButton.setText(contextActivity.getResources().getString(R.string.say_where) + " (" + app4ItActivity.getUnseenWhereSuggestions() + ")");
        } else {
            controls.suggestWhereButton.setText(contextActivity.getResources().getString(R.string.say_where));
        }
    }

    private Controls collectControls(View view) {
        Controls ret = new Controls();
        ret.app4ItButton = ((Button)view.findViewById(R.id.btnActivityApp4It));
        ret.noThanksButton = ((Button)view.findViewById(R.id.btnActivityNoThanks));
        ret.suggestWhenButton = ((Button)view.findViewById(R.id.btnActivityWhen));
        ret.suggestWhereButton = ((Button)view.findViewById(R.id.btnActivityWhere));
        ret.title = (TextView)view.findViewById(R.id.activity_row_title);
        ret.more = (TextView)view.findViewById(R.id.activity_row_more);
        ret.invitations = (TextView)view.findViewById(R.id.activity_row_invitations);
        ret.comments = (TextView)view.findViewById(R.id.activity_row_comments);
        ret.map = (TextView)view.findViewById(R.id.activity_row_map);
        ret.edit = (TextView)view.findViewById(R.id.activity_row_edit);
        ret.details = (TextView)view.findViewById(R.id.activity_row_details);
        ret.icon = (ImageView)view.findViewById(R.id.activity_row_type_icon);
        return ret;
    }

    private void setTheGridOfPeopleGoing(App4ItActivity app4ItActivity, View view) {
        if(SystemHelper.areWePre21()) {
            PeopleHoldingTableLayout peopleHoldingTableLayout = (PeopleHoldingTableLayout)view.findViewById(R.id.activity_row_body_table);
            peopleHoldingTableLayout.constructTheTable(app4ItActivity, homeUserId, contextActivity);
        } else {
            GridView gridView = (GridView)view.findViewById(R.id.activity_row_body_grid);
            ActivitiesGridAdapter gridAdapter = gridAdapters.get(app4ItActivity.getActivityId());
            if(gridAdapter == null) {
                gridAdapter = new ActivitiesGridAdapter(contextActivity,homeUserId,app4ItActivity);
                gridAdapters.put(app4ItActivity.getActivityId(), gridAdapter);
            }
            gridView.setAdapter(gridAdapter);
        }
    }

    private void addActionsToButtons(final Controls controls, final App4ItActivity app4ItActivity) {
        controls.app4ItButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                FirebaseGateway firebaseGateway = new FirebaseGateway(contextActivity);
                firebaseGateway.setUserAsGoing(app4ItActivity.getActivityId(),homeUserId);
                firebaseGateway.setNotificationPreference(app4ItActivity.getActivityId(),homeUserId,"optOutComments","N");

                //take care of buttons. however this should happen by itself due to the list refresh
                controls.app4ItButton.setEnabled(false);
                controls.noThanksButton.setEnabled(true);
            }});

        controls.noThanksButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                FirebaseGateway firebaseGateway = new FirebaseGateway(contextActivity);
                firebaseGateway.setUserAsNotGoing(app4ItActivity.getActivityId(), homeUserId);
                firebaseGateway.setNotificationPreference(app4ItActivity.getActivityId(),homeUserId,"optOutComments","Y");

                //take care of buttons. however this should happen by itself due to the list refresh
                controls.noThanksButton.setEnabled(false);
                controls.app4ItButton.setEnabled(true);
            }});

        controls.suggestWhenButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                //zero the counter on the button. the display is redrawn in home.onstart
                app4ItActivity.setUnseenWhenSuggestions(0);
                app4ItActivity.setUnseenWhenSuggestionsRealtime(0);

                // let's open the new activity
                Intent intent = new Intent(contextActivity, SuggestActivity.class);
                intent.putExtra(MessageIdentifiers.SUGGEST_TYPE, SuggestionType.TIME.toString());
                intent.putExtra(MessageIdentifiers.ACTIVITY_PARCEL, new App4ItActivityParcel(app4ItActivity));
                contextActivity.startActivity(intent);
            }

        });

        controls.suggestWhereButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                //zero the counter on the button. the display is redrawn in home.onstart
                app4ItActivity.setUnseenWhereSuggestions(0);
                app4ItActivity.setUnseenWhereSuggestionsRealtime(0);

                // let's open the new activity
                Intent intent = new Intent(contextActivity, SuggestActivity.class);
                intent.putExtra(MessageIdentifiers.SUGGEST_TYPE, SuggestionType.PLACE.toString());
                intent.putExtra(MessageIdentifiers.ACTIVITY_PARCEL, new App4ItActivityParcel(app4ItActivity));
                contextActivity.startActivity(intent);
            }});
    }

    private void addActionsToTextViewControls(Controls controls, final App4ItActivity app4ItActivity) {
        controls.more.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                String textToShow;
                if(!"".equals(app4ItActivity.getMoreAbout().trim())) {
                    textToShow = app4ItActivity.getMoreAbout().trim();
                } else {
                    textToShow = contextActivity.getResources().getString(R.string.event_has_no_description);
                }

                //better dialog for More start
                AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);

                builder.setView(getMoreTextView(textToShow))
                        .setTitle("More about " + app4ItActivity.getTitle())
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //don't do anything. just the dialog will close
                            }
                        });

                builder.create().show();
                //better dialog for More end
            }});

        controls.invitations.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // let's open the new activity
                Intent intent = new Intent(contextActivity, InviteActivity.class);
                intent.putExtra(MessageIdentifiers.ACTIVITY_ID, app4ItActivity.getActivityId());
                intent.putExtra(MessageIdentifiers.ACTIVITY_TITLE, app4ItActivity.getTitle());
                intent.putExtra(MessageIdentifiers.ACTIVITY_OWNER_ID, app4ItActivity.getCreatedByUserId());
                intent.putParcelableArrayListExtra(MessageIdentifiers.INVITATION_LIST,(ArrayList<? extends Parcelable>)app4ItActivity.getInvitationList());
                contextActivity.startActivity(intent);
            }
        });

        controls.comments.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(contextActivity, CommentsActivity.class);
                intent.putExtra(MessageIdentifiers.ACTIVITY_ID, app4ItActivity.getActivityId());
                intent.putExtra(MessageIdentifiers.ACTIVITY_TITLE, app4ItActivity.getTitle());
                contextActivity.setActivityIdWhoseCommentsHaveBeenOpen(app4ItActivity.getActivityId()); //look at the field declaration for explanation
                contextActivity.startActivityForResult(intent,HomeActivity.OPEN_COMMENTS_ACTIVITY_REQUEST);
            }});

        controls.map.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(app4ItActivity.getMapLocation() != null) {
                    navigateToReadMap(app4ItActivity);
                } else {
                    performDialogAboutOpeningMapToRead(app4ItActivity);
                }
            }
        });

        controls.edit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // let's open activity for event editing
                Intent intent = new Intent(contextActivity, EditActivityActivity.class);
                intent.putExtra(MessageIdentifiers.ACTIVITY_PARCEL, new App4ItActivityParcel(app4ItActivity));
                contextActivity.startActivity(intent);
            }});
    }

    private void performDialogAboutOpeningMapToRead(final App4ItActivity app4ItActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextActivity);
        builder.setTitle(contextActivity.getResources().getString(R.string.no_map_location_saved_for_this_event));
        builder.setMessage(contextActivity.getResources().getString(R.string.do_you_want_to_look_for_this_on_the_map).replace("THIS_LOCATION",app4ItActivity.getWhereAsString()));

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                navigateToReadMap(app4ItActivity);
            }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //don't do anything
            }
        });
        // Create the AlertDialog object and show it
        builder.create().show();
    }

    private void navigateToReadMap(App4ItActivity app4ItActivity) {
        Intent intent = new Intent(contextActivity, MapReadActivity.class);
        intent.putExtra(MessageIdentifiers.ACTIVITY_MAP_LOCATION, app4ItActivity.getMapLocation());
        intent.putExtra(MessageIdentifiers.ACTIVITY_MAP_ADDRESS, app4ItActivity.getWhereAsString());
        contextActivity.startActivity(intent);
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//L og.d("ActivitiesAdapter", "View asked for index " + position + ", convertView is " + convertView);

		View view = provideView(convertView, parent);
        Controls controls = collectControls(view);
		final App4ItActivity app4ItActivity = activitiesToDisplay.get(position);

		setTitleDetailsIcon(controls, app4ItActivity);
        setTheGridOfPeopleGoing(app4ItActivity, view);
		
		addActionsToButtons(controls, app4ItActivity);
		considerStates(app4ItActivity,controls);
        addActionsToTextViewControls(controls, app4ItActivity);
		
		return view;		
	}

	
	private String getDetailsText(App4ItActivity activity) {
		StringBuilder ret = new StringBuilder("");

        //@todo note that the phone book may have changed since the time the activity was created
        if(activity.getCreatedByUserId().equals(homeUserId)) {
            ret.append("created by you");
        } else if(activity.getCreatedByName() != null && !activity.getCreatedByName().trim().equals("")) {
			ret.append("with ").append(activity.getCreatedByName().trim());
		} else {
            App4ItUserProfile userProfile = App4ItUserProfileManager.getUserProfileNoCreate(activity.getCreatedByUserId());
            if(userProfile != null && userProfile.getName() != null && !userProfile.getName().trim().equals("")) {
                ret.append("with ").append(userProfile.getName().trim());
            }
        }

		if(activity.getWhenAsString() != null && !activity.getWhenAsString().trim().equals("")) {
			if(ret.length() != 0) {
				ret.append(", ");
			}
			ret.append(activity.getWhenAsString().trim());
		}

		if(activity.getWhereAsString() != null && !activity.getWhereAsString().trim().equals("")) {
			if(ret.length() != 0) {
				ret.append(", ");
			}
			ret.append(activity.getWhereAsString().trim());
		}
				
		return ret.toString();
	}

    private TextView getMoreTextView(String content) {
        TextView showText = new TextView(contextActivity);

        showText.setAutoLinkMask(Linkify.ALL);
        showText.setPadding(50,10,50,5);
        showText.setText(content);
        showText.setTextIsSelectable(true);


        return showText;
    }

    private class Controls {
        Button app4ItButton;
        Button noThanksButton;
        Button suggestWhenButton;
        Button suggestWhereButton;
        TextView title;
        TextView more;
        TextView invitations;
        TextView comments;
        TextView map;
        TextView edit;
        TextView details;
        ImageView icon;
    }

}

