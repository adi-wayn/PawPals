package com.example.pawpals;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private float initialTouchY = 0;
    private float lastProgress = 0f;
    private boolean isDragging = false;
    private boolean isDrawerOpen = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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
            Intent intent = new Intent(MainActivity.this, CommunityActivity.class);
            startActivity(intent);
        });

        View newReportButton = findViewById(R.id.newReportButtonContainer);
        newReportButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportMapActivity.class);
            startActivity(intent);
        });


    }
}
