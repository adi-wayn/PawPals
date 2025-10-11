package com.example.pawpals;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
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

        loadCurrentUser();
        requestLocationAndLoadCommunities();

        buttonSave.setOnClickListener(v -> handleProfileUpdate());
    }

    private void initViews() {
        inputName = findViewById(R.id.input_name);
        inputContactDetails = findViewById(R.id.input_contact_details);
        inputFieldsOfInterest = findViewById(R.id.input_fields_of_interest);
        inputNewCommunity = findViewById(R.id.input_new_community);
        spinnerCommunities = findViewById(R.id.spinner_communities);
        buttonSave = findViewById(R.id.button_save);
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
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();

                // ✅ Load nearby communities through CommunityUtils
                CommunityUtils.loadNearbyCommunities(this, currentLat, currentLng, spinnerCommunities);
            } else {
                Toast.makeText(this, "Unable to get location. Try again.", Toast.LENGTH_SHORT).show();
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
        if (!name.isEmpty()) updates.put("userName", name);
        if (!contactDetails.isEmpty()) updates.put("contactDetails", contactDetails);
        if (!fieldsOfInterest.isEmpty()) updates.put("fieldsOfInterest", fieldsOfInterest);

        // ✅ If manager, just update fields
        if (isManager) {
            updateUserProfile(updates, "Profile updated!");
            return;
        }

        // ✅ If non-manager: check for selected or new community
        if (spinnerCommunities.getSelectedItem() != null) {
            String selectedCommunity = spinnerCommunities.getSelectedItem().toString();

            CommunityUtils.saveUserAndCommunity(
                    this,
                    name,
                    contactDetails,
                    fieldsOfInterest,
                    selectedCommunity,
                    userId,
                    false,  // not creating new
                    currentLat,
                    currentLng
            );
        } else {
            String newCommunityName = safeText(inputNewCommunity);
            if (newCommunityName.isEmpty()) {
                Toast.makeText(this, "Please enter a community name", Toast.LENGTH_SHORT).show();
                return;
            }

            CommunityUtils.saveUserAndCommunity(
                    this,
                    name,
                    contactDetails,
                    fieldsOfInterest,
                    newCommunityName,
                    userId,
                    true,   // creating new
                    currentLat,
                    currentLng
            );
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
