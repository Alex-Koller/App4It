package com.dreambig.app4it.util;

import java.io.FileOutputStream;

/**
 * Created by Alexandr on 02/01/2016.
 */
public class FileUtil {

    public static void attemptToClose(FileOutputStream fio) {
        if(fio != null) {
            try {
                fio.close();
            } catch (Exception e) {
                //not much to do here
            }
        }
    }

}
