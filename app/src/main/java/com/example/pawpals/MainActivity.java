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
    private static final int REQUEST_MAP_REPORT = 2001;

    private MapController mapController;
    private User currentUser;

    private float initialTouchY = 0;
    private float lastProgress = 0f;
    private boolean isDragging = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = getIntent().getParcelableExtra("currentUser");

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        AuthHelper authHelper = new AuthHelper();
        FirebaseUser firebaseUser = authHelper.getCurrentUser();

        // === בדיקת משתמש מחובר ===
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();

            UserRepository userRepo = new UserRepository();
            userRepo.getUserById(userId, new UserRepository.FirestoreUserCallback() {
                @Override
                public void onSuccess(User user) {
                    currentUser = user;
                    FcmTokenManager.registerCurrentToken();

                    TextView greetingText = findViewById(R.id.greetingText);
                    TextView statusBar = findViewById(R.id.statusBar);
                    TextView communityStats = findViewById(R.id.communityStats);
                    TextView activeUsersStat = findViewById(R.id.activeUsersStat);
                    TextView totalDogs = findViewById(R.id.totalDogs);

                    greetingText.setText("Hello, " + user.getUserName() + "!");
                    statusBar.setText("You are part of the community: " + user.getCommunityName());

                    String communityName = user.getCommunityName();

                    // Active users
                    MapRepository locationRepo = new MapRepository();
                    locationRepo.getUserLocationsWithNamesByCommunity(communityName,
                            new MapRepository.FirestoreUserLocationsWithNamesCallback() {
                                @Override
                                public void onSuccess(Map<String, Pair<LatLng, String>> userLocationsWithNames) {
                                    activeUsersStat.setText("Active users nearby: " + userLocationsWithNames.size());
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e("MainActivity", "Error fetching active users", e);
                                }
                            });

                    // Community members & dogs
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
        } else {
            Log.w("MainActivity", "No logged-in Firebase user");
        }

        // === MapView ===
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

        // === Focus on specific community area ===
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

        // === Visibility toggle ===
        TextView labelInvisible = findViewById(R.id.labelInvisible);
        TextView labelVisible = findViewById(R.id.labelVisible);
        labelVisible.setAlpha(1f);
        labelInvisible.setAlpha(0.5f);

        labelInvisible.setOnClickListener(v -> {
            mapController.setVisibleToOthers(false);
            labelInvisible.setAlpha(1f);
            labelVisible.setAlpha(0.5f);
        });

        labelVisible.setOnClickListener(v -> {
            mapController.setVisibleToOthers(true);
            labelVisible.setAlpha(1f);
            labelInvisible.setAlpha(0.5f);
        });

        // === Drawer & BottomSheet ===
        setupDrawerAndBottomSheet();
    }

    private void setupDrawerAndBottomSheet() {
        View root = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MotionLayout mainMotion = findViewById(R.id.main);
        MotionLayout drawerMotion = findViewById(R.id.drawerMotionLayout);
        View bottomSheet = findViewById(R.id.bottomSheet);
        View menuButton = findViewById(R.id.imageButton);
        View overlay = drawerMotion.findViewById(R.id.overlay);

        // RTL support
        boolean isRtl = ViewCompat.getLayoutDirection(root) == ViewCompat.LAYOUT_DIRECTION_RTL;
        final int CLOSED_ID = isRtl ? R.id.closed_rtl : R.id.closed_ltr;
        final int OPEN_ID   = isRtl ? R.id.open_rtl   : R.id.open_ltr;
        final int TRANS_ID  = isRtl ? R.id.t_rtl      : R.id.t_ltr;

        drawerMotion.setTransition(TRANS_ID);
        drawerMotion.setState(CLOSED_ID, -1, -1);

        final float drawerW = getResources().getDimension(R.dimen.drawer_width);
        final ViewGroup.MarginLayoutParams mbLp =
                (ViewGroup.MarginLayoutParams) menuButton.getLayoutParams();
        final int marginStartPx = mbLp.getMarginStart();
        final float gapPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, -4, getResources().getDisplayMetrics());

        drawerMotion.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override public void onTransitionStarted(MotionLayout ml, int s, int e) {}
            @Override public void onTransitionChange(MotionLayout ml, int s, int e, float p) {
                float dir = isRtl ? -1f : 1f;
                float delta = (drawerW - marginStartPx - gapPx) * p * dir;
                menuButton.setTranslationX(delta);
            }
            @Override public void onTransitionCompleted(MotionLayout ml, int id) {}
            @Override public void onTransitionTrigger(MotionLayout ml, int id, boolean b, float v) {}
        });

        // BottomSheet drag
        bottomSheet.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchY = event.getRawY();
                    lastProgress = mainMotion.getProgress();
                    isDragging = true;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        float dy = initialTouchY - event.getRawY();
                        float deltaProgress = dy / 500f;
                        float newProgress = lastProgress + deltaProgress;
                        newProgress = Math.max(0f, Math.min(1f, newProgress));
                        mainMotion.setProgress(newProgress);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDragging = false;
                    if (mainMotion.getProgress() > 0.5f) mainMotion.transitionToEnd();
                    else mainMotion.transitionToStart();
                    return true;
            }
            return false;
        });

        // Menu button toggle
        menuButton.setOnClickListener(v -> {
            final boolean wantOpen = drawerMotion.getCurrentState() != OPEN_ID;
            drawerMotion.transitionToState(wantOpen ? OPEN_ID : CLOSED_ID);
        });

        // Close drawer on overlay tap
        overlay.setOnClickListener(v -> drawerMotion.transitionToState(CLOSED_ID));

        // === Buttons in drawer ===
        findViewById(R.id.communityButtonContainer).setOnClickListener(v -> {
            if (currentUser != null) {
                if (currentUser.isManager()) {
                    Intent intent = new Intent(MainActivity.this, ManagerCommunityActivity.class);
                    intent.putExtra("currentUser", currentUser);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(MainActivity.this, CommunityActivity.class);
                    intent.putExtra("currentUser", currentUser);
                    startActivity(intent);
                }
            }
        });

        findViewById(R.id.newReportButtonContainer).setOnClickListener(v -> openReportMapPicker());

        findViewById(R.id.myProfileButton).setOnClickListener(v -> {
            drawerMotion.transitionToState(CLOSED_ID);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
            }, 300);
        });

        findViewById(R.id.settingsButton).setOnClickListener(v -> {
            drawerMotion.transitionToState(CLOSED_ID);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
            }, 300);
        });

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            drawerMotion.transitionToState(CLOSED_ID);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }, 300);
        });
    }

    // === Report Map Picker ===
    private final ActivityResultLauncher<Intent> mapReportPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String type = result.getData().getStringExtra("selectedType");
                            if (type != null) {
                                mapController.enterReportMode(type);
                                MotionLayout mainMotion = findViewById(R.id.main);
                                mainMotion.transitionToStart();
                            }
                        }
                    });

    private void openReportMapPicker() {
        Intent intent = new Intent(this, ReportMapActivity.class);
        mapReportPickerLauncher.launch(intent);
    }

    // === Lifecycle ===
    @Override protected void onResume() { super.onResume(); if (mapController != null) mapController.onResume(); }
    @Override protected void onPause() { if (mapController != null) mapController.onPause(); super.onPause(); }
    @Override protected void onDestroy() { if (mapController != null) mapController.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapController != null) mapController.onLowMemory(); }
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapController != null) mapController.onSaveInstanceState(outState);
    }
}
