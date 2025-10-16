package com.example.pawpals;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
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

public class RegistrationDetailsActivity extends AppCompatActivity {

    private EditText inputName, inputCommunity, inputContactDetails, inputBio;
    private CheckBox checkboxCreateCommunity;
    private MaterialButton buttonContinue;
    private Spinner spinnerCommunities;

    private final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private FusedLocationProviderClient locationClient;

    private double currentLat = 0;
    private double currentLng = 0;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_details);

        initViews();

        locationClient = LocationServices.getFusedLocationProviderClient(this);

        buttonContinue.setOnClickListener(v -> handleRegistrationDetails());

        checkboxCreateCommunity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            inputCommunity.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            spinnerCommunities.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        requestLocationAndLoadCommunities();
    }

    private void initViews() {
        inputName = findViewById(R.id.input_name);
        inputCommunity = findViewById(R.id.input_community);
        inputContactDetails = findViewById(R.id.input_contact_details);
        inputBio = findViewById(R.id.input_bio);
        checkboxCreateCommunity = findViewById(R.id.checkbox_create_community);
        buttonContinue = findViewById(R.id.button_continue);
        spinnerCommunities = findViewById(R.id.spinner_communities);
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
                Log.d("Registration", "Location: " + currentLat + ", " + currentLng);

                // ✅ Delegate loading nearby communities to CommunityUtils
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

    private void handleRegistrationDetails() {
        String name = safeText(inputName);
        String contactDetails = safeText(inputContactDetails);
        String bio = safeText(inputBio);
        boolean wantsToCreate = checkboxCreateCommunity.isChecked();
        String communityName;

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (wantsToCreate) {
            communityName = safeText(inputCommunity);
            if (communityName.isEmpty()) {
                Toast.makeText(this, "Please enter a community name to create", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (spinnerCommunities.getSelectedItem() == null) {
                Toast.makeText(this, "Please select a community", Toast.LENGTH_SHORT).show();
                return;
            }
            communityName = spinnerCommunities.getSelectedItem().toString();
        }

        // ✅ Delegate saving logic to CommunityUtils
        CommunityUtils.saveUserAndCommunity(
                this,
                name,
                contactDetails,
                bio,
    }

    private void checkCommunityExistence(String name,
                                         String contactDetails,
                                         String bio,
                                         String communityName,
                                         boolean wantsToCreate) {
        db.collection("communities")
                .document(communityName)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean exists = snapshot.exists();
                    if (exists && wantsToCreate) {
                        Toast.makeText(this, "Community already exists!", Toast.LENGTH_SHORT).show();
                    } else if (!exists && !wantsToCreate) {
                        Toast.makeText(this, "Community doesn't exist. Please check 'create' to create it.", Toast.LENGTH_SHORT).show();
                    } else if (wantsToCreate) {
                        Intent intent = new Intent(RegistrationDetailsActivity.this, CommunityCreationDetailsActivity.class);
                        intent.putExtra("communityName", communityName);
                        intent.putExtra("userId", userId);
                        intent.putExtra("userName", name);
                        intent.putExtra("contactDetails", contactDetails);
                        intent.putExtra("bio", bio);
                        intent.putExtra("lat", currentLat);
                        intent.putExtra("lng", currentLng);
                        startActivity(intent);
                        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 300);
                    } else {
                        // הצטרפות לקהילה קיימת
                        saveUser(name, contactDetails, bio, communityName, false);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error checking community", Toast.LENGTH_SHORT).show()
                );
    }

    private void saveUser(String name,
                          String contactDetails,
                          String bio,
                          String communityName,
                          boolean isManager) {
        UserRepository userRepository = new UserRepository();

        // אם Community שלך מקבל מנהל בבנאי – נעביר כבר עם פרטי קשר וביו
        Community community = isManager
                ? new Community(
                communityName,
                userId,
                wantsToCreate,
                currentLat,
                currentLng
        );
    }

    private String safeText(EditText et) {
        return et == null || et.getText() == null ? "" : et.getText().toString().trim();
    }
}
