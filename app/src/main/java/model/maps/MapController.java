package model.maps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.pawpals.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;

public class MapController {
    private final Context context;
    private final FusedLocationProviderClient locationClient;
    private final MapView mapView;
    private GoogleMap googleMap;
    private boolean isMapReady = false;

    public MapController(MapView mapView, Context context) {
        this.mapView = mapView;
        this.context = context;
        this.locationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void initializeMap(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(map -> {
            googleMap = map;
            isMapReady = true;

            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

            Log.d("MapController", "key " + context.getString(R.string.maps_api_key));

            if (hasLocationPermission()) {
                googleMap.setMyLocationEnabled(true);

                // נחכה מעט לאחר onMapReady
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    locationClient.getLastLocation()
                            .addOnSuccessListener(location -> {
                                if (location != null) {
                                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                    Log.d("MapController", "Real location: " + userLatLng.latitude + ", " + userLatLng.longitude);
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f));
                                } else {
                                    Log.w("MapController", "Location is null. Falling back to Tel Aviv.");
                                    moveToTelAviv();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("MapController", "Failed to get location: " + e.getMessage());
                                moveToTelAviv();
                            });
                }, 500); // המתנה קצרה לטעינת map tiles
            } else {
                Log.w("MapController", "No location permission granted. Showing Tel Aviv.");
                moveToTelAviv();
            }
        });
    }

    private void moveToTelAviv() {
        if (isMapReady && googleMap != null) {
            LatLng telAviv = new LatLng(32.0853, 34.7818);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(telAviv, 13f));
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Lifecycle methods
    public void onResume() { mapView.onResume(); }
    public void onPause() { mapView.onPause(); }
    public void onDestroy() { mapView.onDestroy(); }
    public void onLowMemory() { mapView.onLowMemory(); }
    public void onSaveInstanceState(Bundle outState) {
        mapView.onSaveInstanceState(outState);
    }
}
