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
import android.util.Pair;

import androidx.core.app.ActivityCompat;

import com.example.pawpals.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final Map<String, Marker> userMarkers = new HashMap<>();
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

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

            startRealTimeLocationUpdates(); // התחלת עדכון מיקום רציף

            Log.d("MapController", "key " + context.getString(R.string.maps_api_key));

            if (!hasLocationPermission()) {
                Log.w("MapController", "No location permission granted. Showing Tel Aviv.");
                moveToTelAviv();
                return;
            }

            googleMap.setMyLocationEnabled(true);

            // שלב 1: שליפת פרטי המשתמש הנוכחי
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

                                        // שמירת מיקום
                                        locationRepo.updateUserLocation(currentUserId, userLatLng.latitude, userLatLng.longitude);

                                        // שליפת מיקומי המשתמשים בקהילה
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

    // שליפת מיקומי משתמשים מהקהילה עם שמות והצגתם במפה
    private void loadCommunityMembersLocations(String communityName) {
        locationRepo.listenToCommunityLocations(communityName, currentUserId, userRepo,
                new LocationRepository.FirestoreLiveLocationCallback() {
                    @Override
                    public void onUserLocationUpdated(String userId, String userName, LatLng position) {
                        if (userMarkers.containsKey(userId)) {
                            userMarkers.get(userId).setPosition(position);
                        } else {
                            Marker marker = googleMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(userName));
                            userMarkers.put(userId, marker);
                        }
                    }

                    @Override
                    public void onUserRemoved(String userId) {
                        if (userMarkers.containsKey(userId)) {
                            userMarkers.get(userId).remove();
                            userMarkers.remove(userId);
                        }
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void startRealTimeLocationUpdates() {
        // הגדרת הבקשה לעדכוני מיקום (מינימום כל 5 שניות)
        this.locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, // דיוק גבוה
                5000 // מרווח בקשות: 5 שניות
        )
                .setMinUpdateIntervalMillis(3000) // זמן מינימלי בין עדכונים: 3 שניות
                .build();

        // הגדרת callback לעדכוני מיקום
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    Log.d("MapController", "Realtime user location: " + lat + ", " + lng);

                    // שמירת מיקום ב־Firestore
                    locationRepo.updateUserLocation(currentUserId, lat, lng);
                }
            }
        };

        // בדיקה שיש הרשאות, ואז התחלת בקשת העדכונים
        if (hasLocationPermission()) {
            locationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        }
    }

    private void stopRealTimeLocationUpdates() {
        if (locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
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

    public void onPause() {
        if (mapView != null) mapView.onPause();
        stopRealTimeLocationUpdates(); // הוספה
    }

    public void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        locationRepo.removeLiveLocationListener();

        if (locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
    }

    public void onLowMemory() { mapView.onLowMemory(); }
    public void onSaveInstanceState(Bundle outState) {
        mapView.onSaveInstanceState(outState);
    }
}
