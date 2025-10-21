package com.example.pawpals;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.pawpals.utils.CommunityUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

import model.User;
import model.firebase.Firestore.UserRepository;

public class EditProfileActivity extends AppCompatActivity {

    private EditText inputName, inputContactDetails, inputFieldsOfInterest, inputNewCommunity;
    private Spinner spinnerCommunities;
    private MaterialButton buttonSave;
    private TextView textCommunityLabel;
    private CheckBox checkboxCreateCommunity;



    private final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private final UserRepository userRepository = new UserRepository();

    private boolean isManager = false;

    private FusedLocationProviderClient locationClient;
    private double currentLat = 0, currentLng = 0;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initViews();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        checkboxCreateCommunity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            inputNewCommunity.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            spinnerCommunities.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });
        loadCurrentUser();
        requestLocationAndLoadCommunities();

        buttonSave.setOnClickListener(v -> handleProfileUpdate());
    }

    private void initViews() {
        inputName = findViewById(R.id.input_name);
        inputContactDetails = findViewById(R.id.input_contact_details);
        inputFieldsOfInterest = findViewById(R.id.input_fields_of_interest);
        inputNewCommunity = findViewById(R.id.input_new_community);
        textCommunityLabel = findViewById(R.id.text_community_label);
        spinnerCommunities = findViewById(R.id.spinner_communities);
        buttonSave = findViewById(R.id.button_save);
        checkboxCreateCommunity = findViewById(R.id.checkbox_create_community);
    }


    private void loadCurrentUser() {
        userRepository.getUserById(userId, new UserRepository.FirestoreUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    inputName.setText(user.getUserName());
                    inputContactDetails.setText(user.getContactDetails());
                    inputFieldsOfInterest.setText(user.getFieldsOfInterest());
                    isManager = user.isManager();

                    // 🔹 Hide community-related inputs entirely for managers
                    if (isManager) {
                        spinnerCommunities.setVisibility(View.GONE);
                        inputNewCommunity.setVisibility(View.GONE);
                        textCommunityLabel.setVisibility(View.GONE);
                        checkboxCreateCommunity.setVisibility(View.GONE); // ✅ hide the checkbox too
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void requestLocationAndLoadCommunities() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndLoadCommunities();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void fetchLocationAndLoadCommunities() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && !isManager) { // 🔹 Only load for non-managers
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                CommunityUtils.loadNearbyCommunities(this, currentLat, currentLng, spinnerCommunities);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndLoadCommunities();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleProfileUpdate() {
        String name = safeText(inputName);
        String contactDetails = safeText(inputContactDetails);
        String fieldsOfInterest = safeText(inputFieldsOfInterest);

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("userName", name);
        updates.put("contactDetails", contactDetails);
        updates.put("fieldsOfInterest", fieldsOfInterest);

        if (!isManager) {
            boolean wantsToCreate = checkboxCreateCommunity.isChecked();

            String newCommunity;
            if (wantsToCreate) {
                newCommunity = safeText(inputNewCommunity);
                if (newCommunity.isEmpty()) {
                    Toast.makeText(this, "Please enter a community name to create", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                if (spinnerCommunities.getSelectedItem() == null) {
                    Toast.makeText(this, "Please select a community", Toast.LENGTH_SHORT).show();
                    return;
                }
                newCommunity = spinnerCommunities.getSelectedItem().toString();
            }

            // Use CommunityUtils to validate/create community and update profile
            CommunityUtils.saveUserAndCommunity(
                    this,
                    name,
                    contactDetails,
                    fieldsOfInterest,
                    newCommunity,
                    userId,
                    wantsToCreate, // true if creating community
                    currentLat,
                    currentLng
            );


        } else {
            // Managers cannot change community
            updateUserProfile(updates, "Profile updated!");
        }
    }

    private void updateUserProfile(Map<String, Object> updates, String successMsg) {
        userRepository.updateUserProfile(userId, updates, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String id) {
                Toast.makeText(EditProfileActivity.this, successMsg, Toast.LENGTH_SHORT).show();

                // Navigate to MainActivity after successful update
                Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                // Optionally, you can pass the updated user object if needed
                userRepository.getUserById(userId, new UserRepository.FirestoreUserCallback() {
                    @Override
                    public void onSuccess(User updatedUser) {
                        intent.putExtra("currentUser", updatedUser);
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // fallback: just go to MainActivity without passing user
                        startActivity(intent);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditProfileActivity.this,
                        "Failed to update profile: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    private String safeText(EditText et) {
        return et == null || et.getText() == null ? "" : et.getText().toString().trim();
    }
}
