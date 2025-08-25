package model.firebase.Authentication;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthHelper {
    private static final String TAG = "AuthHelper";
    private final FirebaseAuth mAuth;

    public AuthHelper() {
        mAuth = FirebaseAuth.getInstance();
    }

    // הרשמה עם אימייל+סיסמה
    public void registerUser(String email, String password, Activity activity, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(mAuth.getCurrentUser());
                    } else {
                        Log.w(TAG, "Registration failed", task.getException());
                        Toast.makeText(activity, "Registration failed", Toast.LENGTH_SHORT).show();
                        callback.onFailure(task.getException());
                    }
                });
    }

    // כניסה עם אימייל+סיסמה
    public void loginUser(String email, String password, Activity activity, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(mAuth.getCurrentUser());
                    } else {
                        Log.w(TAG, "Login failed", task.getException());
                        Toast.makeText(activity, "Login failed", Toast.LENGTH_SHORT).show();
                        callback.onFailure(task.getException());
                    }
                });
    }

    // *** חדש: כניסה/הרשמה עם Google (ללא fetchSignInMethodsForEmail) ***
    public void signInWithGoogleIdToken(String idToken, Activity activity, GoogleAuthCallback callback) {
        AuthCredential cred = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(cred)
                .addOnCompleteListener(activity, (Task<AuthResult> task) -> {
                    if (task.isSuccessful()) {
                        boolean isNew = task.getResult() != null
                                && task.getResult().getAdditionalUserInfo() != null
                                && task.getResult().getAdditionalUserInfo().isNewUser();
                        callback.onSuccess(mAuth.getCurrentUser(), isNew);
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            String email = ((FirebaseAuthUserCollisionException) e).getEmail();
                            callback.onCollision(email, cred); // נקשור אחרי כניסה בסיסמה
                        } else {
                            callback.onFailure(e);
                        }
                    }
                });
    }

    // *** חדש: קישור אישור (למשל Google) לחשבון המחובר ***
    public void linkWithCredential(AuthCredential credential, Activity activity, AuthCallback callback) {
        FirebaseUser current = mAuth.getCurrentUser();
        if (current == null) {
            callback.onFailure(new IllegalStateException("No signed-in user to link"));
            return;
        }
        current.linkWithCredential(credential)
                .addOnCompleteListener(activity, t -> {
                    if (t.isSuccessful()) {
                        callback.onSuccess(mAuth.getCurrentUser());
                    } else {
                        callback.onFailure(t.getException());
                    }
                });
    }

    public FirebaseUser getCurrentUser() { return mAuth.getCurrentUser(); }

    public void signOut() { mAuth.signOut(); }

    // ממשקים
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }

    public interface GoogleAuthCallback {
        void onSuccess(FirebaseUser user, boolean isNewUser);
        void onCollision(String email, AuthCredential pendingCredential);
        void onFailure(Exception e);
    }
}