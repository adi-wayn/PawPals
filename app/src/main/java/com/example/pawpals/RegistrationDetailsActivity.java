package com.example.pawpals;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import model.Community;
import model.CommunityManager;
import model.User;
import model.firebase.CommunityRepository;
import model.firebase.LocationRepository;
import model.firebase.UserRepository;

public class RegistrationDetailsActivity extends AppCompatActivity {

    private EditText inputName, inputCommunity;
    private CheckBox checkboxCreateCommunity;
    private MaterialButton buttonContinue;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // המשתמש הנוכחי
    private FusedLocationProviderClient locationClient;
    private LocationRepository locationRepo;
    private double currentLat = 0;
    private double currentLng = 0;
    private Spinner spinnerCommunities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_details);

        inputName = findViewById(R.id.input_name);
        inputCommunity = findViewById(R.id.input_community);
        checkboxCreateCommunity = findViewById(R.id.checkbox_create_community);
        buttonContinue = findViewById(R.id.button_continue);
        spinnerCommunities = findViewById(R.id.spinner_communities);

        buttonContinue.setOnClickListener(v -> handleRegistrationDetails());

        locationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRepo = new LocationRepository();

        // בקשת מיקום נוכחי
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    Log.d("Registration", "User location: " + currentLat + ", " + currentLng);
                }

                locationRepo.getNearbyCommunities(currentLat, currentLng, 5000, new LocationRepository.FirestoreNearbyCommunitiesCallback() {
                    @Override
                    public void onSuccess(List<String> nearbyCommunityIds) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                RegistrationDetailsActivity.this,
                                android.R.layout.simple_spinner_item,
                                nearbyCommunityIds
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerCommunities.setAdapter(adapter);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(RegistrationDetailsActivity.this, "Failed to load nearby communities", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }

        checkboxCreateCommunity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                inputCommunity.setVisibility(View.VISIBLE);
                spinnerCommunities.setVisibility(View.GONE);
            } else {
                inputCommunity.setVisibility(View.GONE);
                spinnerCommunities.setVisibility(View.VISIBLE);
            }
        });
    }

    private void handleRegistrationDetails() {
        String name = inputName.getText().toString().trim();
        boolean wantsToCreate = checkboxCreateCommunity.isChecked();
        String communityName;

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (wantsToCreate) {
            communityName = inputCommunity.getText().toString().trim();
            if (communityName.isEmpty()) {
                Toast.makeText(this, "Please enter a community name to create", Toast.LENGTH_SHORT).show();
                return;
            }

            // בדיקה רגילה אם הקהילה כבר קיימת
            checkCommunityExistence(name, communityName, true);

        } else {
            // אם המשתמש מצטרף לקהילה קיימת – נשתמש בשם מה־Spinner
            if (spinnerCommunities.getSelectedItem() == null) {
                Toast.makeText(this, "Please select a community", Toast.LENGTH_SHORT).show();
                return;
            }

            communityName = spinnerCommunities.getSelectedItem().toString();

            // נבדוק האם הקהילה הקרובה באמת קיימת בסביבה
            locationRepo.getNearbyCommunities(currentLat, currentLng, 5000, new LocationRepository.FirestoreNearbyCommunitiesCallback() {
                @Override
                public void onSuccess(List<String> nearbyCommunityIds) {
                    if (nearbyCommunityIds.contains(communityName)) {
                        checkCommunityExistence(name, communityName, false);
                    } else {
                        Toast.makeText(RegistrationDetailsActivity.this, "Community is not near your current location", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(RegistrationDetailsActivity.this, "Failed to check nearby communities", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkCommunityExistence(String name, String communityName, boolean wantsToCreate) {
        db.collection("communities")
                .document(communityName)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean communityExists = snapshot.exists();

                    if (communityExists && wantsToCreate) {
                        Toast.makeText(this, "Community already exists!", Toast.LENGTH_SHORT).show();
                    } else if (!communityExists && !wantsToCreate) {
                        Toast.makeText(this, "Community doesn't exist yet. Please check 'create' to create it.", Toast.LENGTH_SHORT).show();
                    } else {
                        saveUserAndCommunity(name, communityName, wantsToCreate);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking community", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserAndCommunity(String name, String communityName, boolean isManager) {
        CommunityRepository communityRepository = new CommunityRepository();

        if (isManager) {
            communityRepository.createCommunity(
                    communityName,
                    userId,
                    currentLat,
                    currentLng,
                    new ArrayList<>(),
                    new CommunityRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess(String id) {
                            saveUser(name, communityName, isManager);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(RegistrationDetailsActivity.this, "Failed to create community", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        } else {
            saveUser(name, communityName, false);
        }
    }

    private void saveUser(String name, String communityName, boolean isManager) {
        UserRepository userRepository = new UserRepository();

        // נבנה את הקהילה עם מיקום
        Community community;
        if (isManager) {
            CommunityManager communityManager = new CommunityManager(name, communityName);
            community = new Community(communityName, currentLat, currentLng, communityManager);
        } else {
            community = new Community(communityName, currentLat, currentLng);
        }

        // נבנה את המשתמש
        User user = isManager
                ? new CommunityManager(name, communityName)
                : new User(name, communityName);

        user.setIsManager(isManager);

        // שלב שמירה
        User finalUser = user;
        userRepository.createUserProfile(userId, user, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String documentId) {
                Toast.makeText(RegistrationDetailsActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegistrationDetailsActivity.this, MainActivity.class);
                Log.d("Registration", "Sending user: " + finalUser.getClass().getSimpleName());
                Log.d("Registration", "user name: " + finalUser.getUserName());
                Log.d("Registration", "community: " + finalUser.getCommunityName());

                intent.putExtra("currentUser", finalUser);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(RegistrationDetailsActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
