package com.example.pawpals.firebase;

import android.util.Log;

import model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
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

    public void createCommunity(String communityName, String createdByUserId, FirestoreCallback callback) {
        Map<String, Object> communityData = new HashMap<>();
        communityData.put("name", communityName);
        communityData.put("createdBy", createdByUserId);

        db.collection("communities")
                .document(communityName)
                .set(communityData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Community created: " + communityName);
                    callback.onSuccess(communityName);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to create community", e);
                    callback.onFailure(e);
                });
    }

    public void checkIfUserProfileExists(String userId, FirestoreExistCallback callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> callback.onResult(snapshot.exists()))
                .addOnFailureListener(callback::onError);
    }

    public interface FirestoreExistCallback {
        void onResult(boolean exists);
        void onError(Exception e);
    }


    public interface FirestoreCallback {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }
}
