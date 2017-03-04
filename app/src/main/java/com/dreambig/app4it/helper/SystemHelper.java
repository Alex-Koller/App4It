package com.dreambig.app4it.helper;

import android.os.Build;

/**
 * Created by Alexandr on 25/11/2015.
 */
public class SystemHelper {

    public static boolean areWePre21() {
        return Build.VERSION.SDK_INT < 21;
    }

}
