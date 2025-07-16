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
import com.google.android.gms.maps.model.*;

import java.util.Map;

import model.User;
import model.firebase.LocationRepository;
import model.firebase.UserRepository;

public class MapController {
    private final Context context;
    private final MapView mapView;
    private final FusedLocationProviderClient locationClient;
    private final LocationRepository locationRepo = new LocationRepository();
    private final UserRepository userRepo = new UserRepository();

    private GoogleMap googleMap;
    private boolean isMapReady = false;
    private String currentUserId;
    private User currentUser;

    public MapController(MapView mapView, Context context, String currentUserId) {
        this.mapView = mapView;
        this.context = context;
        this.currentUserId = currentUserId;
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

            if (!hasLocationPermission()) {
                Log.w("MapController", "No location permission granted. Showing Tel Aviv.");
                moveToTelAviv();
                return;
            }

            googleMap.setMyLocationEnabled(true);

            // שלב 1: שלוף פרטי משתמש
            userRepo.getUserById(currentUserId, new UserRepository.FirestoreUserCallback() {
                @Override
                public void onSuccess(User user) {
                    currentUser = user;

                    // שלב 2: קבלת מיקום נוכחי
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        locationClient.getLastLocation()
                                .addOnSuccessListener(location -> {
                                    if (location != null) {
                                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f));
                                        Log.d("MapController", "User location: " + userLatLng);

                                        // שמירת מיקום ב־Firestore
                                        locationRepo.updateUserLocation(currentUserId, userLatLng.latitude, userLatLng.longitude);

                                        // הצגת משתמשים מהקהילה
                                        loadCommunityMembersLocations(user.getCommunityName());
                                    } else {
                                        Log.w("MapController", "Location is null. Falling back to Tel Aviv.");
                                        moveToTelAviv();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("MapController", "Failed to get location: " + e.getMessage());
                                    moveToTelAviv();
                                });
                    }, 500);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("MapController", "Failed to get user data: " + e.getMessage());
                    moveToTelAviv();
                }
            });
        });
    }

    private void loadCommunityMembersLocations(String communityName) {
        locationRepo.getUserLocationsByCommunity(communityName, new LocationRepository.FirestoreLocationsCallback() {
            @Override
            public void onSuccess(Map<String, LatLng> userLocations) {
                for (Map.Entry<String, LatLng> entry : userLocations.entrySet()) {
                    String userId = entry.getKey();
                    LatLng latLng = entry.getValue();

                    if (userId.equals(currentUserId)) continue; // לא להוסיף marker של עצמי

                    googleMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("משתמש מהקהילה"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("MapController", "Failed to load community locations: " + e.getMessage());
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
