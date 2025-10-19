package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import model.User;
import model.firebase.Firestore.UserRepository;

public class SettingsActivity extends AppCompatActivity {

    private TextView textUserName;
    private final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize views
        textUserName = findViewById(R.id.text_user_name);
        Button deleteButton = findViewById(R.id.button_delete_account);

        // Load user's name and manager status
        userRepository.getUserById(userId, new UserRepository.FirestoreUserCallback() {
            @Override
            public void onSuccess(User user) {
                // Set username
                if (user != null && user.getUserName() != null && !user.getUserName().isEmpty()) {
                    textUserName.setText(user.getUserName());
                } else {
                    textUserName.setText("User");
                }

                // Hide delete button for managers
                if (user != null && user.isManager()) {
                    deleteButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SettingsActivity.this, "Failed to load username", Toast.LENGTH_SHORT).show();
                textUserName.setText("User");
            }
        });

        // Back button
        Button backButton = findViewById(R.id.button_back);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Privacy Policy
        findViewById(R.id.button_privacy_policy).setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, PrivacyPolicyActivity.class))
        );

        // Edit Profile
        findViewById(R.id.button_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, EditProfileActivity.class))
        );

        // Edit Dogs
        findViewById(R.id.button_edit_dogs).setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, EditDogsActivity.class))
        );

        // Alerts
        findViewById(R.id.button_alerts).setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, AlertsActivity.class))
        );

        // Delete Account (only visible if not a manager)
        deleteButton.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, DeleteAccountActivity.class))
        );
    }


    private void loadUserName() {
        userRepository.getUserById(userId, new UserRepository.FirestoreUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.getUserName() != null && !user.getUserName().isEmpty()) {
                    textUserName.setText(user.getUserName());
                } else {
                    textUserName.setText("User");
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SettingsActivity.this, "Failed to load username", Toast.LENGTH_SHORT).show();
                textUserName.setText("User");
            }
        });
    }
}
