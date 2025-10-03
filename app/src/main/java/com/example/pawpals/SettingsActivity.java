package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Back button
        Button backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Privacy Policy
        findViewById(R.id.button_privacy_policy).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, PrivacyPolicyActivity.class));
        });

        // Edit Profile
        findViewById(R.id.button_edit_profile).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, EditProfileActivity.class));
        });

        // Edit Dogs
        findViewById(R.id.button_edit_dogs).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, EditDogsActivity.class));
        });

        // Alerts
        findViewById(R.id.button_alerts).setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, AlertsActivity.class));
        });
    }
}
