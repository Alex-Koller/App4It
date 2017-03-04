package com.dreambig.app4it.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dreambig.app4it.ContactsActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserCandidate;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.helper.UIHelper;
import com.dreambig.app4it.impl.App4ItUserProfileManager;

public class ContactsAdapter extends BaseAdapter {
	
	private ContactsActivity context;
	private List<App4ItUser> data = new ArrayList<>();
    private List<App4ItUserCandidate> dataCandidates = new ArrayList<>();

	public ContactsAdapter(ContactsActivity context) {
		this.context = context;
	}

    public void setData(List<App4ItUser> data, List<App4ItUserCandidate> dataCandidates) {
        this.data = data;
        this.dataCandidates = dataCandidates;
        notifyDataSetChanged();
    }
	
	@Override
	public int getCount() {
		return data.size() + dataCandidates.size();
	}

	@Override
	public Object getItem(int position) {
        if(position < data.size()) return data.get(position);
        else return dataCandidates.get(position - data.size());
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if(convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.contact_row, parent, false);
            ((Button)view.findViewById(R.id.btnContactStatus)).setTypeface(UIHelper.getOrCreateOurFont(context), Typeface.BOLD);
        }

        //shape the cell
        if(position < data.size()) {
            makeAUserCell(view, data.get(position));
        } else {
            makeACandidateCell(view, dataCandidates.get(position - data.size()));
        }

		return view;
		
	}

    private void makeAUserCell(final View view, final App4ItUser user) {
        //picture
        ImageView imageView = (ImageView)view.findViewById(R.id.contact_picture);
        imageView.setVisibility(View.VISIBLE);
        App4ItUserProfile userProfile = App4ItUserProfileManager.getUserProfileNoCreate(user.getUserId());
        if(userProfile != null && userProfile.getPicture() != null && userProfile.isPictureReal()) {
            imageView.setImageBitmap(userProfile.getPicture());
        } else {
            imageView.setImageBitmap(UIHelper.getNoImageAvailable(context));
        }


        //name
        TextView textView = (TextView)view.findViewById(R.id.contact_row_name);
        textView.setText(user.getName());

        //button
        Button button = (Button)view.findViewById(R.id.btnContactStatus);
        button.setText(R.string.contact_is_on_beapp4it);
        button.setBackgroundResource(R.drawable.contact_status_button);
        button.setEnabled(false);

        //action
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //do effect
                view.setBackgroundColor(Color.LTGRAY);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setBackgroundColor(Color.parseColor("#00efefef")); //back to normal
                    }
                },30);
                UIHelper.showTheirProfileDialogFragment(context,user.getUserId(),user.getName());
            }
        });
    }

    private void makeACandidateCell(View view, final App4ItUserCandidate candidate) {
        ImageView imageView = (ImageView)view.findViewById(R.id.contact_picture);
        imageView.setVisibility(View.GONE);

        TextView textView = (TextView)view.findViewById(R.id.contact_row_name);
        textView.setText(candidate.getName());

        Button button = (Button)view.findViewById(R.id.btnContactStatus);
        if(candidate.isInvited()) {
            button.setText(R.string.sms_sent);
            button.setBackgroundResource(R.drawable.contact_invited_button);
        } else {
            button.setText(R.string.invite_via_sms);
            button.setBackgroundResource(R.drawable.contact_status_button);
        }
        button.setEnabled(true);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                smsIntent.setData(Uri.parse("smsto:"));
                smsIntent.setType("vnd.android-dir/mms-sms");
                smsIntent.putExtra("address", candidate.getNumber());
                smsIntent.putExtra("sms_body", getTextMessage(candidate.getName()));

                try {
                    context.goingToShare();
                    context.startActivity(smsIntent);
                    candidate.setInvited(true);
                }
                catch (android.content.ActivityNotFoundException ex) {
                    UIHelper.showLongMessage(context,"SMS failed sending");
                }
            }
        });

        //no view action
        view.setOnClickListener(null);
    }

    private String getTextMessage(String name) {
        String firstNameOnly = name.split(" ")[0];
        return "Hi " + firstNameOnly + ". Join me in using BeApp4It. It's a great app for sharing ideas for events and activities. It's here on Apple App Store: http://itunes.com/apps/beapp4it. And here on Google Play: http://play.google.com/store/apps/details?id=com.dreambig.app4it. Or look at the website www.beapp4it.com.";
    }

}

