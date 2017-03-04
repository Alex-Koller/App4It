package com.dreambig.app4it.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

/**
 * Created by Alexandr on 11/11/2015.
 */
public class ImageUtil {

    public static void compressBitmapToJPEGFile(Bitmap bmp, int compressionLevel, FileOutputStream fio) {
        bmp.compress(Bitmap.CompressFormat.JPEG, compressionLevel, fio);
    }

    public static byte[] compressBitmapIntoJPEG(Bitmap bmp, int compressionLevel) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, compressionLevel, stream);
        return stream.toByteArray();
    }

    public static String imageToBase64StringQualityHigh(Bitmap image) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);

    }

    public static String imageToBase64StringQualityLow(Bitmap image) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 25, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);

    }

    public static Bitmap toSmallImageVersion(Bitmap image) {
        return Bitmap.createScaledBitmap(image, 192, 192, false);
    }

    public static Bitmap base64StringToBitmap(String data) {
        if(data != null) {
            byte[] decodedByte = Base64.decode(data, 0);
            return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
        } else {
            return null;
        }
    }
}
