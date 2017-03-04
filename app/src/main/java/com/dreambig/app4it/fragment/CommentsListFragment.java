package com.dreambig.app4it.fragment;

import android.app.ListFragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.dreambig.app4it.CommentsActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.adapter.CommentsAdapter;

public class CommentsListFragment extends ListFragment {
	
	private CommentsAdapter adapter;
	
	@Override
	public void onStart() {
		super.onStart();
		
		setEmptyText(Html.fromHtml("<font color='#C8C8C8'><b>" + getResources().getString(R.string.no_comments) + "</b></font>"));
			
	}
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        adapter = new CommentsAdapter((CommentsActivity)getActivity());
        setListAdapter(adapter);
                
        View ret = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView)ret.findViewById(android.R.id.list);
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setStackFromBottom(true);
        listView.setDivider(null);
        
        return ret;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((CommentsActivity)getActivity()).startFeedingCommentsFromFirebase(adapter);
    }
    

}
