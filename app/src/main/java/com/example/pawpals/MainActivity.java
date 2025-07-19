package com.example.pawpals;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.MapView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import model.User;
import model.maps.MapController;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MapController mapController;
    private float initialTouchY = 0;
    private float lastProgress = 0f;
    private boolean isDragging = false;
    private boolean isDrawerOpen = false;
    private User currentUser;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = getIntent().getParcelableExtra("currentUser");

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        MapView mapView = findViewById(R.id.mapView);

        // שליפת מזהה משתמש נוכחי מ־FirebaseAuth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            String currentUserId = firebaseUser.getUid();
            mapController = new MapController(mapView, this, currentUserId);

            // בקשת הרשאה והפעלת המפה
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                mapController.initializeMap(savedInstanceState);
            }
        } else {
            Log.e("MainActivity", "FirebaseAuth.getCurrentUser() returned null. Cannot initialize map.");
            // אפשר להחזיר למסך התחברות או להציג שגיאה
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            mapController.initializeMap(savedInstanceState);
        }

        TextView labelInvisible = findViewById(R.id.labelInvisible);
        TextView labelVisible = findViewById(R.id.labelVisible);

        // מצב התחלה: משתמש נראה
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MotionLayout mainMotion = findViewById(R.id.main);
        MotionLayout drawerMotion = findViewById(R.id.drawerMotionLayout);
        View bottomSheet = findViewById(R.id.bottomSheet);
        View menuButton = findViewById(R.id.imageButton);
        View overlay = drawerMotion.findViewById(R.id.overlay);

        // Handle Bottom Sheet dragging
        bottomSheet.setOnTouchListener((v, event) -> {
            if (isDrawerOpen) return false;
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
                    if (mainMotion.getProgress() > 0.5f) {
                        mainMotion.transitionToEnd();
                    } else {
                        mainMotion.transitionToStart();
                    }
                    return true;
            }
            return false;
        });

        // Handle drawer open/close from hamburger menu
        menuButton.setOnClickListener(v -> {
            if (!isDrawerOpen) {
                drawerMotion.transitionToState(R.id.open);
                overlay.setVisibility(View.VISIBLE);
                isDrawerOpen = true;
                // Hide hamburger button and collapse bottom sheet
                menuButton.setVisibility(View.GONE);
                mainMotion.transitionToStart();
            } else {
                drawerMotion.transitionToState(R.id.closed);
                overlay.setVisibility(View.GONE);
                isDrawerOpen = false;
                // Restore hamburger button
                menuButton.setVisibility(View.VISIBLE);
            }
        });

        // Handle drawer close on overlay tap
        overlay.setOnClickListener(v -> {
            drawerMotion.transitionToState(R.id.closed);
            overlay.setVisibility(View.GONE);
            isDrawerOpen = false;
            // Restore hamburger button
            menuButton.setVisibility(View.VISIBLE);
        });

        View communityCard = findViewById(R.id.communityButtonContainer);
        communityCard.setOnClickListener(v -> {
            if (currentUser.isManager()) {
                // If the user is a manager, go to the community management screen
                Intent intent = new Intent(MainActivity.this, ManagerCommunityActivity.class);
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
            } else {
                // If the user is not a manager, go to the community activity screen
                Intent intent = new Intent(MainActivity.this, CommunityActivity.class);
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
            }
        });

        View newReportButton = findViewById(R.id.newReportButtonContainer);
        newReportButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportMapActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        View myProfileButton = findViewById(R.id.myProfileButton);
        myProfileButton.setOnClickListener(v -> {
            // סגירת התפריט
            drawerMotion.transitionToState(R.id.closed);
            overlay.setVisibility(View.GONE);
            isDrawerOpen = false;
            menuButton.setVisibility(View.VISIBLE);

            // מעבר לעמוד הפרופיל
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
            }, 300); // עיכוב של 300 מילישניות
        });

        View settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            // סגירת התפריט הצדדי
            drawerMotion.transitionToState(R.id.closed);
            overlay.setVisibility(View.GONE);
            isDrawerOpen = false;
            menuButton.setVisibility(View.VISIBLE);

            // מעבר לעמוד ההגדרות
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
            }, 300); // עיכוב של 300 מילישניות כדי לאפשר לאנימציית הסגירה לרוץ
        });

        View logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            // סגירת התפריט
            drawerMotion.transitionToState(R.id.closed);
            overlay.setVisibility(View.GONE);
            isDrawerOpen = false;
            menuButton.setVisibility(View.VISIBLE);

            // יציאה מהאפליקציה
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }, 300); // עיכוב של 300 מילישניות
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapController != null) mapController.onResume();
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
