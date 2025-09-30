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
import model.firebase.firestore.UserRepository;

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
        String communityName = safeText(inputCommunity);

        Map<String, Object> updates = new HashMap<>();
        if (!name.isEmpty()) updates.put("userName", name);
        if (!contactDetails.isEmpty()) updates.put("contactDetails", contactDetails);
        if (!fieldsOfInterest.isEmpty()) updates.put("fieldsOfInterest", fieldsOfInterest);

        if (!isManager && !communityName.isEmpty()) {
            updates.put("communityName", communityName);
        }

        if (updates.isEmpty()) {
            Toast.makeText(this, "Nothing to update", Toast.LENGTH_SHORT).show();
            return;
        }

        userRepository.updateUserProfile(userId, updates, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String id) {
                Toast.makeText(EditProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
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
