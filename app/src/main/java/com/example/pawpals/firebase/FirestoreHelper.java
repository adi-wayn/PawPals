package com.example.pawpals.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.pawpals.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class FirestoreHelper {
    private static final String TAG = "FirestoreHelper";
    private final FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void createUserProfile(String userId, User user, FirestoreCallback callback) {
        Map<String, Object> userMap = user.toMap();

        db.collection("users")
                .document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created for: " + userId);
                    callback.onSuccess(userId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user profile", e);
                    callback.onFailure(e);
                });
    }


    public interface FirestoreCallback {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }
}
