package com.dreambig.app4it.fragment;

import android.app.ListFragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dreambig.app4it.InviteActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.adapter.InvitationsAdapter;
import com.dreambig.app4it.entity.App4ItInvitationItem;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.enums.InvitationStatus;

import java.util.List;
import java.util.Map;

public class InvitationListFragment extends ListFragment {

    private InvitationsAdapter adapter;
    private List<App4ItInvitationItem> invitationItemList;

    @Override
    public void onStart() {
        super.onStart();

        setEmptyText(Html.fromHtml("<font color='#C8C8C8'><b>" + getResources().getString(R.string.no_users_notice) + "</b></font>"));

    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {    		   
        adapter = new InvitationsAdapter((InviteActivity)getActivity(),getArguments());
        setListAdapter(adapter);
        if(invitationItemList != null) {
            adapter.setDataToDisplay(invitationItemList);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void setContent(List<App4ItInvitationItem> invitationItemList) {
        if(adapter != null) {
            adapter.setDataToDisplay(invitationItemList);
            adapter.notifyDataSetChanged();
        }

        this.invitationItemList = invitationItemList;
    }

}
