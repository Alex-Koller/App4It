package com.dreambig.app4it.helper;


import com.dreambig.app4it.R;
import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.enums.Format;
import com.dreambig.app4it.fragment.TheirProfileDialogFragment;
import com.dreambig.app4it.util.DateUtil;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.widget.Toast;

public class UIHelper {

    private static Bitmap gridCellPlaceholder;
    private static Bitmap quickDummyProfilePictureOne;
    private static Bitmap quickDummyProfilePictureTwo;
    private static Bitmap quickDummyProfilePictureThree;
    private static Bitmap noImageAvailable;

    private static Typeface ourFont;

    public static void showRegistrationInputInvalid(Context context, boolean prefixInvalid, boolean mainPartInvalid) {
        if(prefixInvalid && mainPartInvalid) {
            showLongMessage(context,"Please select the phone prefix and insert the rest of your phone number");
        } else if (prefixInvalid) {
            showBriefMessage(context,"Please select the international phone code");
        } else {
            showBriefMessage(context,"Please insert a valid phone number");
        }
    }
	
	public static void showBriefMessage(Context context, String text) {
		Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
	}

	public static void showLongMessage(Context context, String text) {
		Toast.makeText(context, text, Toast.LENGTH_LONG).show();
	}

	
	public static String whenDefinitionToString(Format whenFormat, String value) {
		String ret;
		if(whenFormat.equals(Format.DATE)) {
			ret = DateUtil.printAsDate(value);
		} else if (whenFormat.equals(Format.DATE_TIME)) {
			ret = DateUtil.printAsDateTime(value);
		} else {
			//means the format must be freetext
			ret = value;
		}	
		return ret;
	}
	
	public static int convertPixelsToDp(int value, double scale) {
		return (int) (value * scale + 0.5f);
	}

    public static int convertDpToPixels(int value) {
        return Math.round(value*(Resources.getSystem().getDisplayMetrics().xdpi/DisplayMetrics.DENSITY_DEFAULT));
    }

    public static float getWidthInDp(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels / displayMetrics.density;
    }

    public static Bitmap getGridCellPlaceholderImage(Context context) {
        if(gridCellPlaceholder == null) {
            gridCellPlaceholder = BitmapFactory.decodeResource(context.getResources(), R.drawable.userplaceholder);
        }

        return gridCellPlaceholder;
    }

    public static Bitmap getQuickDummyProfilePictureOne(Context context) {
        if(quickDummyProfilePictureOne == null) {
            quickDummyProfilePictureOne = BitmapFactory.decodeResource(context.getResources(), R.drawable.defaultprofilepicturebackground);
        }

        return quickDummyProfilePictureOne;
    }

    public static Bitmap getQuickDummyProfilePictureTwo(Context context) {
        if(quickDummyProfilePictureTwo == null) {
            quickDummyProfilePictureTwo = BitmapFactory.decodeResource(context.getResources(), R.drawable.defaultprofilepicturebackgroundtwo);
        }

        return quickDummyProfilePictureTwo;
    }

    public static Bitmap getQuickDummyProfilePictureThree(Context context) {
        if(quickDummyProfilePictureThree == null) {
            quickDummyProfilePictureThree = BitmapFactory.decodeResource(context.getResources(), R.drawable.defaultprofilepicturebackgroundthree);
        }

        return quickDummyProfilePictureThree;
    }

    public static Bitmap getNoImageAvailable(Context context) {
        if(noImageAvailable == null) {
            noImageAvailable = BitmapFactory.decodeResource(context.getResources(), R.drawable.noimagaavailablevtwo);
        }

        return noImageAvailable;
    }

    public static Typeface getOrCreateOurFont(Context context) {
        if(ourFont == null) {
            ourFont = Typeface.createFromAsset(context.getAssets(), "fonts/MavenPro-Regular.ttf");
        }

        return ourFont;
    }

    public static void showTheirProfileDialogFragment(Activity owningActivity, String userId, String userName) {
        String tag = "theirProfileDialog";

        FragmentTransaction ft = owningActivity.getFragmentManager().beginTransaction();
        Fragment prev = owningActivity.getFragmentManager().findFragmentByTag(tag);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = TheirProfileDialogFragment.newInstance(userId, userName);
        newFragment.show(ft, tag);
    }
}
