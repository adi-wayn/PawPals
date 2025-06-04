package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pawpals.firebase.AuthHelper;
import com.example.pawpals.firebase.FirestoreHelper;
import com.google.firebase.auth.FirebaseUser;

public class LauncherActivity extends AppCompatActivity {

    private final AuthHelper authHelper = new AuthHelper();
    private final FirestoreHelper firestoreHelper = new FirestoreHelper();

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
            firestoreHelper.checkIfUserProfileExists(userId, new FirestoreHelper.FirestoreExistCallback() {
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
