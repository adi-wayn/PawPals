package com.example.pawpals;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import model.User;
import model.firebase.Firestore.CommunityRepository;
import model.firebase.Storage.StorageRepository;

public class CommunitySettingsActivity extends AppCompatActivity {
    public static final String EXTRA_CURRENT_USER = "currentUser";

    private User currentUser;
    private String communityId;
    private CommunityRepository repo;
    private TextView tvCommunity;
    private Switch swOpenApplications;
    private EditText etDescription;
    private ImageView imgCommunity;
    private Button btnUploadImage, btnDeleteImage, btnSave;
    private Uri selectedImageUri;
    private StorageRepository storageRepo;
    private String existingImageUrl; // לשמירה על כתובת קיימת אם יש

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_settings);

        currentUser = getIntent().getParcelableExtra(EXTRA_CURRENT_USER);
        repo = new CommunityRepository();
        storageRepo = new StorageRepository();

        tvCommunity = findViewById(R.id.tvCommunityName);
        swOpenApplications = findViewById(R.id.swOpenApplications);
        etDescription = findViewById(R.id.etCommunityDescription);
        imgCommunity = findViewById(R.id.imgCommunity);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnDeleteImage = findViewById(R.id.btnDeleteImage); // חדש
        btnSave = findViewById(R.id.btnSave);

        tvCommunity.setText(currentUser.getCommunityName());

        // בורר תמונה
        ActivityResultLauncher<String> imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        Glide.with(this).load(uri).into(imgCommunity);
                    }
                }
        );

        btnUploadImage.setOnClickListener(v -> imagePicker.launch("image/*"));

        // שליפת פרטי קהילה (כולל תיאור ותמונה קיימת)
        repo.getCommunityIdByName(currentUser.getCommunityName(),
                new CommunityRepository.FirestoreIdCallback() {
                    @Override
                    public void onSuccess(String id) {
                        communityId = id;

                        // שליפת מצב סוויץ'
                        repo.getManagerApplicationsOpen(communityId,
                                new CommunityRepository.FirestoreBooleanCallback() {
                                    @Override
                                    public void onSuccess(boolean value) {
                                        swOpenApplications.setChecked(value);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        swOpenApplications.setChecked(false);
                                    }
                                });

                        // שליפת פרטי תיאור ותמונה
                        repo.getCommunityDetails(communityId,
                                new CommunityRepository.FirestoreCommunityCallback() {
                                    @Override
                                    public void onSuccess(String description, String imageUrl) {
                                        if (description != null)
                                            etDescription.setText(description);
                                        if (imageUrl != null && !imageUrl.isEmpty()) {
                                            existingImageUrl = imageUrl;
                                            Glide.with(CommunitySettingsActivity.this)
                                                    .load(imageUrl)
                                                    .placeholder(R.drawable.ic_profile_placeholder)
                                                    .into(imgCommunity);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(CommunitySettingsActivity.this,
                                                "Failed to load community info", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(CommunitySettingsActivity.this,
                                "Community not found", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

        // שינוי מצב קבלת מועמדויות
        swOpenApplications.setOnCheckedChangeListener(
                (CompoundButton btn, boolean isChecked) -> {
                    if (communityId == null) return;
                    repo.setManagerApplicationsOpen(communityId, isChecked,
                            new CommunityRepository.FirestoreCallback() {
                                @Override
                                public void onSuccess(String documentId) {
                                    Toast.makeText(CommunitySettingsActivity.this,
                                            isChecked ? "Applications opened" : "Applications closed",
                                            Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(CommunitySettingsActivity.this,
                                            "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    swOpenApplications.setChecked(!isChecked);
                                }
                            });
                });

        // מחיקת תמונה
        btnDeleteImage.setOnClickListener(v -> {
            if (existingImageUrl == null && selectedImageUri == null) {
                Toast.makeText(this, "No image to delete", Toast.LENGTH_SHORT).show();
                return;
            }

            storageRepo.deleteByPath("communities/" + communityId + "/profile.jpg",
                    new StorageRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            repo.updateCommunityImage(communityId, "",
                                    new CommunityRepository.FirestoreCallback() {
                                        @Override
                                        public void onSuccess(String id) {
                                            selectedImageUri = null;
                                            existingImageUrl = null;
                                            imgCommunity.setImageResource(R.drawable.ic_profile_placeholder);
                                            Toast.makeText(CommunitySettingsActivity.this,
                                                    "Image deleted", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Toast.makeText(CommunitySettingsActivity.this,
                                                    "Failed to clear image URL", Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }

                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(CommunitySettingsActivity.this,
                                    "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // שמירת שינויים
        btnSave.setOnClickListener(v -> {
            if (communityId == null) return;

            String newDesc = etDescription.getText().toString().trim();

            // עדכון תיאור
            repo.updateCommunityDescription(communityId, newDesc, new CommunityRepository.FirestoreCallback() {
                @Override
                public void onSuccess(String id) {
                    Toast.makeText(CommunitySettingsActivity.this, "Description updated", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(CommunitySettingsActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            // העלאת תמונה אם נבחרה
            if (selectedImageUri != null) {
                storageRepo.uploadCommunityProfileImage(
                        CommunitySettingsActivity.this,
                        communityId,
                        selectedImageUri,
                        1080,
                        80,
                        null,
                        new StorageRepository.UploadCallback() {
                            @Override
                            public void onSuccess(@NonNull String downloadUrl) {
                                repo.updateCommunityImage(communityId, downloadUrl,
                                        new CommunityRepository.FirestoreCallback() {
                                            @Override
                                            public void onSuccess(String id) {
                                                Toast.makeText(CommunitySettingsActivity.this,
                                                        "Image updated", Toast.LENGTH_SHORT).show();
                                                finishToCommunity(); // חזרה למסך הקהילה
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                Toast.makeText(CommunitySettingsActivity.this,
                                                        "Image update failed: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }

                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(CommunitySettingsActivity.this,
                                        "Upload failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                finishToCommunity(); // אם אין תמונה חדשה – פשוט חוזרים
            }
        });
    }

    private void finishToCommunity() {
        Intent intent = new Intent(this,
                currentUser.isManager() ? ManagerCommunityActivity.class : CommunityActivity.class);
        intent.putExtra("currentUser", currentUser);
        startActivity(intent);
        finish();
    }
}