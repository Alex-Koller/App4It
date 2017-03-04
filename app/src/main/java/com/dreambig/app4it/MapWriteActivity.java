package com.dreambig.app4it;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.dreambig.app4it.entity.App4ItMapLocation;
import com.dreambig.app4it.helper.A4ItHelper;
import com.dreambig.app4it.util.MapHelper;
import com.dreambig.app4it.util.MessageIdentifiers;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MapWriteActivity extends Activity  {

    public static final int ACTION_DONE = 1;
    private String addressMarkersBelongTo;
    private App4ItMapLocation openedWithMapLocation;
    private String openWithMapAddress;
    private List<Marker> currentMarkers = new ArrayList<>();

    private App4ItApplication getDelegate() {
        return (App4ItApplication)getApplication();
    }

    private EditText getSearchField() {
        return ((EditText)findViewById(R.id.map_write_textfield));
    }

    private void setTextField(String value) {
        getSearchField().setText(value);
    }

    private GoogleMap getGoogleMap() {
        return ((MapFragment) getFragmentManager().findFragmentById(R.id.map_write)).getMap();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mapwrite, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_done:
                A4ItHelper.hideKeyboard(MapWriteActivity.this,getSearchField());
                handleDoneClick();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleDoneClick() {

        if(!getValueInSearchField().equals(addressMarkersBelongTo)) {
            searchForAddress(getValueInSearchField());
        }

        proceedSearchAndMarkersInLine();
    }

    private void proceedSearchAndMarkersInLine() {
        if(currentMarkers.size() == 0 && getValueInSearchField().equals("")) {
            Toast.makeText(MapWriteActivity.this, "No address has been selected", Toast.LENGTH_SHORT).show();
        } else if (currentMarkers.size() == 0) {
            Toast.makeText(MapWriteActivity.this,"'" + getValueInSearchField() + "' not found", Toast.LENGTH_LONG).show();
        }
        else if (currentMarkers.size() == 1) {
            Marker marker = currentMarkers.get(0);
            navigateBack(new App4ItMapLocation(marker.getPosition().latitude,marker.getPosition().longitude));
        } else {
            Toast.makeText(MapWriteActivity.this, "Tap one of the markers to choose the location to save", Toast.LENGTH_LONG).show();
        }
    }

    private void giveMarkersListener(GoogleMap googleMap) {
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                navigateBack(new App4ItMapLocation(marker.getPosition().latitude,marker.getPosition().longitude));
                return true;
            }
        });
    }

    private void navigateBack(App4ItMapLocation selectedMapLocation) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(MessageIdentifiers.ACTIVITY_MAP_LOCATION,selectedMapLocation);
        resultIntent.putExtra(MessageIdentifiers.ACTIVITY_MAP_ADDRESS,getValueInSearchField());
        setResult(ACTION_DONE,resultIntent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_write);
        parseIntentIntoInstanceVariables(getIntent());
        giveActions();
        if(openWithMapAddress != null) {
            setTextField(openWithMapAddress);
        }

        final GoogleMap googleMap = getGoogleMap();
        googleMap.setPadding(0,160,0,0);
        googleMap.setMyLocationEnabled(true);
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                addressMarkersBelongTo = openWithMapAddress;
                currentMarkers = MapHelper.navigateOnMap(MapWriteActivity.this,googleMap,openedWithMapLocation,openWithMapAddress,null);
            }
        });

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                try {
                    currentMarkers = MapHelper.removeMarkers(currentMarkers);
                    currentMarkers.add(getGoogleMap().addMarker(new MarkerOptions().position(latLng)));
                    String address = MapHelper.getAddressFromLocation(MapWriteActivity.this, latLng.latitude, latLng.longitude);
                    setTextField(address);
                    addressMarkersBelongTo = address;
                } catch (Exception e) {
                    Toast.makeText(MapWriteActivity.this, "Map address couldn't be received", Toast.LENGTH_SHORT).show();
                }
            }
        });

        giveMarkersListener(googleMap);
    }

    private String getValueInSearchField() {
        return getSearchField().getText().toString().trim();
    }

    private void giveActions() {
        findViewById(R.id.map_write_searchBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String lookFor = getValueInSearchField();
                if(!lookFor.equals("")) {
                    A4ItHelper.hideKeyboard(MapWriteActivity.this,getSearchField());
                    searchForAddress(lookFor);
                }
            }
        });
    }

    private void searchForAddress(String address) {
        currentMarkers = MapHelper.removeMarkers(currentMarkers);
        currentMarkers = MapHelper.navigateOnMap(MapWriteActivity.this,getGoogleMap(),null,address,null);
        addressMarkersBelongTo = address;
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
    }


}
