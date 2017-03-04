package com.dreambig.app4it;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.dreambig.app4it.entity.App4ItAddressAndMapLocation;
import com.dreambig.app4it.entity.App4ItMapLocation;
import com.dreambig.app4it.util.MapHelper;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import java.util.List;

public class MapReadActivity extends Activity  {

    private List<App4ItAddressAndMapLocation> addressAndMapLocations;
    private App4ItMapLocation openedWithMapLocation;
    private String openWithMapAddress;
    private String titleToUse;

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_read);
        parseIntentIntoInstanceVariables(getIntent());
        if(openWithMapAddress != null) {
            setTitle(openWithMapAddress);
        }

        if(titleToUse != null) {
            setTitle(titleToUse);
        }

        final GoogleMap googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        googleMap.setPadding(0,95,0,0);
        googleMap.setMyLocationEnabled(true);
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                MapHelper.navigateOnMap(MapReadActivity.this,googleMap,openedWithMapLocation,openWithMapAddress,addressAndMapLocations);
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        getDelegate().activityStops();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDelegate().activityStarts(null);
    }

    private void parseIntentIntoInstanceVariables(Intent intent) {
        openedWithMapLocation = intent.getParcelableExtra(MessageIdentifiers.ACTIVITY_MAP_LOCATION);
        openWithMapAddress = intent.getStringExtra(MessageIdentifiers.ACTIVITY_MAP_ADDRESS);
        addressAndMapLocations = intent.getParcelableArrayListExtra(MessageIdentifiers.ADDRESSES_AND_LOCATIONS);
        titleToUse = intent.getStringExtra(MessageIdentifiers.MAP_READ_TITLE);
    }

}
