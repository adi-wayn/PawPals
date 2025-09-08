package com.example.pawpals;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import model.User;
import model.firebase.firestore.CommunityRepository;

public class CommunitySettingsActivity extends AppCompatActivity {
    public static final String EXTRA_CURRENT_USER = "currentUser";

    private User currentUser;
    private String communityId;
    private CommunityRepository repo;

    private TextView tvCommunity;
    private Switch swOpenApplications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_settings);

        currentUser = getIntent().getParcelableExtra(EXTRA_CURRENT_USER);
        repo = new CommunityRepository();

        tvCommunity = findViewById(R.id.tvCommunityName);
        swOpenApplications = findViewById(R.id.swOpenApplications);

        tvCommunity.setText(currentUser.getCommunityName());

        // הבא את communityId לפי שם
        repo.getCommunityIdByName(currentUser.getCommunityName(), new CommunityRepository.FirestoreIdCallback() {
            @Override public void onSuccess(String id) {
                communityId = id;
                // מצב התחלתי של הסוויץ'
                repo.getManagerApplicationsOpen(communityId, new CommunityRepository.FirestoreBooleanCallback() {
                    @Override public void onSuccess(boolean value) { swOpenApplications.setChecked(value); }
                    @Override public void onFailure(Exception e) { /* ignore, ברירת מחדל false */ }
                });
            }
            @Override public void onFailure(Exception e) {
                Toast.makeText(CommunitySettingsActivity.this, "Community not found", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        // שינוי הדגל
        swOpenApplications.setOnCheckedChangeListener((CompoundButton btn, boolean isChecked) -> {
            if (communityId == null) return;
            repo.setManagerApplicationsOpen(communityId, isChecked, new CommunityRepository.FirestoreCallback() {
                @Override public void onSuccess(String documentId) {
                    Toast.makeText(CommunitySettingsActivity.this,
                            isChecked ? "Applications opened" : "Applications closed",
                            Toast.LENGTH_SHORT).show();
                }
                @Override public void onFailure(Exception e) {
                    Toast.makeText(CommunitySettingsActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    swOpenApplications.setChecked(!isChecked);
                }
            });
        });
    }
}