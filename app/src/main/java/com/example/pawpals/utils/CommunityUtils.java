package com.example.pawpals.utils;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;

import com.example.pawpals.CommunityCreationDetailsActivity;
import com.example.pawpals.MainActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import model.CommunityManager;
import model.User;
import model.firebase.Firestore.CommunityRepository;
import model.firebase.Firestore.MapRepository;
import model.firebase.Firestore.UserRepository;

/**
 * Utility class for managing community-related operations
 * (creation, user-community linking, nearby spinner, etc.).
 */
public class CommunityUtils {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "CommunityUtils";

    public static void loadNearbyCommunities(Context context, double lat, double lng, Spinner spinner) {
        Log.d(TAG, "Loading nearby communities for lat=" + lat + ", lng=" + lng);
        MapRepository mapRepo = new MapRepository();

        mapRepo.getNearbyCommunities(lat, lng, 5000, new MapRepository.FirestoreNearbyCommunitiesCallback() {
            @Override
            public void onSuccess(List<String> nearbyCommunityNames) {
                Log.d(TAG, "Nearby communities loaded: " + nearbyCommunityNames);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_spinner_item,
                        nearbyCommunityNames.isEmpty()
                                ? new ArrayList<>(List.of("No nearby communities"))
                                : nearbyCommunityNames
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "Failed to load nearby communities", e);
                Toast.makeText(context, "Failed to load nearby communities", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void saveUserAndCommunity(Context context, String name, String contactDetails, String bio,
                                            String communityName, String userId, boolean isManager,
                                            double lat, double lng) {

        Log.d(TAG, "saveUserAndCommunity called with communityName=" + communityName + ", isManager=" + isManager);

        db.collection("communities")
                .document(communityName)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean exists = snapshot.exists();
                    Log.d(TAG, "Community exists: " + exists);

                    if (exists && isManager) {
                        Log.d(TAG, "Community already exists, cannot create as manager");
                        Toast.makeText(context, "Community already exists!", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (!exists && !isManager) {
                        Log.d(TAG, "Community doesn't exist, cannot join");
                        Toast.makeText(context, "Community doesn't exist. Check 'create' to create it.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    CommunityRepository communityRepository = new CommunityRepository();

                    if (isManager && !exists) {
                        Log.d(TAG, "Creating new community: " + communityName);
                        communityRepository.createCommunity(communityName, userId, lat, lng, "", "", new ArrayList<>(),
                                new CommunityRepository.FirestoreCallback() {
                                    @Override
                                    public void onSuccess(String id) {
                                        Log.d(TAG, "Community created successfully: " + id);
                                        saveUserAndNavigate(context, name, contactDetails, bio, communityName, userId, true, lat, lng);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.d(TAG, "Failed to create community", e);
                                        Toast.makeText(context, "Failed to create community", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Log.d(TAG, "Joining existing community or saving user without creating");
                        saveUserAndNavigate(context, name, contactDetails, bio, communityName, userId, isManager, lat, lng);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Error checking community existence", e);
                    Toast.makeText(context, "Error checking community", Toast.LENGTH_SHORT).show();
                });
    }

    private static void saveUserAndNavigate(Context context, String name, String contactDetails, String bio,
                                            String communityName, String userId, boolean isManager,
                                            double lat, double lng) {

        Log.d(TAG, "saveUserAndNavigate called for user=" + name + ", community=" + communityName + ", isManager=" + isManager);
        UserRepository userRepository = new UserRepository();

        User user = isManager
                ? new CommunityManager(name, communityName, contactDetails, bio)
                : new User(name, communityName, contactDetails, bio);

        user.setIsManager(isManager);

        userRepository.createUserProfile(userId, user, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String documentId) {
                Log.d(TAG, "User saved successfully: " + documentId);
                Toast.makeText(context, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();

                Intent intent;

                if (isManager) {
                    // If the user created a new community, go to the setup screen
                    intent = new Intent(context, CommunityCreationDetailsActivity.class);
                    intent.putExtra("communityName", communityName);
                    intent.putExtra("lat", lat);
                    intent.putExtra("lng", lng);
                    intent.putExtra("userId", userId);
                    intent.putExtra("userName", name);
                    intent.putExtra("contactDetails", contactDetails);
                    intent.putExtra("bio", bio);

                } else {
                    // If the user just joined an existing community, go back to main
                    intent = new Intent(context, MainActivity.class);
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);

            }

            @Override
            public void onFailure(Exception e) {
                Log.d(TAG, "Failed to save user data", e);
                Toast.makeText(context, "Failed to save user data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}