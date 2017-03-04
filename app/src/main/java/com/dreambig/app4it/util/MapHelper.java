package com.dreambig.app4it.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.Toast;

import com.dreambig.app4it.R;
import com.dreambig.app4it.entity.App4ItAddressAndMapLocation;
import com.dreambig.app4it.entity.App4ItMapLocation;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Alexandr on 04/10/2015.
 */
public class MapHelper {

    private static Bitmap textHoldingMarker;

    private static Bitmap getTextHoldingMarker(Context context) {
        if(textHoldingMarker == null) {
            textHoldingMarker = BitmapFactory.decodeResource(context.getResources(), R.drawable.textmarker);
        }

        return textHoldingMarker;
    }

    public static List<Marker> removeMarkers(List<Marker> markers) {
        for(Marker marker : markers) marker.remove();

        return new ArrayList<>();
    }

    public static String getAddressFromLocation(Context context, double latitude, double longitude) throws Exception {
        Geocoder geoCoder = new Geocoder(context, Locale.getDefault());

        List<Address> addresses = geoCoder.getFromLocation(latitude, longitude, 1);
        if(addresses.size() == 0) {
            throw new Exception("No address found");
        }

        return addressToString(addresses.get(0));
    }

    private static String addressToString(Address address) {
        int maxAddressLine = address.getMaxAddressLineIndex();
        if(maxAddressLine > 1) {
            String line1 = address.getAddressLine(0);
            String line2 = address.getAddressLine(1);

            return line1 + " " + line2;
        } else {
            return alternativeWayOfAddressCollection(address);
        }
    }

    private static String alternativeWayOfAddressCollection(Address address) {
        String thoroughfare = StringUtil.emptyStringForNull(address.getThoroughfare());
        String subThoroughfare = StringUtil.emptyStringForNull(address.getSubThoroughfare());
        String postalCode = StringUtil.emptyStringForNull(address.getPostalCode());
        String locality = StringUtil.emptyStringForNull(address.getLocality());
        String knownName = StringUtil.emptyStringForNull(address.getFeatureName());

        String composed = thoroughfare + " " + subThoroughfare + " " + postalCode + " " + locality;
        composed = StringUtil.getRidOfMultipleSpaces(composed);

        return decideOnUsingName(knownName, thoroughfare, subThoroughfare, composed);
    }

    private static String decideOnUsingName(String name, String thoroughfare, String subThoroughfare, String addressNow) {

        if(name.equals("") || (!thoroughfare.equals("") && !subThoroughfare.equals("")) || addressNow.contains(name)) {
            return addressNow;
        } else {
            return name + " " + addressNow;
        }
    }

    public static List<Address> findMapAddressForString(Context context, String in) {
        Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
        try {
            return geoCoder.getFromLocationName(in, 15);
        } catch (IOException e) {
            return null;
        }
    }

    public static List<Marker> navigateOnMap(Context context, GoogleMap googleMap, App4ItMapLocation givenMapLocation, String givenMapAddress, List<App4ItAddressAndMapLocation> addressAndMapLocations) {
        List<Marker> markerList = new ArrayList<>();
        if(givenMapLocation != null) {
            Marker marker = googleMap.addMarker(new MarkerOptions().position(givenMapLocation.toLatLng()));
            markerList.add(marker);
        } else if (addressAndMapLocations != null && addressAndMapLocations.size() > 0) {
            for(App4ItAddressAndMapLocation addressAndMapLocation : addressAndMapLocations) {
                if(addressAndMapLocation.getMapLocation() != null) {
                    markerList.add(addLabeledMarker(googleMap,addressAndMapLocation.getMapLocation().toLatLng(),addressAndMapLocation.getAddress(),context));
                }
            }
        } else if (givenMapAddress != null && !givenMapAddress.trim().equals("")) {
            Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geoCoder.getFromLocationName(givenMapAddress, 15);
                for(Address address : addresses) {
                    LatLng point = new LatLng(address.getLatitude(), address.getLongitude());
                    Marker marker = googleMap.addMarker(new MarkerOptions().position(point));
                    markerList.add(marker);
                }
            } catch (IOException e) {
                Toast.makeText(context, "Failed to receive map location for '" + givenMapAddress + "'", Toast.LENGTH_LONG).show();
            }
        } else {
            return markerList;
        }

        if(markerList.size() == 0) {
            Toast.makeText(context,"'" + givenMapAddress + "' not found", Toast.LENGTH_LONG).show();
        } else if (markerList.size() == 1) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(markerList.get(0).getPosition(), 16F);
            googleMap.animateCamera(cameraUpdate);
        } else {
            fitAllMarkersOnMap(googleMap, markerList);
        }

        return markerList;
    }

    private static void fitAllMarkersOnMap(GoogleMap googleMap, List<Marker> markers) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : markers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();

        int padding = 60; // offset from edges of the map in pixels
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        googleMap.animateCamera(cameraUpdate);
    }

    public static boolean coordinatesTheSame(App4ItMapLocation one, App4ItMapLocation two) {
        double epsilon = 0.00005f;

        return Math.abs(one.getLatitude() - two.getLatitude()) <= epsilon && Math.abs(one.getLongitude() - two.getLongitude()) <= epsilon;
    }

    private static Marker addLabeledMarker(GoogleMap map, LatLng latLng, String text, Context context) {
        Bitmap immutableBitmap = getTextHoldingMarker(context);
        Bitmap bitmap = immutableBitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(bitmap);

        TextPaint paint = new TextPaint();
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(45);
        paint.setTextAlign(Paint.Align.CENTER);

        text = String.valueOf(TextUtils.ellipsize(text, paint, 250, TextUtils.TruncateAt.END));
        canvas.drawText(text, 155, 80, paint);

        MarkerOptions options = new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromBitmap(bitmap)).anchor(0.2f,1.0f);
        return map.addMarker(options);
    }

}
