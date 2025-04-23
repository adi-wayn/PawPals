package com.example.pawpals;

import android.annotation.SuppressLint;
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MotionLayout motionLayout = findViewById(R.id.main);
        View bottomSheet = findViewById(R.id.bottomSheet);

        bottomSheet.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchY = event.getRawY();
                    lastProgress = motionLayout.getProgress();
                    isDragging = true;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        float dy = initialTouchY - event.getRawY();
                        float deltaProgress = dy / 500f;
                        float newProgress = lastProgress + deltaProgress;
                        newProgress = Math.max(0f, Math.min(1f, newProgress));
                        motionLayout.setProgress(newProgress);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDragging = false;
                    if (motionLayout.getProgress() > 0.5f) {
                        motionLayout.transitionToEnd();
                    } else {
                        motionLayout.transitionToStart();
                    }
                    return true;
            }
            return false;
        });
    }
}
