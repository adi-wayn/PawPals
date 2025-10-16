package com.example.pawpals;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import model.Community;
import model.CommunityManager;
import model.User;
import model.firebase.Firestore.CommunityRepository;
import model.firebase.Firestore.UserRepository;
import model.firebase.Storage.StorageRepository;

import java.util.ArrayList;

public class CommunityCreationDetailsActivity extends AppCompatActivity {

    private EditText etDescription;
    private ImageView imgPreview;
    private MaterialButton btnUpload, btnCreate;
    private Uri selectedImageUri;
    private StorageRepository storageRepo;
    private CommunityRepository communityRepo;
    private UserRepository userRepo;

    private String communityName, userId, userName, contactDetails, bio;
    private double lat, lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_creation_details);

        etDescription = findViewById(R.id.etDescription);
        imgPreview = findViewById(R.id.imgPreview);
        btnUpload = findViewById(R.id.btnUploadImage);
        btnCreate = findViewById(R.id.btnCreateCommunity);

        storageRepo = new StorageRepository();
        communityRepo = new CommunityRepository();
        userRepo = new UserRepository();

        // קבלת נתונים מהמסך הקודם
        communityName = getIntent().getStringExtra("communityName");
        userId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");
        contactDetails = getIntent().getStringExtra("contactDetails");
        bio = getIntent().getStringExtra("bio");
        lat = getIntent().getDoubleExtra("lat", 0);
        lng = getIntent().getDoubleExtra("lng", 0);

        // בורר תמונה
        ActivityResultLauncher<String> imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        Glide.with(this).load(uri).into(imgPreview);
                    }
                }
        );

        btnUpload.setOnClickListener(v -> imagePicker.launch("image/*"));

        btnCreate.setOnClickListener(v -> createCommunity());
    }

    private void createCommunity() {
        String description = etDescription.getText().toString().trim();

        if (communityName.isEmpty() || userId == null) {
            Toast.makeText(this, "Missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        // שלב 1 – צור את הקהילה (ללא תמונה בשלב ראשון)
        communityRepo.createCommunity(
                communityName,
                userId,
                lat,
                lng,
                description,
                "",
                new ArrayList<>(),
                new CommunityRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess(String id) {
                        if (selectedImageUri != null) {
                            uploadImageAndFinalize(description);
                        } else {
                            finalizeCreation(description, "");
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(CommunityCreationDetailsActivity.this,
                                "Failed to create community", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void uploadImageAndFinalize(String description) {
        storageRepo.uploadCommunityProfileImage(
                this,
                communityName,
                selectedImageUri,
                1080,
                80,
                null,
                new StorageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(@NonNull String downloadUrl) {
                        communityRepo.updateCommunityImage(communityName, downloadUrl, new CommunityRepository.FirestoreCallback() {
                            @Override
                            public void onSuccess(String id) {
                                finalizeCreation(description, downloadUrl);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                finalizeCreation(description, "");
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Exception e) {
                        finalizeCreation(description, "");
                    }
                });
    }

    private void finalizeCreation(String description, String imageUrl) {
        // צור את המשתמש כמנהל
        CommunityManager manager = new CommunityManager(userName, communityName, contactDetails, bio);
        manager.setIsManager(true);

        //userId
        userRepo.createUserProfile(FirebaseAuth.getInstance().getCurrentUser().getUid(), manager, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String docId) {
                Toast.makeText(CommunityCreationDetailsActivity.this, "Community created!", Toast.LENGTH_SHORT).show();

                Intent i = new Intent(CommunityCreationDetailsActivity.this, MainActivity.class);
                i.putExtra("currentUser", manager);
                startActivity(i);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CommunityCreationDetailsActivity.this,
                        "Failed to save user", Toast.LENGTH_SHORT).show();
            }
        });
    }
}