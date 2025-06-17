package com.example.pawpals;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import model.User;

public class MainActivity extends AppCompatActivity {
    private com.google.android.gms.maps.MapView mapView;
    private com.google.android.gms.maps.GoogleMap googleMap;
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
        User user = getIntent().getParcelableExtra("currentUser");
        Log.d("MainActivity", "user = " + user);
        Log.d("MainActivity", "user community = " + user.getCommunityName());
        Log.d("MainActivity", "user type = " + user.getClass().getSimpleName());

        Log.d("API_KEY_TEST", getString(R.string.maps_api_key));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            googleMap = map;

            // תצורת UI של המפה
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

            // מיקום פתיחה – לדוגמה: תל אביב
            com.google.android.gms.maps.model.LatLng telAviv = new com.google.android.gms.maps.model.LatLng(32.0853, 34.7818);
            googleMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(telAviv, 13f));
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
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

}
