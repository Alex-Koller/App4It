package com.dreambig.app4it.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ListFragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dreambig.app4it.NewActivityActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.adapter.NewActivityInvitationsAdapter;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.util.MessageIdentifiers;

public class NewActivityInvitationListFragment extends ListFragment {


    @Override
    public void onStart() {
        super.onStart();

        setEmptyText(Html.fromHtml("<font color='#C8C8C8'><b>" + getResources().getString(R.string.no_users_to_invite_to_new_event) + "</b></font>"));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {    		   
    	NewActivityInvitationsAdapter adapter = new NewActivityInvitationsAdapter(getActivity(),retrieveCheckedUsers(savedInstanceState));
        setListAdapter(adapter); 
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private Set<App4ItUser> retrieveCheckedUsers(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            List<App4ItUser> checkedUsers = savedInstanceState.getParcelableArrayList(MessageIdentifiers.INVITATION_LIST);
            if(checkedUsers != null) {
                return new HashSet<>(checkedUsers);
            }
        }

        return new HashSet<>();

    }

    public Collection<App4ItUser> getCheckedOnes() {
        return ((NewActivityInvitationsAdapter)getListAdapter()).getCheckedOnes();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((NewActivityActivity)getActivity()).giveMeContacts((NewActivityInvitationsAdapter)getListAdapter());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        NewActivityInvitationsAdapter adapter = (NewActivityInvitationsAdapter)getListAdapter();
        if(adapter != null) {
            outState.putParcelableArrayList(MessageIdentifiers.INVITATION_LIST, new ArrayList<>(adapter.getCheckedOnes()));
        }
    }



}
