package model.firebase.Authentication;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthHelper {
    private static final String TAG = "AuthHelper";
    private final FirebaseAuth mAuth;

    public AuthHelper() {
        mAuth = FirebaseAuth.getInstance();
    }

    public void registerUser(String email, String password, Activity activity, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        callback.onSuccess(user);
                    } else {
                        Log.w(TAG, "Registration failed", task.getException());
                        Toast.makeText(activity, "Registration failed", Toast.LENGTH_SHORT).show();
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void loginUser(String email, String password, Activity activity, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        callback.onSuccess(user);
                    } else {
                        Log.w(TAG, "Login failed", task.getException());
                        Toast.makeText(activity, "Login failed", Toast.LENGTH_SHORT).show();
                        callback.onFailure(task.getException());
                    }
                });
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public void signOut() {
        mAuth.signOut();
    }

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }
}
