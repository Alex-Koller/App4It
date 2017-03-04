package com.dreambig.app4it.fragment;

import android.app.ListFragment;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.dreambig.app4it.App4ItApplication;
import com.dreambig.app4it.HomeActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.adapter.ActivitiesAdapter;
import com.dreambig.app4it.api.App4ItActivityManager;
import com.dreambig.app4it.api.SuccessOrFailureCallback;
import com.dreambig.app4it.entity.App4ItActivity;
import com.dreambig.app4it.entity.App4ItActivityParcel;
import com.dreambig.app4it.helper.SystemHelper;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.impl.App4ItActivityManagerImpl;
import com.dreambig.app4it.repository.FirebaseGateway;

public class ActivitiesListFragment extends ListFragment {
	
	private ActivitiesAdapter adapter = null;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        //L og.d("ActivitiesListFragment", "Activities list fragment is creating view");
    	try {
    		adapter = new ActivitiesAdapter((HomeActivity)getActivity());
    		setListAdapter(adapter);
    	} catch (Exception e) {
    		UIHelper.showLongMessage(getActivity(), "Couldn't proceed - " + e.getMessage());
    	}
        View ret = inflater.inflate(R.layout.custom_activities_list_content,container, false);
        doTouchesOnTheEmptyView(ret);
        return ret;
    }

    private void doTouchesOnTheEmptyView(View view) {
        TextView stepOne = (TextView)view.findViewById(R.id.guide_step_one);
        //stepOne.setTypeface(UIHelper.getOrCreateOurFont(getActivity()), Typeface.BOLD);
        stepOne.setText(Html.fromHtml("<font color='#b8b8b8'>1.</font> There's <font color='#181818'>something you want to do</font> or somewhere you want to go"));

        TextView stepTwo = (TextView)view.findViewById(R.id.guide_step_two);
        //stepTwo.setTypeface(UIHelper.getOrCreateOurFont(getActivity()), Typeface.BOLD);
        stepTwo.setText(Html.fromHtml("<font color='#b8b8b8'>2.</font> You <font color='#181818'>create an event</font> and invite whoever you think may want to join"));

        TextView stepThree = (TextView)view.findViewById(R.id.guide_step_three);
        //stepThree.setTypeface(UIHelper.getOrCreateOurFont(getActivity()), Typeface.BOLD);
        stepThree.setText(Html.fromHtml("<font color='#b8b8b8'>3.</font> Then you relax and wait to<br></br><font color='#181818'>see who is App4It</font>"));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((HomeActivity)getActivity()).activitiesAdapterIsReady(adapter);
    }


    public void refreshNamesFromPhonebook() {
        if(adapter != null) {
            adapter.refreshNamesFromPhonebook();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final App4ItActivity selectedActivity = getSelectedActivity((ListView)v,menuInfo);
        menu.setHeaderTitle(selectedActivity.getTitle());

        if(selectedActivity.getCreatedByUserId().equals(getDelegate().getLoggedInUserId())) {

            menu.add(Menu.NONE,0,Menu.NONE,getResources().getString(R.string.delete_event_let_everybody_know)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    App4ItActivityManager activityManager = new App4ItActivityManagerImpl();
                    activityManager.deleteActivity(getActivity(),true,new App4ItActivityParcel(selectedActivity),getDelegate().getLoggedInUserId(),new SuccessOrFailureCallback() {
                        @Override
                        public void callback(boolean success, String errorMessage) {
                            if(!success) {
                                UIHelper.showBriefMessage(getActivity(), errorMessage);
                            } else {
                                UIHelper.showBriefMessage(getActivity(), selectedActivity.getTitle() + " " + getResources().getString(R.string.deleted).toLowerCase());
                            }
                        }
                    });
                    return false;
                }
            });
            menu.add(Menu.NONE,1,Menu.NONE,getResources().getString(R.string.delete_event_keep_silent)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    App4ItActivityManager activityManager = new App4ItActivityManagerImpl();
                    activityManager.deleteActivity(getActivity(),false,new App4ItActivityParcel(selectedActivity),getDelegate().getLoggedInUserId(),new SuccessOrFailureCallback() {
                        @Override
                        public void callback(boolean success, String errorMessage) {
                            if(!success) {
                                UIHelper.showBriefMessage(getActivity(), errorMessage);
                            } else {
                                UIHelper.showBriefMessage(getActivity(), selectedActivity.getTitle() + " " + getResources().getString(R.string.deleted).toLowerCase());
                            }
                        }
                    });
                    return false;
                }
            });

        } else {
            menu.add(Menu.NONE,2,Menu.NONE,getResources().getString(R.string.delete)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    FirebaseGateway firebaseGateway = new FirebaseGateway(getActivity());
                    String loggedInUserIdentifier = getDelegate().getLoggedInUserId();

                    firebaseGateway.setUserAsHavingDeletedTheInvitation(selectedActivity.getActivityId(),loggedInUserIdentifier);
                    firebaseGateway.removeFromUsersInvitedToBucket(selectedActivity.getActivityId(),loggedInUserIdentifier);
                    firebaseGateway.setNotificationPreference(selectedActivity.getActivityId(),loggedInUserIdentifier,"optOutComments","Y");
                    return false;
                }
            });
        }
    }

    private App4ItActivity getSelectedActivity(ListView listView, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        return (App4ItActivity) listView.getItemAtPosition(info.position);
    }

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getActivity().getApplication();
    }
}
