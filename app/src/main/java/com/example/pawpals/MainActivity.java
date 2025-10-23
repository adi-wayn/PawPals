package com.example.pawpals;

import static android.content.ContentValues.TAG;

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
import java.util.concurrent.atomic.AtomicInteger;

import model.Dog;
import model.User;
import model.firebase.Authentication.AuthHelper;
import model.firebase.CloudMessaging.FcmTokenManager;
import model.firebase.Firestore.CommunityRepository;
import model.firebase.Firestore.MapRepository;
import model.firebase.Firestore.UserRepository;
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

                    // === Active users ===
                    MapRepository locationRepo = new MapRepository();
                    locationRepo.getUserLocationsWithNamesByCommunity(
                            communityName,
                            new MapRepository.FirestoreUserLocationsWithNamesCallback() {
                                @Override
                                public void onSuccess(Map<String, Pair<LatLng, String>> userLocationsWithNames) {
                                    activeUsersStat.setText("Active users nearby: " + userLocationsWithNames.size());
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e("MainActivity", "Error fetching active users", e);
                                }
                            }
                    );

                    // === Community members & dogs ===
                    userRepo.getUsersByCommunityWithIds(
                            communityName,
                            new UserRepository.FirestoreUsersWithIdsCallback() {
                                @Override
                                public void onSuccess(List<Pair<String, User>> rows) {
                                    communityStats.setText("Community members: " + rows.size());
                                    AtomicInteger dogsCount = new AtomicInteger(0);
                                    AtomicInteger completed = new AtomicInteger(0);

                                    for (Pair<String, User> pair : rows) {
                                        String uid = pair.first;

                                        userRepo.getDogsForUser(
                                                uid,
                                                new UserRepository.FirestoreDogsListCallback() {
                                                    @Override
                                                    public void onSuccess(List<Dog> dogs) {
                                                        Log.w(TAG, "user: " + pair.second.getUserName() + " dogs: " + dogs);
                                                        dogsCount.addAndGet(dogs.size());
                                                        if (completed.incrementAndGet() == rows.size()) {
                                                            totalDogs.setText("Total dogs in the community: " + dogsCount.get());
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(Exception e) {
                                                        Log.w(TAG, "Failed to get dogs for user " + uid, e);
                                                        if (completed.incrementAndGet() == rows.size()) {
                                                            totalDogs.setText("Total dogs in the community: " + dogsCount.get());
                                                        }
                                                    }
                                                }
                                        );
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e("MainActivity", "Error fetching users in community", e);
                                }
                            }
                    );
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
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE
                );
            } else {
                mapController.initializeMap(savedInstanceState);
            }
        }

        // === Focus on specific community area ===
        String focusCommunity = getIntent().getStringExtra(EXTRA_FOCUS_COMMUNITY_NAME);
        int focusRadius = getIntent().getIntExtra(EXTRA_FOCUS_RADIUS, 1500);

        if (focusCommunity != null && !focusCommunity.isEmpty()) {
            CommunityRepository communityRepo = new CommunityRepository();
            communityRepo.getCommunityCenterAndRadiusByName(
                    focusCommunity,
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
                    }
            );
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

        // ⚙️ RTL support — אחרי שה-View נטען לגמרי
        drawerMotion.post(() -> {
            boolean isRtl = ViewCompat.getLayoutDirection(root) == ViewCompat.LAYOUT_DIRECTION_RTL;
            final int CLOSED_ID = isRtl ? R.id.closed_rtl : R.id.closed_ltr;
            final int OPEN_ID = isRtl ? R.id.open_rtl : R.id.open_ltr;
            final int TRANS_ID = isRtl ? R.id.t_rtl : R.id.t_ltr;

            drawerMotion.setTransition(TRANS_ID);
            drawerMotion.setState(CLOSED_ID, -1, -1);

            // כיוון ומיקום מדויק
            ViewCompat.setLayoutDirection(drawerMotion, ViewCompat.LAYOUT_DIRECTION_LOCALE);
            ViewCompat.setLayoutDirection(menuButton, ViewCompat.LAYOUT_DIRECTION_LOCALE);
            menuButton.setRotationY(isRtl ? 180f : 0f);

            // 🔹 TransitionListener – כדי לסגור את ה-BottomSheet כשפותחים מגירה
            drawerMotion.setTransitionListener(new MotionLayout.TransitionListener() {
                @Override
                public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
                    // אם המגירה נפתחת – סגור את ה-BottomSheet
                    if (endId == OPEN_ID && mainMotion.getProgress() > 0f) {
                        mainMotion.transitionToStart();
                    }
                }

                @Override
                public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {}

                @Override
                public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {}

                @Override
                public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {}
            });

            // 🔹 לחיצה על הכפתור פותחת/סוגרת את המגירה, מסונכרן עם BottomSheet
            menuButton.setOnClickListener(v -> {
                boolean isBottomSheetOpen = mainMotion.getProgress() > 0.3f;
                boolean wantOpen = drawerMotion.getCurrentState() != OPEN_ID;

                if (isBottomSheetOpen) {
                    // אם ה-BottomSheet פתוח – סגור אותו לפני פתיחת המגירה
                    mainMotion.transitionToStart();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        drawerMotion.transitionToState(wantOpen ? OPEN_ID : CLOSED_ID);
                    }, 300);
                } else {
                    drawerMotion.transitionToState(wantOpen ? OPEN_ID : CLOSED_ID);
                }
            });

            // 🔹 לחיצה על overlay סוגרת את המגירה
            overlay.setOnClickListener(v -> drawerMotion.transitionToState(CLOSED_ID));
        });

        // === BottomSheet ===
        bottomSheet.setOnTouchListener((v, event) -> {
            // 🧱 מניעת גרירה כשמגירה פתוחה
            boolean isRtl = ViewCompat.getLayoutDirection(root) == ViewCompat.LAYOUT_DIRECTION_RTL;
            final int OPEN_ID = isRtl ? R.id.open_rtl : R.id.open_ltr;
            if (drawerMotion.getCurrentState() == OPEN_ID) {
                return true; // אל תאפשר תנועה של הבוטום שיט
            }

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

        // === ניווט בתוך המגירה ===
        findViewById(R.id.communityButtonContainer).setOnClickListener(v -> {
            if (currentUser != null) {
                Intent intent;
                if (currentUser.isManager()) {
                    intent = new Intent(MainActivity.this, ManagerCommunityActivity.class);
                } else {
                    intent = new Intent(MainActivity.this, CommunityActivity.class);
                }
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
            }
        });

        findViewById(R.id.newReportButtonContainer).setOnClickListener(v -> openReportMapPicker());

        findViewById(R.id.myProfileButton).setOnClickListener(v -> {
            boolean isRtl = ViewCompat.getLayoutDirection(root) == ViewCompat.LAYOUT_DIRECTION_RTL;
            drawerMotion.transitionToState(isRtl ? R.id.closed_rtl : R.id.closed_ltr);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
            }, 300);
        });

        findViewById(R.id.settingsButton).setOnClickListener(v -> {
            boolean isRtl = ViewCompat.getLayoutDirection(root) == ViewCompat.LAYOUT_DIRECTION_RTL;
            drawerMotion.transitionToState(isRtl ? R.id.closed_rtl : R.id.closed_ltr);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
            }, 300);
        });

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            boolean isRtl = ViewCompat.getLayoutDirection(root) == ViewCompat.LAYOUT_DIRECTION_RTL;
            drawerMotion.transitionToState(isRtl ? R.id.closed_rtl : R.id.closed_ltr);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }, 300);
        });
    }

    // === Report Map Picker ===
    private final ActivityResultLauncher<Intent> mapReportPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
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
    @Override
    protected void onResume() {
        super.onResume();
        if (mapController != null) mapController.onResume();

        // === שמירה על RTL גם אחרי שינוי שפה או חזרה מהמסכים האחרים ===
        View root = findViewById(R.id.rootLayout);
        boolean isRtl = ViewCompat.getLayoutDirection(root) == ViewCompat.LAYOUT_DIRECTION_RTL;
        View drawerMotion = findViewById(R.id.drawerMotionLayout);
        View menuButton = findViewById(R.id.imageButton);
        ViewCompat.setLayoutDirection(drawerMotion, ViewCompat.LAYOUT_DIRECTION_LOCALE);
        ViewCompat.setLayoutDirection(menuButton, ViewCompat.LAYOUT_DIRECTION_LOCALE);
        menuButton.setRotationY(isRtl ? 180f : 0f);
    }

    @Override
    protected void onPause() {
        if (mapController != null) mapController.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mapController != null) mapController.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapController != null) mapController.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapController != null) mapController.onSaveInstanceState(outState);
    }
}