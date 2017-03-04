package com.dreambig.app4it.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.dreambig.app4it.App4ItApplication;
import com.dreambig.app4it.R;
import com.dreambig.app4it.SuggestActivity;
import com.dreambig.app4it.api.FirebaseActivityUpdateCallback;
import com.dreambig.app4it.api.NewsCenter;
import com.dreambig.app4it.entity.App4ItActivityParcel;
import com.dreambig.app4it.entity.App4ItMapLocation;
import com.dreambig.app4it.entity.App4ItSuggestion;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserProfile;
import com.dreambig.app4it.enums.Format;
import com.dreambig.app4it.enums.NewsType;
import com.dreambig.app4it.enums.Preference;
import com.dreambig.app4it.enums.SuggestionType;
import com.dreambig.app4it.impl.App4ItUserProfileManager;
import com.dreambig.app4it.impl.NewsCenterImpl;
import com.dreambig.app4it.repository.FirebaseGateway;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.dreambig.app4it.helper.UIHelper;
import com.firebase.client.FirebaseError;

public class SuggestPreferencesFragment extends Fragment {
	
	private SuggestionType type;
	private App4ItActivityParcel activityParcel;
	private Boolean isHomeUserOwner;

    private List<App4ItSuggestion> suggestions;
    private boolean rendered = false;

    private void promoteSuggestion(Format format, String value, App4ItMapLocation suggestedMapLocation) {
        FirebaseGateway firebaseGateway = new FirebaseGateway(getActivity());

        final List<NewsType> whatChanged = new ArrayList<>();
        Format whenType;
        String whenValue;
        String whereValue;
        App4ItMapLocation mapLocation;

        if(type.equals(SuggestionType.TIME)) {
            whatChanged.add(NewsType.ACTIVITY_TIME_EDITED);
            whenType = format;
            whenValue = value;
            whereValue = activityParcel.getWhereAsString();
            mapLocation = activityParcel.getMapLocation();
        } else {
            whatChanged.add(NewsType.ACTIVITY_PLACE_EDITED);
            whenType = activityParcel.getWhenFormat();
            whenValue = activityParcel.getWhenValue();
            whereValue = value;
            mapLocation = suggestedMapLocation;
        }

        firebaseGateway.updateActivityAttributes(activityParcel.getActivityId(),activityParcel.getTitle(),activityParcel.getMoreAbout(),whenType,whenValue,whereValue,mapLocation,activityParcel.getType(), new FirebaseActivityUpdateCallback() {
            @Override
            public void accept(FirebaseError firebaseError) {
                if(firebaseError != null) {
                    UIHelper.showBriefMessage(getActivity(),"An error occured :-( " + firebaseError.getMessage());
                } else {
                    //lets post news about this
                    NewsCenter newsCenter = new NewsCenterImpl();
                    newsCenter.postNewsAboutEditedActivity(getActivity(),activityParcel.getActivityId(),getDelegate().getLoggedInUserId(),whatChanged,activityParcel.getTitle(),activityParcel.getTitle());
                }
            }
        });

    }
	
	private void addActionOnSuggestionButton(Button button, final Format format, final String value, final App4ItMapLocation mapLocation) {
		//only if this activity can be modified by the user
		if(isHomeUserOwner) {
			button.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					//ask if the user really wants to promote this suggestion to become the when/where of the event
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			        builder.setMessage(type == SuggestionType.TIME ? R.string.do_you_want_to_promote_this_time : R.string.do_you_want_to_promote_this_place)
			               .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			                   public void onClick(DialogInterface dialog, int id) {
			                       promoteSuggestion(format, value, mapLocation);
			                   }
			               })
			               .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			                   public void onClick(DialogInterface dialog, int id) {
			                       // don't do anything
			                   }
			               });
			        // Create the AlertDialog object and show it
			        builder.create().show();										
				}});
		} else {
			button.setClickable(false);
		}
	}

    public void setSuggestions(List<App4ItSuggestion> suggestions) {
        this.suggestions = suggestions;
        if(!rendered && getView() != null) {
            redraw(getView());
            rendered = true;
        }
    }

    public List<App4ItSuggestion> getSuggestions() {
        return suggestions;
    }

    public boolean isThereAlreadySuchSuggestionFormat(Format format, String value) {

        if(suggestions == null) {
            return false;
        }

        for(App4ItSuggestion suggestion : suggestions) {

            if(suggestion.getFormat().equals(format) && suggestion.getValue().equalsIgnoreCase(value)) {
                return true;
            }

        }

        return false;
    }

    public void addSuggestion(App4ItSuggestion suggestion) {
        suggestions.add(suggestion);
        redraw(getView());
        makeSureSuggestionIsVisible((HorizontalScrollView) getView().findViewById(R.id.suggest_activity_preferences_layout_horizontal_scroll));
    }

    private void makeSureSuggestionIsVisible(final HorizontalScrollView scrollView) {
        View mainView = getView();

        //we do not actually make sure the layout is called but it seems to work
        if(mainView != null) {
            final LinearLayout mainGrid = (LinearLayout) mainView.findViewById(R.id.suggestMainGrid);
            mainGrid.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    removeMyself();

                    LinearLayout lastSuggestionView = (LinearLayout) mainGrid.getChildAt(suggestions.size() - 1);
                    if (lastSuggestionView != null) {
                        Rect rect = new Rect();
                        mainGrid.getGlobalVisibleRect(rect);
                        int mainGridHitWidth = rect.width();
                        int lastSuggestionViewWidth = lastSuggestionView.getWidth();
                        int scrollToX = lastSuggestionView.getLeft() - (mainGridHitWidth - lastSuggestionViewWidth); //so last suggestion occupies the right most part of the visible main grid
                        if (scrollToX > 0) {
                            scrollView.scrollTo(scrollToX, 0);
                        }
                    }
                }

                private void removeMyself() {
                    if (Build.VERSION.SDK_INT < 16)
                        mainGrid.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    else
                        mainGrid.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });

        }
    }

    private void fadePreferencesOut(View containingView) {
        containingView.findViewById(R.id.preferencesContainer).setVisibility(View.INVISIBLE);
    }

    private void fadePreferencesIn(View containingView) {
        View viewToFadeIn = containingView.findViewById(R.id.preferencesContainer);
        AlphaAnimation anim = new AlphaAnimation(0, 1);
        anim.setDuration(1000);
        viewToFadeIn.setVisibility(View.VISIBLE);
        viewToFadeIn.startAnimation(anim);
    }

    private void redraw(View containingView) {
		//L og.d("SuggestPreferencesFragment", "redraw(...)...");

        App4ItApplication delegate = getDelegate();
		LinearLayout namesContainer = (LinearLayout)containingView.findViewById(R.id.suggestNamesColumn);
		LinearLayout mainGrid = (LinearLayout)containingView.findViewById(R.id.suggestMainGrid);
		//first remove what's there now
		namesContainer.removeAllViews();
		mainGrid.removeAllViews();
		
		if(suggestions.size() > 0) {
            fadePreferencesOut(containingView);
            containingView.findViewById(R.id.noSuggestionsNote).setVisibility(View.GONE);
			//(re)build the matrix		
			List<App4ItUser> users = createListOfUsers(suggestions,delegate);
			populateNameList(users,namesContainer,delegate);
			
			//now the suggestion columns
			for(int i = 0;i < suggestions.size();i++) {
				final App4ItSuggestion suggestion = suggestions.get(i);
				LinearLayout prefContainer = createOneSuggestionLinearLayout();
				String humanReadableHeader = (type == SuggestionType.TIME ? UIHelper.whenDefinitionToString(suggestion.getFormat(), suggestion.getValue()) : suggestion.getValue());
				
				Button headerButton = createGridButton(humanReadableHeader,R.drawable.black_button);
				headerButton.setTextColor(Color.WHITE);
				addActionOnSuggestionButton(headerButton,suggestion.getFormat(),suggestion.getValue(),suggestion.getMapLocation());
				prefContainer.addView(headerButton);
				for(App4ItUser user : users) {
					Button button;
					Preference preference = suggestion.getResponses().get(user.getUserId());
					if(preference ==  null && user.getUserId().equals(delegate.getLoggedInUserId())) {
						//home user hasn't declared their preference in this suggestion
						button = createGridButton("Tap to toggle",R.drawable.light_mouse_gray_button);
					} else if (preference == null) {
						button = createGridButton("",R.drawable.light_mouse_gray_button);
					} else if (preference.equals(Preference.FINE)) {
						button = createGridButton("",R.drawable.green_button);
					} else {
						button = createGridButton("",R.drawable.red_button);
					}
					
					if(user.getUserId().equals(delegate.getLoggedInUserId())) {
						button.setTag(preference);
						button.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View view) {
								Preference preference = (Preference)view.getTag();
								if(preference == null) {
									//move it to green
									view.setBackgroundResource(R.drawable.green_button);
									view.setTag(Preference.FINE);
                                    setAnswerToSuggestion(Preference.FINE, suggestion.getSuggestionId());
								} else if (preference.equals(Preference.FINE)) {
									//move it to red
									view.setBackgroundResource(R.drawable.red_button);
									view.setTag(Preference.NO);
                                    setAnswerToSuggestion(Preference.NO, suggestion.getSuggestionId());
								} else if (preference.equals(Preference.NO)) {
									//move to green
									view.setBackgroundResource(R.drawable.green_button);
									view.setTag(Preference.FINE);
                                    setAnswerToSuggestion(Preference.FINE, suggestion.getSuggestionId());
								}
								
							}});
					} else {
						button.setClickable(false);
					}
					
					prefContainer.addView(button);
				}
				 
				mainGrid.addView(prefContainer);
			}


            fillInWithEmptyTiles(users,mainGrid);
            fadePreferencesIn(containingView);
		} else {
            containingView.findViewById(R.id.noSuggestionsNote).setVisibility(View.VISIBLE);
		}
	}

    private void setAnswerToSuggestion(Preference preference, String suggestionId) {
        App4ItApplication delegate = getDelegate();
        FirebaseGateway firebaseGateway = new FirebaseGateway(getActivity());
        firebaseGateway.saveResponseToSuggestion(type,delegate.getLoggedInUserId(),delegate.getLoggedInUserNumber(),activityParcel.getActivityId(),preference,suggestionId);
    }

    private void fillInWithEmptyTiles(Collection<App4ItUser> users, LinearLayout mainGrid) {
        //9 is number just about enough to fill in the grid
        int minNumberOfTiles = 9;

        if(suggestions.size() < minNumberOfTiles) {
            for(int i = 0;i < (minNumberOfTiles - suggestions.size());i++) {
                LinearLayout prefContainer = createOneSuggestionLinearLayout();
                for(int j = 0;j < users.size() + 1;j++) {
                    Button button = createGridButton("",R.drawable.light_mouse_gray_button);
                    button.setClickable(false);
                    prefContainer.addView(button);
                }
                mainGrid.addView(prefContainer);
            }
        }
    }
	

	
	private int convertDipToPixels(int value) {
		Resources r = getResources();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, r.getDisplayMetrics());
		return (int)(px + 0.5f);
	}	
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        populateInstanceVariables(getArguments());

    	View ret = inflater.inflate(R.layout.suggest_activity_preferences_fragment, container, false);

        if(!rendered && suggestions != null) {
            redraw(ret);
            rendered = true;
        }

        return ret;
    }

    private void populateInstanceVariables(Bundle arguments) {
        this.type = SuggestionType.valueOf(arguments.getString(MessageIdentifiers.SUGGEST_TYPE));
        this.activityParcel = arguments.getParcelable(MessageIdentifiers.ACTIVITY_PARCEL);
        this.isHomeUserOwner = arguments.getBoolean(MessageIdentifiers.IS_HOME_USER_OWNER);
    }
    
	private LinearLayout createOneSuggestionLinearLayout() {
		LinearLayout ret = new LinearLayout(getActivity());
		ret.setOrientation(LinearLayout.VERTICAL);
		LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
		ret.setLayoutParams(layoutParams);
		return ret;
	}    
    
	private void populateNameList(List<App4ItUser> users, LinearLayout container,App4ItApplication delegate) {
		//first add the empty space in the top left corner of the 'grid' - or the map
        if(type.equals(SuggestionType.TIME)) {
            container.addView(createGridButton("",R.drawable.clear_button));
        } else {
            Button mapButton = createGridButton(getResources().getString(R.string.view_on_map), R.drawable.clear_button);
            mapButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((SuggestActivity)getActivity()).navigateToReadMap();
                }
            });
            container.addView(mapButton);
        }

		for(App4ItUser user : users) {
			String displayName = user.getName();
            if(displayName == null) {
                App4ItUserProfile userProfile = App4ItUserProfileManager.getUserProfile(getActivity(),user);
                displayName = userProfile.getName();
            }
			int drawableRes = user.getUserId().equals(delegate.getLoggedInUserId()) ? R.drawable.mouse_gray_button : R.drawable.light_mouse_gray_button;
			Button button = createGridButton(displayName,drawableRes);
			button.setClickable(false);
			container.addView(button);
		}	
	}  
	
	private Button createGridButton(String text, int backgroundDrawable) {
		Button ret = new Button(getActivity());
		ret.setBackgroundResource(backgroundDrawable);

		ret.setMinHeight(convertDipToPixels(37));
		ret.setHeight(convertDipToPixels(37));
		int margin = convertDipToPixels(2);
		int padding = convertDipToPixels(3);
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(0, margin, margin, 0);
		ret.setLayoutParams(layoutParams);
		ret.setPadding(padding, 0, padding, 0);
		//stick a line break on the place of the first space
		String textBroken = text.replaceFirst("\\s", System.getProperty("line.separator"));
		ret.setText(textBroken);
        ret.setTextSize(12);
		return ret;
	}

    private List<App4ItUser> createListOfUsers(List<App4ItSuggestion> suggestions, App4ItApplication delegate) {
    	Set<App4ItUser> setOfUsers = new HashSet<>();
    	
    	for(App4ItSuggestion suggestion : suggestions) {
            setOfUsers.addAll(suggestion.getResponders());
    	}
    	
    	List<App4ItUser> ret = new ArrayList<>(setOfUsers);
    	//now make sure that the home user is there
    	App4ItUser homeUser = new App4ItUser("You",delegate.getLoggedInUserNumber(),delegate.getLoggedInUserId());
    	ret.remove(homeUser);
    	ret.add(homeUser);
    	return ret;
    }

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getActivity().getApplication();
    }
           
}
