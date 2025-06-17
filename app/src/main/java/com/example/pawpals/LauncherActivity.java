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
            String userId = currentUser.getUid();

            userRepository.checkIfUserProfileExists(userId, new UserRepository.FirestoreExistCallback() {
                @Override
                public void onResult(boolean exists) {
                    if (exists) {
                        // נטען את האובייקט User ואז נעבור ל־MainActivity
                        userRepository.getUserById(userId, new UserRepository.FirestoreUserCallback() {
                            @Override
                            public void onSuccess(model.User user) {
                                if (user != null) {
                                    Intent intent = new Intent(LauncherActivity.this, MainActivity.class);
                                    intent.putExtra("currentUser", user);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LauncherActivity.this, "User data is null", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LauncherActivity.this, LoginActivity.class));
                                    finish();
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(LauncherActivity.this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LauncherActivity.this, LoginActivity.class));
                                finish();
                            }
                        });
                    } else {
                        startActivity(new Intent(LauncherActivity.this, RegisterActivity.class));
                        finish();
                    }
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
