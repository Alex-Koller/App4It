package com.dreambig.app4it.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.dreambig.app4it.R;
import com.dreambig.app4it.entity.App4ItUser;

public class NewActivityInvitationsAdapter extends BaseAdapter {
	
	private Context context;
	private List<App4ItUser> usersList = new ArrayList<>();
	private Set<App4ItUser> checkedOnes;


	public NewActivityInvitationsAdapter(Context context, Set<App4ItUser> checkedOnes) {
		this.context = context;
        this.checkedOnes = checkedOnes;
	}

    //data manipulation start
    public void setUsersList(List<App4ItUser> usersList) {
        Collections.sort(usersList, new Comparator<App4ItUser>() {
            @Override
            public int compare(App4ItUser lhs, App4ItUser rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        this.usersList = usersList;
        notifyDataSetChanged();
    }

    public Set<App4ItUser> getCheckedOnes() {
        return checkedOnes;
    }

    //data manipulation end
	
	@Override
	public int getCount() {
		return usersList.size();
	}

	@Override
	public Object getItem(int position) {
		return usersList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final App4ItUser app4ItUser = usersList.get(position);
		String userName = app4ItUser.getName();

        View view;
        if(convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.new_activity_invitation_row, parent, false);
        }

		TextView textView = (TextView)view.findViewById(R.id.new_activity_invitation_row_name);
		textView.setText(userName);		
		
		CheckBox checkbox = (CheckBox)view.findViewById(R.id.new_activity_invitation_row_checkbox);
		checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton view, boolean isChecked) {
				if(isChecked) {
					checkedOnes.add(app4ItUser);
				} else {
					checkedOnes.remove(app4ItUser);
				}
				
			}});

        if(checkedOnes.contains(app4ItUser)) {
            checkbox.setChecked(true); //note that this triggers the onCheckedChange listener. but the checkedOnes is a set so all good
        } else {
            checkbox.setChecked(false); //same as above
        }

        giveItBackGroundColour(view, position);
		
		return view;		
	}

    private void giveItBackGroundColour(View view, int position) {
        if(position % 2 == 1) view.setBackgroundColor(Color.rgb(240,240,240));
        else view.setBackgroundColor(Color.rgb(255, 255, 255));
    }

}

