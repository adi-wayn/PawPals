package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import model.User;
import model.firebase.AuthHelper;
import model.firebase.UserRepository;

public class LauncherActivity extends AppCompatActivity {

    private static final String TAG = "LauncherActivity";
    private final AuthHelper authHelper = new AuthHelper();
    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = authHelper.getCurrentUser();

        if (currentUser == null) {
            Log.d(TAG, "No user signed in → LoginActivity");
            goToLogin();
            return;
        }

        Log.d(TAG, "Attempting reload for user: " + currentUser.getUid());

        currentUser.reload()
                .addOnSuccessListener(unused -> {
                    FirebaseUser refreshed = FirebaseAuth.getInstance().getCurrentUser();
                    if (refreshed == null) {
                        Log.w(TAG, "User invalid after reload → redirecting to Login");
                        FirebaseAuth.getInstance().signOut();
                        goToLogin();
                        return;
                    }

                    String userId = refreshed.getUid();
                    Log.d(TAG, "Reload successful → user ID: " + userId);
                    checkUserProfile(userId);

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "User reload failed: " + e.getMessage(), e);
                    FirebaseAuth.getInstance().signOut();
                    goToLogin();
                });
    }

    private void checkUserProfile(String userId) {
        userRepository.checkIfUserProfileExists(userId, new UserRepository.FirestoreExistCallback() {
            @Override
            public void onResult(boolean exists) {
                if (exists) {
                    Log.d(TAG, "User profile exists → loading user");
                    loadUserAndContinue(userId);
                } else {
                    Log.d(TAG, "User profile does not exist → RegisterActivity");
                    startActivity(new Intent(LauncherActivity.this, RegisterActivity.class));
                    finish();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking user profile: " + e.getMessage(), e);
                Toast.makeText(LauncherActivity.this, "Error checking profile", Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void loadUserAndContinue(String userId) {
        userRepository.getUserById(userId, new UserRepository.FirestoreUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    Log.d(TAG, "Loaded user: " + user.getClass().getSimpleName());
                    Log.d(TAG, "User data: " + user.toMap());

                    Intent intent = new Intent(LauncherActivity.this, MainActivity.class);
                    intent.putExtra("currentUser", user);
                    startActivity(intent);
                    finish();
                } else {
                    Log.e(TAG, "User loaded but is null → LoginActivity");
                    Toast.makeText(LauncherActivity.this, "User data is null", Toast.LENGTH_SHORT).show();
                    goToLogin();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load user profile: " + e.getMessage(), e);
                Toast.makeText(LauncherActivity.this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
