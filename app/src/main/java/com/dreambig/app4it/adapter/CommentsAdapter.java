package com.dreambig.app4it.adapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dreambig.app4it.App4ItApplication;
import com.dreambig.app4it.CommentsActivity;
import com.dreambig.app4it.MyProfileActivity;
import com.dreambig.app4it.R;
import com.dreambig.app4it.entity.App4ItComment;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.impl.App4ItUserProfileManager;
import com.dreambig.app4it.util.DateUtil;
import com.dreambig.app4it.helper.UIHelper;

public class CommentsAdapter extends BaseAdapter {
	
	private CommentsActivity context;
    private String loggedInUserId;
    private Map<String,String> phoneBook;

	private List<App4ItComment> data = new ArrayList<App4ItComment>();

    public Context getContext() {
        return context;
    }

	public CommentsAdapter(CommentsActivity context) {
		this.context = context;
        this.loggedInUserId = getDelegate().getLoggedInUserId();
        this.phoneBook = getDelegate().getPhoneNumberToNameMap();
	}

    private App4ItApplication getDelegate() {
        return (App4ItApplication)context.getApplication();
    }

    //data manipulation methods start
    public List<App4ItComment> getComments() {
        return data;
    }

    public void addComment(App4ItComment comment) {
        //L og.d("CommentsAdapter", "Adding new comment to adapter");
        data.add(comment);
    }

    public boolean containsComment(App4ItComment comment) {
        return data.contains(comment);
    }
    //data manipulation methods end

    public void doneWithInitialLoad() {
        context.commentsAdapterStopsLoading();
    }

    public void reloadDisplay() {
        notifyDataSetChanged();
    }

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
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
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.comment_row, parent, false);
        }
        return view;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = provideView(convertView, parent);
        ImageView imageView = (ImageView)view.findViewById(R.id.comment_picture);
		TextView textView = (TextView)view.findViewById(R.id.comment_row_text);
		
		final App4ItComment comment = data.get(position);
        final App4ItUserProfile userProfile = App4ItUserProfileManager.getUserProfile(context,new App4ItUser(comment.getCreatedByName(),comment.getCreatedByNumber(),comment.getCreatedBy()));

        //do picture
        imageView.setImageBitmap(userProfile.getPicture());
        //do name
		String name;
		if(comment.getCreatedBy().equals(loggedInUserId)) {
			name = "You";
		} else {
			name = phoneBook.get(comment.getCreatedByNumber());
		}
		
		if(name == null) {
			//this would be the case if we don't have anyone on our phone under this number
			name = userProfile.getName(); //this can be profile name or number
		}
		
		textView.setText(Html.fromHtml("<b>" + name + "</b>: " + comment.getText()));
		
        giveItStatus(view,comment);
        decorateTheRow(view, position);

        //do action
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(comment.getCreatedBy().equals(loggedInUserId)) {
                    Intent intent = new Intent(context, MyProfileActivity.class);
                    context.startActivity(intent);
                } else {
                    UIHelper.showTheirProfileDialogFragment(context,comment.getCreatedBy(),userProfile.getName());
                }
            }
        });

		return view;
		
	}

    private void giveItStatus(View view, App4ItComment comment) {

        String postStatus = context.getPostStatus(comment.getIdentifier());
        TextView statusTextView = (TextView)view.findViewById(R.id.comment_row_status);

        if(postStatus == null) {
            statusTextView.setText(getDateStampForComment(comment.getCreatedOn()));
        } else {
            statusTextView.setText(postStatus);
        }

    }

    private String getDateStampForComment(Long postedDateTimeMilliseconds) {
        Date postedDateTime = new Date(postedDateTimeMilliseconds);
        SimpleDateFormat formatToUse;
        if(DateUtil.isOlderThanXDays(postedDateTime,6)) {
            formatToUse = new SimpleDateFormat("dd/MM HH:mm");
        } else {
            formatToUse = new SimpleDateFormat("EEEE HH:mm");
        }

        return formatToUse.format(postedDateTime);
    }

    private void decorateTheRow(View view, int position) {
        View separatorView = view.findViewById(R.id.comment_row_separator);

        if(position == data.size() - 1) {
            separatorView.setVisibility(View.INVISIBLE);
        } else {
            separatorView.setVisibility(View.VISIBLE);
        }

        /*
        if(position % 2 == 0) view.setBackgroundColor(Color.rgb(240,240,240));
        else view.setBackgroundColor(Color.rgb(255,255,255)); */
    }

}

