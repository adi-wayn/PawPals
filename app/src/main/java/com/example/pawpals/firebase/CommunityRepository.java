package com.example.pawpals.firebase;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class CommunityRepository {
    private static final String TAG = "CommunityRepository";
    private final FirebaseFirestore db;

    public CommunityRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void createCommunity(String communityName, String managerUserId, FirestoreCallback callback) {
        Map<String, Object> communityData = new HashMap<>();
        communityData.put("name", communityName);
        communityData.put("managerId", managerUserId);

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

    public interface FirestoreExistCallback {
        void onResult(boolean exists);
        void onError(Exception e);
    }


    public interface FirestoreCallback {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }
}
