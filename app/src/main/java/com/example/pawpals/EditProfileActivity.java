package com.example.pawpals;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

import model.User;
import model.firebase.Firestore.UserRepository;

public class EditProfileActivity extends AppCompatActivity {

    private EditText inputName, inputContactDetails, inputFieldsOfInterest, inputCommunity;
    private MaterialButton buttonSave;

    private final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private final UserRepository userRepository = new UserRepository();

    private boolean isManager = false; // track admin status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        inputName = findViewById(R.id.input_name);
        inputContactDetails = findViewById(R.id.input_contact_details);
        inputFieldsOfInterest = findViewById(R.id.input_fields_of_interest); // instead of bio
        inputCommunity = findViewById(R.id.input_community);
        buttonSave = findViewById(R.id.button_save);

        // Load current user data from Firestore
        loadCurrentUser();

        buttonSave.setOnClickListener(v -> handleProfileUpdate());
    }

    private void loadCurrentUser() {
        userRepository.getUserById(userId, new UserRepository.FirestoreUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    inputName.setText(user.getUserName());
                    inputContactDetails.setText(user.getContactDetails());
                    inputFieldsOfInterest.setText(user.getFieldsOfInterest());
                    inputCommunity.setText(user.getCommunityName());

                    isManager = user.isManager();

                    // Lock community editing if user is a manager
                    if (isManager) {
                        inputCommunity.setEnabled(false);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleProfileUpdate() {
        String name = safeText(inputName);
        String contactDetails = safeText(inputContactDetails);
        String fieldsOfInterest = safeText(inputFieldsOfInterest);
        String communityNameInput = safeText(inputCommunity);

        Map<String, Object> updates = new HashMap<>();
        if (!name.isEmpty()) updates.put("userName", name);
        if (!contactDetails.isEmpty()) updates.put("contactDetails", contactDetails);
        if (!fieldsOfInterest.isEmpty()) updates.put("fieldsOfInterest", fieldsOfInterest);

        if (!isManager && !communityNameInput.isEmpty()) {
            // Use user's current location (replace with actual GPS)
            double userLat = ...;
            double userLng = ...;
            int radiusMeters = 5000; // 5 km radius

            CommunityRepository communityRepo = new CommunityRepository();

            // 1️⃣ Try to find a nearby community first
            communityRepo.findCommunityNearby(userLat, userLng, radiusMeters, new CommunityRepository.FirestoreCommunityCallback() {
                @Override
                public void onSuccess(Community community) {
                    // Found a nearby community → join it
                    updates.put("communityName", community.getName());
                    updateUserProfile(updates, "Joined existing community nearby!");
                }

                @Override
                public void onFailure(Exception e) {
                    // No nearby community → create new
                    communityRepo.createCommunity(
                            communityNameInput,
                            userId, // user becomes manager
                            userLat,
                            userLng,
                            new ArrayList<>(),
                            new CommunityRepository.FirestoreCallback() {
                                @Override
                                public void onSuccess(String newCommunityId) {
                                    updates.put("communityName", newCommunityId);
                                    updateUserProfile(updates, "New community created!");
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(EditProfileActivity.this, "Failed to create community", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }
            });

        } else {
            if (!updates.isEmpty()) {
                updateUserProfile(updates, "Profile updated!");
            } else {
                Toast.makeText(this, "Nothing to update", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUserProfile(Map<String, Object> updates, String successMsg) {
        userRepository.updateUserProfile(userId, updates, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String id) {
                Toast.makeText(EditProfileActivity.this, successMsg, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditProfileActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private String safeText(EditText et) {
        return et == null || et.getText() == null ? "" : et.getText().toString().trim();
    }
}
