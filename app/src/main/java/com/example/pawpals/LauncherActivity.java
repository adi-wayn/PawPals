package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import model.firebase.AuthHelper;
import model.firebase.UserRepository;
import com.google.firebase.auth.FirebaseUser;

public class LauncherActivity extends AppCompatActivity {

    private final AuthHelper authHelper = new AuthHelper();
    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = authHelper.getCurrentUser();

        if (currentUser == null) {
            // לא מחובר – מעבר למסך התחברות
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            // מחובר – נבדוק אם יש לו פרופיל
            String userId = currentUser.getUid();
            userRepository.checkIfUserProfileExists(userId, new UserRepository.FirestoreExistCallback() {
                @Override
                public void onResult(boolean exists) {
                    Intent intent = exists
                            ? new Intent(LauncherActivity.this, MainActivity.class)
                            : new Intent(LauncherActivity.this, RegistrationDetailsActivity.class);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(LauncherActivity.this, "Error checking profile", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LauncherActivity.this, LoginActivity.class));
                    finish();
                }
            });
        }
    }
}
