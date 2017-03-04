package com.dreambig.app4it;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dreambig.app4it.adapter.CommentsAdapter;
import com.dreambig.app4it.api.FirebaseCommentSavedCallback;
import com.dreambig.app4it.api.FirebaseHandleAcceptor;
import com.dreambig.app4it.api.FirebaseStringProcessor;
import com.dreambig.app4it.api.NewsCenter;
import com.dreambig.app4it.api.PhonebookCallback;
import com.dreambig.app4it.entity.BehaviourOverrides;
import com.dreambig.app4it.fragment.CommentsListFragment;
import com.dreambig.app4it.impl.NewsCenterImpl;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.firebase.client.ChildEventListener;

public class CommentsActivity extends Activity {

	private String activityId;
	private String activityTitle;
    private ChildEventListener commentsRealtimeListener;
    private boolean areWeLive;
    private Map<String, PostStatus> postStatusMap = new HashMap<>();
    private CommentsAdapter commentsAdapterKept;
    private String notificationPreference;

    public static enum PostStatus {

        SENDING,
        SENT,
        FAILED

    }

    public void commentsAdapterStartsLoading() {
        findViewById(R.id.comments_activity_frame_container).setVisibility(View.GONE);
        findViewById(R.id.comments_loading_notice).setVisibility(View.VISIBLE);
    }

    public void commentsAdapterStopsLoading() {
        findViewById(R.id.comments_loading_notice).setVisibility(View.GONE);
        findViewById(R.id.comments_activity_frame_container).setVisibility(View.VISIBLE);
        getDelegate().storeActivityIdWhoseCommentsAreBeingLookedAt(activityId); //now we are staring at them. so don't save notifications for these comments
    }

    private void populateInstanceVariables(Bundle extras) {
        activityId = extras.getString(MessageIdentifiers.ACTIVITY_ID);
        activityTitle = extras.getString(MessageIdentifiers.ACTIVITY_TITLE);
    }

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //L og.d("CommentsActivity","onPause() called");
        getDelegate().removeAnyActivityIdWhoseCommentsAreBeingLookedAt();
        areWeLive = false;
        getDelegate().activityStops();
        stopCommentsFeed();
    }

    private void stopCommentsFeed() {
        if(commentsRealtimeListener != null) {
            FirebaseGateway firebaseGateway = new FirebaseGateway(this);
            firebaseGateway.stopRealTimeCommentsFeed(activityId,commentsRealtimeListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //L og.d("CommentsActivity","onResume() called");
        areWeLive = true;
        commentsAdapterStartsLoading();
        getDelegate().activityStarts(new PhonebookCallback() {
            @Override
            public void phoneBookContacts(boolean refreshed, Map<String, String> numberToName) {
                //because by the time we are here we may not be live anymore
                if(areWeLive) {
                    startCommentsFeed();
                }
            }
        });

    }


    private void startCommentsFeed() {
        //stick the list of comments in the view
        CommentsListFragment commentsListFragment = new CommentsListFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.comments_activity_frame_container, commentsListFragment);
        transaction.commit();
    }

    public void startFeedingCommentsFromFirebase(CommentsAdapter commentsAdapter) {
        this.commentsAdapterKept = commentsAdapter;
        FirebaseGateway firebaseGateway = new FirebaseGateway(this);
        firebaseGateway.restartCommentsFeed(activityId,commentsAdapter,getDelegate(),getDelegate().getLoggedInUserId(), new FirebaseHandleAcceptor() {
            @Override
            public void accept(ChildEventListener childEventListener) {
                commentsRealtimeListener = childEventListener;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(shouldWeCreateNewHomeActivity()) {
            //assumption is that this will still call onPause
            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        else {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(MessageIdentifiers.ACTIVITY_ID, activityId);
            setResult(RESULT_OK, resultIntent);
            super.onBackPressed();
        }
    }

    private boolean shouldWeCreateNewHomeActivity() {
        Bundle extras = getIntent().getExtras();
        if(extras == null) {
            return false;
        } else {
            String defaultBehaviourOverride = extras.getString(MessageIdentifiers.OVERRIDE_DEFAULT_APP4IT_BEHAVIOUR);
            return BehaviourOverrides.CREATE_NEXT_ACTIVITY_IN_NEW_TASK.equals(defaultBehaviourOverride);
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//L og.d("CommentsActivity","onCreate()...");
		setContentView(R.layout.activity_comments);

        populateInstanceVariables(getIntent().getExtras());

        setUpBellButton();
		
		//add action to the post button
		Button postButton = (Button)findViewById(R.id.btnCommentsActivityPost);
		
		postButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
                final EditText postEdit = (EditText)findViewById(R.id.editCommentsActivityNewComment);
                String textToPost = postEdit.getText().toString();

                if(textToPost != null && !textToPost.trim().equals("")) {
                    textToPost = textToPost.trim();

                    FirebaseGateway firebaseGateway = new FirebaseGateway(getApplicationContext());
                    String commentId = firebaseGateway.storeCommentForActivity(activityId,getDelegate().getLoggedInUserId(),getDelegate().getLoggedInUserNumber(),textToPost, new FirebaseCommentSavedCallback() {
                        @Override
                        public void accept(String commentId, PostStatus postStatus) {
                            updatePostStatusMap(commentId, postStatus);
                            refreshCommentsView();
                        }
                    });
                    updatePostStatusMap(commentId,PostStatus.SENDING);
                    refreshCommentsView();
                    postEdit.setText("");

                    //post news about it out to the world. ideally without performance impact on main thread. Don't think this is too heavy to justify running on worker thread. could potentially be a callback in the above firebase method
                    NewsCenter newsCenter = new NewsCenterImpl();
                    newsCenter.postNewsAboutNewCommentOnActivity(getApplicationContext(),activityId,activityTitle,getDelegate().getLoggedInUserId());
                }
			}});

	}

    private void setUpBellButton() {
        FirebaseGateway firebaseGateway = new FirebaseGateway(getApplicationContext());
        firebaseGateway.getNotificationsPreference(activityId,getDelegate().getLoggedInUserId(),"optOutComments",new FirebaseStringProcessor() {
            @Override
            public void process(String string) {
                notificationPreference = string;
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if("Y".equalsIgnoreCase(notificationPreference)) {
            menu.getItem(0).setIcon(R.drawable.belloff);
            menu.getItem(0).setOnMenuItemClickListener(new BellClickListener("N"));
        } else if ("N".equalsIgnoreCase(notificationPreference)) {
            menu.getItem(0).setIcon(R.drawable.bellon);
            menu.getItem(0).setOnMenuItemClickListener(new BellClickListener("Y"));
        } else {
            menu.getItem(0).setIcon(R.drawable.bellnone);
        }

        return true;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.comments, menu);
        return true;
    }

    private void refreshCommentsView() {
        if(commentsAdapterKept != null) {
            commentsAdapterKept.reloadDisplay();
        }
    }

    private void updatePostStatusMap(String commentId, PostStatus status) {
        //in case of sending we don't want to override
        if(status.equals(PostStatus.SENDING)) {
            if(postStatusMap.get(commentId) == null) {
                postStatusMap.put(commentId,PostStatus.SENDING);
            }
        } else {
            postStatusMap.put(commentId,status);
        }
    }

    public String getPostStatus(String commentId) {
        PostStatus postStatus = postStatusMap.get(commentId);
        if(postStatus == PostStatus.SENDING) {
            return "sending...";
        } else if (postStatus == PostStatus.SENT) {
            return "sent";
        } else if (postStatus == PostStatus.FAILED) {
            return "failed";
        } else {
            return null;
        }
    }

    private class BellClickListener implements MenuItem.OnMenuItemClickListener {

        private String switchTo;

        public BellClickListener(String switchTo) {
            this.switchTo = switchTo;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {

            notificationPreference = switchTo;

            if(switchTo.equalsIgnoreCase("N")) {
                Toast.makeText(CommentsActivity.this,"Notifications ON",Toast.LENGTH_SHORT).show();
            } else if (switchTo.equalsIgnoreCase("Y")) {
                Toast.makeText(CommentsActivity.this,"Notifications OFF",Toast.LENGTH_SHORT).show();
            }

            invalidateOptionsMenu();

            FirebaseGateway firebaseGateway = new FirebaseGateway(getApplicationContext());
            firebaseGateway.setNotificationPreference(activityId,getDelegate().getLoggedInUserId(),"optOutComments",switchTo);

            return false;
        }
    }


}
