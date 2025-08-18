package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.TimeUnit;

import model.User;
import model.firebase.Authentication.AuthHelper;
import model.firebase.firestore.UserRepository;

public class LauncherActivity extends AppCompatActivity {
    private static final String TAG = "LauncherActivity";
    private final AuthHelper authHelper = new AuthHelper();
    private final UserRepository userRepository = new UserRepository();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean navigated = false;
    private final Runnable failSafe = () -> {
        if (!navigated) {
            Log.w(TAG, "Failsafe timeout → LoginActivity");
            goToLogin();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Splash (Android 12+ יופעל אוטומטית, בגרסאות קודמות פשוט יופיע ה־layout שהוגדר בערכת נושא)
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        // מסך רקע/Progress קטן כדי שלא יהיה לבן גם לפני splash API:
        setContentView(R.layout.activity_splash);

        // failsafe אחרי 8 שניות
        handler.postDelayed(failSafe, 8000);

        FirebaseUser currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user signed in → LoginActivity");
            navigateTo(LoginActivity.class);
            return;
        }

        Log.d(TAG, "Attempting reload for user: " + currentUser.getUid());

        // עטיפה עם טיימאאוט; אם נכשלה/נגמר הזמן – ממשיכים כרגיל
        Tasks.withTimeout(currentUser.reload(), 5, TimeUnit.SECONDS)
                .addOnCompleteListener(task -> {
                    FirebaseUser refreshed = FirebaseAuth.getInstance().getCurrentUser();
                    if (refreshed == null) {
                        Log.w(TAG, "User invalid after reload → LoginActivity");
                        FirebaseAuth.getInstance().signOut();
                        navigateTo(LoginActivity.class);
                        return;
                    }
                    checkUserProfile(refreshed.getUid());
                });
    }

    private void checkUserProfile(String userId) {
        userRepository.checkIfUserProfileExists(userId, new UserRepository.FirestoreExistCallback() {
            @Override public void onResult(boolean exists) {
                if (exists) {
                    loadUserAndContinue(userId);
                } else {
                    Log.d(TAG, "Profile missing → RegisterActivity");
                    navigateTo(RegisterActivity.class);
                }
            }
            @Override public void onError(Exception e) {
                Log.e(TAG, "Error checking profile", e);
                Toast.makeText(LauncherActivity.this, "Error checking profile", Toast.LENGTH_SHORT).show();
                navigateTo(LoginActivity.class);
            }
        });
    }

    private void loadUserAndContinue(String userId) {
        userRepository.getUserById(userId, new UserRepository.FirestoreUserCallback() {
            @Override public void onSuccess(User user) {
                if (user == null) {
                    Log.e(TAG, "User is null → LoginActivity");
                    Toast.makeText(LauncherActivity.this, "User data is null", Toast.LENGTH_SHORT).show();
                    navigateTo(LoginActivity.class);
                    return;
                }
                Intent i = new Intent(LauncherActivity.this, MainActivity.class);
                i.putExtra("currentUser", user);
                navigateTo(i);
            }
            @Override public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load user profile", e);
                Toast.makeText(LauncherActivity.this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                navigateTo(LoginActivity.class);
            }
        });
    }

    private void navigateTo(Class<?> cls) {
        navigateTo(new Intent(this, cls));
    }

    private void navigateTo(Intent intent) {
        if (navigated || isFinishing() || isDestroyed()) return;
        navigated = true;
        handler.removeCallbacks(failSafe);
        startActivity(intent);
        finish();
    }

    private void goToLogin() { navigateTo(LoginActivity.class); }
}
