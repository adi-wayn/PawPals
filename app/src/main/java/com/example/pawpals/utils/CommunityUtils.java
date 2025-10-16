package com.example.pawpals.utils;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

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

    /**
     * Load nearby communities into a Spinner.
     */
    public static void loadNearbyCommunities(Context context, double lat, double lng, Spinner spinner) {
        MapRepository mapRepo = new MapRepository();

        mapRepo.getNearbyCommunities(lat, lng, 5000, new MapRepository.FirestoreNearbyCommunitiesCallback() {
            @Override
            public void onSuccess(List<String> nearbyCommunityNames) {
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
                Toast.makeText(context, "Failed to load nearby communities", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Toggles between community Spinner (join existing) and input field (create new).
     */
    public static void setupCommunitySelection(
            CheckBox checkboxCreateCommunity,
            EditText inputCommunity,
            Spinner spinnerCommunities
    ) {
        checkboxCreateCommunity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            inputCommunity.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            spinnerCommunities.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });
    }

    /**
     * Checks if a community exists; if not, optionally creates it, then saves the user.
     */
    public static void saveUserAndCommunity(Context context, String name, String contactDetails, String bio,
                                            String communityName, String userId, boolean isManager,
                                            double lat, double lng) {

        db.collection("communities")
                .document(communityName)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean exists = snapshot.exists();

                    if (exists && isManager) {
                        Toast.makeText(context, "Community already exists!", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (!exists && !isManager) {
                        Toast.makeText(context, "Community doesn't exist. Check 'create' to create it.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    CommunityRepository communityRepository = new CommunityRepository();

                    if (isManager && !exists) {
                        // ✅ Create community first
                        communityRepository.createCommunity(communityName, userId, lat, lng, "", "", new ArrayList<>(),
                                new CommunityRepository.FirestoreCallback() {
                                    @Override
                                    public void onSuccess(String id) {
                                        saveUserAndNavigate(context, name, contactDetails, bio, communityName, userId, true, lat, lng);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(context, "Failed to create community", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        saveUserAndNavigate(context, name, contactDetails, bio, communityName, userId, isManager, lat, lng);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error checking community", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Saves a user and navigates to the main screen upon success.
     */
    private static void saveUserAndNavigate(Context context, String name, String contactDetails, String bio,
                                            String communityName, String userId, boolean isManager,
                                            double lat, double lng) {

        UserRepository userRepository = new UserRepository();

        User user = isManager
                ? new CommunityManager(name, communityName, contactDetails, bio)
                : new User(name, communityName, contactDetails, bio);

        user.setIsManager(isManager);

        userRepository.createUserProfile(userId, user, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String documentId) {
                Toast.makeText(context, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();

                // ✅ Navigate to main screen
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Failed to save user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Creates a new community, marks the creator as manager, and updates their profile.
     */
    public static void createCommunityAndUpdateUser(
            Context context,
            String communityName,
            String managerId,
            double lat,
            double lng,
            Map<String, Object> updates,
            Consumer<String> onSuccess,
            Consumer<String> onFailure
    ) {
        CommunityRepository repo = new CommunityRepository();
        UserRepository userRepo = new UserRepository();

        repo.createCommunity(
                communityName,
                managerId,
                lat,
                lng,
                "",
                "",
                new ArrayList<>(),
                new CommunityRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess(String newCommunityId) {
                        // ✅ Ensure user becomes manager and community is linked
                        updates.put("communityName", communityName);
                        updates.put("isManager", true);

                        userRepo.updateUserProfile(managerId, updates, new UserRepository.FirestoreCallback() {
                            @Override
                            public void onSuccess(String id) {
                                onSuccess.accept("Created new community: " + communityName);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                onFailure.accept("Community created, but failed to update user: " + e.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        onFailure.accept("Failed to create community: " + e.getMessage());
                    }
                }
        );
    }
}
