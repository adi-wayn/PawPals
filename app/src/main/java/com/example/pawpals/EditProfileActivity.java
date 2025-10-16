package com.example.pawpals;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Pair;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.pawpals.utils.CommunityManagerUtils;
import com.example.pawpals.utils.CommunityUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private String currentUserCommunity = null;

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
                    currentUserCommunity = user.getCommunityName();
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.widget.Toast.makeText(EditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
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
        updates.put("userName", name);
        updates.put("contactDetails", contactDetails);
        updates.put("fieldsOfInterest", fieldsOfInterest);

        String newCommunity = spinnerCommunities.getSelectedItem() != null
                ? spinnerCommunities.getSelectedItem().toString()
                : safeText(inputNewCommunity);

        if (newCommunity.isEmpty()) {
            Toast.makeText(this, "Please select or enter a community", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isManager && currentUserCommunity != null && !newCommunity.equals(currentUserCommunity)) {
            startManagerTransferBeforeCommunityChange(newCommunity, updates, currentUserCommunity);
        } else {
            updateUserProfile(updates, "Profile updated!");
        }
    }

    private void startManagerTransferBeforeCommunityChange(String newCommunity, Map<String, Object> updates, String currentCommunity) {
        UserRepository repo = new UserRepository();
        repo.getUsersByCommunityWithIds(currentCommunity, new UserRepository.FirestoreUsersWithIdsCallback() {
            @Override
            public void onSuccess(List<Pair<String, User>> rows) {
                List<String> memberNames = new ArrayList<>();
                List<String> memberIds = new ArrayList<>();

                for (Pair<String, User> pair : rows) {
                    if (pair.first.equals(userId)) continue; // skip self
                    memberIds.add(pair.first);
                    memberNames.add(pair.second.getUserName() != null ? pair.second.getUserName() : "(no name)");
                }

                if (memberNames.isEmpty()) {
                    Toast.makeText(EditProfileActivity.this,
                            "You must assign a new manager before changing communities.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
                builder.setTitle("Select New Manager");

                Spinner spinner = new Spinner(EditProfileActivity.this);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(EditProfileActivity.this,
                        android.R.layout.simple_spinner_item, memberNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                builder.setView(spinner);

                builder.setPositiveButton("Confirm", (dialog, which) -> {
                    int pos = spinner.getSelectedItemPosition();
                    if (pos == Spinner.INVALID_POSITION) {
                        Toast.makeText(EditProfileActivity.this, "Please select a member.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newManagerUid = memberIds.get(pos);

                    CommunityManagerUtils.transferManager(
                            EditProfileActivity.this,
                            currentCommunity,
                            newManagerUid,
                            new CommunityManagerUtils.TransferCallback() {
                                @Override
                                public void onSuccess() {
                                    updates.put("communityName", newCommunity);
                                    updateUserProfile(updates, "Profile updated!");
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(EditProfileActivity.this,
                                            "Failed to transfer manager role: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                builder.show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditProfileActivity.this, "Failed to load community members.", Toast.LENGTH_SHORT).show();
            }
        });
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
