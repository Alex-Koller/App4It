package com.dreambig.app4it.fragment;

import android.app.ListFragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dreambig.app4it.ContactsActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.adapter.ContactsAdapter;

public class ContactsListFragment extends ListFragment {

    private ContactsAdapter contactsAdapter;

    @Override
    public void onStart() {
        super.onStart();

        setEmptyText(Html.fromHtml("<font color='#C8C8C8'><b>" + getResources().getString(R.string.no_users_notice) + "</b></font>"));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        contactsAdapter = new ContactsAdapter((ContactsActivity)getActivity());
        setListAdapter(contactsAdapter);
 
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ContactsActivity)getActivity()).giveMeContacts(contactsAdapter);
    }

}
