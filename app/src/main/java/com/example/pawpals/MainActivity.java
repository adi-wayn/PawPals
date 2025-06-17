package com.example.pawpals;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import model.User;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private float initialTouchY = 0;
    private float lastProgress = 0f;
    private boolean isDragging = false;
    private boolean isDrawerOpen = false;
    private User currentUser;
    private GoogleMap mMap;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        currentUser = getIntent().getParcelableExtra("currentUser");

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

        // אתחול המפה
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // גרירת BottomSheet
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

        // פתיחה וסגירת תפריט צד
        menuButton.setOnClickListener(v -> {
            if (!isDrawerOpen) {
                drawerMotion.transitionToState(R.id.open);
                overlay.setVisibility(View.VISIBLE);
                isDrawerOpen = true;
                menuButton.setVisibility(View.GONE);
                mainMotion.transitionToStart();
            } else {
                drawerMotion.transitionToState(R.id.closed);
                overlay.setVisibility(View.GONE);
                isDrawerOpen = false;
                menuButton.setVisibility(View.VISIBLE);
            }
        });

        overlay.setOnClickListener(v -> {
            drawerMotion.transitionToState(R.id.closed);
            overlay.setVisibility(View.GONE);
            isDrawerOpen = false;
            menuButton.setVisibility(View.VISIBLE);
        });

        View communityCard = findViewById(R.id.communityButtonContainer);
        communityCard.setOnClickListener(v -> {
            Intent intent;
            if (currentUser.isManager()) {
                intent = new Intent(MainActivity.this, ManagerCommunityActivity.class);
            } else {
                intent = new Intent(MainActivity.this, CommunityActivity.class);
            }
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        View newReportButton = findViewById(R.id.newReportButtonContainer);
        newReportButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportMapActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        View myProfileButton = findViewById(R.id.myProfileButton);
        myProfileButton.setOnClickListener(v -> {
            drawerMotion.transitionToState(R.id.closed);
            overlay.setVisibility(View.GONE);
            isDrawerOpen = false;
            menuButton.setVisibility(View.VISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
            }, 300);
        });

        View settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            drawerMotion.transitionToState(R.id.closed);
            overlay.setVisibility(View.GONE);
            isDrawerOpen = false;
            menuButton.setVisibility(View.VISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
            }, 300);
        });

        View logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            drawerMotion.transitionToState(R.id.closed);
            overlay.setVisibility(View.GONE);
            isDrawerOpen = false;
            menuButton.setVisibility(View.VISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }, 300);
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // תצוגת מיקום גאוגרפי לדוגמה
        LatLng telAviv = new LatLng(32.0853, 34.7818);
        mMap.addMarker(new MarkerOptions().position(telAviv).title("תל אביב"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(telAviv, 12));
    }
}
