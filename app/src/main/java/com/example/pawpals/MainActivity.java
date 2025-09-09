package com.example.pawpals;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Map;

import model.User;
import model.firebase.Authentication.AuthHelper;
import model.firebase.CloudMessaging.FcmTokenManager;
import model.firebase.firestore.CommunityRepository;
import model.firebase.firestore.MapRepository;
import model.firebase.firestore.UserRepository;
import model.maps.MapController;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_FOCUS_COMMUNITY_NAME = "EXTRA_FOCUS_COMMUNITY_NAME";
    public static final String EXTRA_FOCUS_RADIUS = "EXTRA_FOCUS_RADIUS";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MapController mapController;
    private User currentUser;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = getIntent().getParcelableExtra("currentUser");

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        AuthHelper authHelper = new AuthHelper();
        FirebaseUser firebaseUser = authHelper.getCurrentUser();

        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();

            UserRepository userRepo = new UserRepository();
            userRepo.getUserById(userId, new UserRepository.FirestoreUserCallback() {
                @Override
                public void onSuccess(User user) {
                    FcmTokenManager.registerCurrentToken();
                    TextView greetingText = findViewById(R.id.greetingText);
                    TextView statusBar = findViewById(R.id.statusBar);

                    greetingText.setText("Hello, " + user.getUserName() + "!");
                    statusBar.setText("You are part of the community: " + user.getCommunityName());

                    TextView communityStats = findViewById(R.id.communityStats);
                    TextView activeUsersStat = findViewById(R.id.activeUsersStat);
                    TextView totalDogs = findViewById(R.id.totalDogs);

                    String communityName = user.getCommunityName();

                    MapRepository locationRepo = new MapRepository();
                    locationRepo.getUserLocationsWithNamesByCommunity(communityName, new MapRepository.FirestoreUserLocationsWithNamesCallback() {
                        @Override
                        public void onSuccess(Map<String, Pair<LatLng, String>> userLocationsWithNames) {
                            activeUsersStat.setText("Active users nearby: " + userLocationsWithNames.size());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e("MainActivity", "Error fetching active users", e);
                        }
                    });

                    userRepo.getUsersByCommunity(communityName, new UserRepository.FirestoreUsersListCallback() {
                        @Override
                        public void onSuccess(List<User> users) {
                            communityStats.setText("Community members: " + users.size());
                            int dogsCount = 0;
                            for (User u : users) {
                                if (u.getDogs() != null) dogsCount += u.getDogs().size();
                            }
                            totalDogs.setText("Total dogs in community: " + dogsCount);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e("MainActivity", "Error fetching users in community", e);
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("MainActivity", "Failed to fetch user info", e);
                }
            });
        }

        MapView mapView = findViewById(R.id.mapView);

        if (firebaseUser != null) {
            String currentUserId = firebaseUser.getUid();
            mapController = new MapController(mapView, this, currentUserId);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                mapController.initializeMap(savedInstanceState);
            }
        }

        // --- Handle "Area Map" focusing ---
        String focusCommunity = getIntent().getStringExtra(EXTRA_FOCUS_COMMUNITY_NAME);
        int focusRadius = getIntent().getIntExtra(EXTRA_FOCUS_RADIUS, 1500);

        if (focusCommunity != null && !focusCommunity.isEmpty()) {
            CommunityRepository communityRepo = new CommunityRepository();
            communityRepo.getCommunityCenterAndRadiusByName(focusCommunity,
                    new CommunityRepository.CommunityGeoCallback() {
                        @Override
                        public void onSuccess(double lat, double lng, int radiusMeters) {
                            int r = (focusRadius > 0) ? focusRadius : radiusMeters;
                            mapController.focusOnArea(lat, lng, r);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Log.e("MainActivity", "Failed to get community center: " + e.getMessage());
                        }
                    });
        }

        // יתר הקוד (Drawer, BottomSheet, כפתורים) נשאר כמו שכתבת קודם
    }

    @Override
    protected void onResume() { super.onResume(); if (mapController != null) mapController.onResume(); }
    @Override
    protected void onPause() { if (mapController != null) mapController.onPause(); super.onPause(); }
    @Override
    protected void onDestroy() { if (mapController != null) mapController.onDestroy(); super.onDestroy(); }
    @Override
    public void onLowMemory() { super.onLowMemory(); if (mapController != null) mapController.onLowMemory(); }
    @Override
    protected void onSaveInstanceState(Bundle outState) { super.onSaveInstanceState(outState); if (mapController != null) mapController.onSaveInstanceState(outState); }
}
