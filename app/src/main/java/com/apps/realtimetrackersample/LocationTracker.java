package com.apps.realtimetrackersample;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class LocationTracker extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private LatLng locationMarker;
    private ArrayList<LatLng> path = new ArrayList<LatLng>();

    public LocationTracker() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        requestLocationUpdates();
        return super.onStartCommand(intent, flags, startId);
    }

    private void requestLocationUpdates() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationRequest request = new LocationRequest();
        request.setInterval(2000);
        request.setFastestInterval(2000);
        request.setSmallestDisplacement(1F);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return;
        } else {
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    locationMarker = new LatLng(location.getLatitude(), location.getLongitude());
                    Intent intent = new Intent("LatLong");
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("Location", locationMarker);
                    intent.putExtra("Location", bundle);
                    LocalBroadcastManager.getInstance(LocationTracker.this).sendBroadcast(intent);
                    path.add(locationMarker);
                }
            }, null);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
