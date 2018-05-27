package com.apps.realtimetrackersample;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.location.places.Places;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap, map;
    LatLng locationMarker;
    ArrayList<LatLng> path;
    Polyline line;
    private HashMap<String, Marker> mMarkers = new HashMap<>();

    public GoogleApiClient mGoogleApiClient;
    public LocationRequest mLocationRequest;
    private final static int REQUEST_LOCATION = 199;
    private boolean isAlreadyConnected = false;
    private static boolean never_ask_again = false;
    private AlertDialog alert;
    
    String key = "Current Location";
    Button startTrack, endTrack, distance;
    boolean isSTARTClicked = false, isENDClicked = false;
    double totalDistance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        startTrack = findViewById(R.id.startTrack);
        endTrack = findViewById(R.id.endTrack);
        distance = findViewById(R.id.distance);
        distance.setVisibility(View.GONE);
        startTrack.setVisibility(View.GONE);
        endTrack.setVisibility(View.GONE);

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mLocationReceiver, new IntentFilter("LatLong"));

        initiateGPSState();
    }

    public void showButtons(){
        setMarker();
        startTrack.setVisibility(View.VISIBLE);
        endTrack.setVisibility(View.VISIBLE);
        startTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSTARTClicked = true;
                isENDClicked = false;
                mMap.clear();
                mMap.setMaxZoomPreference(18);
                setMarker();
                path = new ArrayList<LatLng>();
                totalDistance =0;
                distance.setVisibility(View.GONE);
                setMarker("Start Point");
            }
        });
        endTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSTARTClicked){
                    isSTARTClicked = false;
                    isENDClicked = true;
                    if (isLocationServiceRunning(LocationTracker.class)){
                        stopService(new Intent(MainActivity.this, LocationTracker.class));
                    }
                    totalDistance = getDistance(path);
                    //Log.d("distance", "Distance in meters: " + totalDistance);
                    distance.setText(((int) totalDistance) + " meters distance covered");
                    distance.setVisibility(View.VISIBLE);
                    setMarker("End Point");
                } else{
                    Toast.makeText(MainActivity.this, "Please Click Start Tracking First!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        map = googleMap;
        mMap.setMaxZoomPreference(18);
        //if (!isSTARTClicked)
        setMarker();
    }

    private void setMarker(final String title) {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            initiateGPSState();
        } else {
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    //Toast.makeText(MainActivity.this, "Lat: " + location.getLatitude(), Toast.LENGTH_SHORT).show();
                    locationMarker = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(locationMarker).title(title).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationMarker, 16.0f));
                }
            }, null);
        }
    }

    private void setMarker() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationRequest request = new LocationRequest();
        request.setInterval(2000);
        request.setFastestInterval(2000);
        request.setSmallestDisplacement(1F);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            initiateGPSState();
        } else {
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    locationMarker = new LatLng(location.getLatitude(), location.getLongitude());

                    if (isSTARTClicked == true && isENDClicked == false){
                        path.add(locationMarker);
                    }

                    if (!mMarkers.containsKey(key)) {
                        mMarkers.put(key, mMap.addMarker(new MarkerOptions().title(key).position(locationMarker).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))));
                    } else {
                        mMarkers.get(key).setPosition(locationMarker);
                    }
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (Marker marker : mMarkers.values()) {
                        builder.include(marker.getPosition());
                    }
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
                }
            }, null);
        }
    }

    public void initiateGPSState() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION}, 999);
        } else {
            if (servicesPlayServicesAvailable()) {
                noLocation();
            }
        }
    }

    private boolean servicesPlayServicesAvailable() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(MainActivity.this);
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(resultCode, MainActivity.this, 0).show();
            return false;
        }
    }

    public boolean noLocation() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            enableLoc();
        } else if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            connectGoogleApiClient();
        } else {
            showButtons();
        }
        return false;
    }

    private void enableLoc() {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            connectGoogleApiClient();
        } else {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
            builder.setAlwaysShow(true);
            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    if (status.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        try {
                            //never_ask_again = false;
                            status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                    } else if (status.getStatusCode() == LocationSettingsStatusCodes.CANCELED){
                        alertForNeverAskAgain("Please turn on GPS!");
                    }
                }
            });
        }
    }

    private void connectGoogleApiClient() {
        isAlreadyConnected = false;
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .enableAutoManage(this, 0, this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 999) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    boolean showRationale = false;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        alertForNeverAskAgain("Application needs to access your Location. Please provide permission to access Location.");
                    }
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    never_ask_again = false;
                    initiateGPSState();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_LOCATION: {
                if (requestCode == REQUEST_LOCATION && resultCode == RESULT_OK) {
                    showButtons();
                } else {
                    initiateGPSState();
                }
            }
        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (!isAlreadyConnected) {
            isAlreadyConnected = true;
            noLocation();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }


    @Override
    protected void onPause(){
        super.onPause();
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && isSTARTClicked && !isENDClicked) {
            Intent intent = new Intent(this, LocationTracker.class);
            startService(intent);
        }
    }

    @Override
    protected void onDestroy() {
        if (alert != null && alert.isShowing()) {
            alert.dismiss();
        }
        super.onDestroy();
        if (mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
        if (isLocationServiceRunning(LocationTracker.class)){
            stopService(new Intent(MainActivity.this, LocationTracker.class));
        }
    }

    @Override
    protected void onStop() {
        if (alert != null && alert.isShowing()) {
            alert.dismiss();
        }
        super.onStop();
        if (mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && isSTARTClicked && !isENDClicked) {
            Intent intent = new Intent(this, LocationTracker.class);
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        super.onResume();
        if(never_ask_again){
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                never_ask_again = false;
                initiateGPSState();
            } else {
                alertForNeverAskAgain("Application needs to access your Location. Please provide permission to access Location.");
            }
        }
        if (isLocationServiceRunning(LocationTracker.class)){
            stopService(new Intent(MainActivity.this, LocationTracker.class));
        }
    }

    private void alertForNeverAskAgain(String message) {
        AlertDialog.Builder locationDialog = new AlertDialog.Builder(MainActivity.this);
        locationDialog.setMessage(message);
        locationDialog.setCancelable(false);
        locationDialog.setTitle("Permission Required!!!");
        locationDialog.setPositiveButton("Goto Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (alert != null && alert.isShowing()) {
                            alert.dismiss();
                        }
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                        never_ask_again = true;
                    }
                });
        if (alert != null && alert.isShowing()) {
            alert.dismiss();
        }
        alert = locationDialog.create();
        alert.show();
    }

    private double getDistance(ArrayList<LatLng> locationPoints){
        double totalCoveredDistance = 0;
        int length = locationPoints.size() - 1;
        float[] distance = new float[1];
        for (int i = 0; i < length; i++) {
            Location.distanceBetween(locationPoints.get(i).latitude,
                    locationPoints.get(i).latitude,
                    locationPoints.get(i+1).latitude,
                    locationPoints.get(i+1).latitude,
                    distance);
            totalCoveredDistance = totalCoveredDistance + distance[0];
        }
        return totalCoveredDistance;
    }

    private boolean isLocationServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getBundleExtra("Location");
            LatLng location = (LatLng) bundle.getParcelable("Location");
            if (path != null){
                path.add(location);
            }
        }
    };

}
