package com.example.pawpals;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import model.FeedAdapter;
import model.Report;
import model.User;
import model.firebase.Firestore.CommunityRepository;

public class ManagerCommunityActivity extends AppCompatActivity {

    private User currentUser;
    private ImageView imageViewProfile;
    private TextView textViewCommunityName, textViewCommunityDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_community);

        // קבלת המשתמש מה-Intent
        currentUser = getIntent().getParcelableExtra("currentUser");
        CommunityRepository communityRepo = new CommunityRepository();

        imageViewProfile = findViewById(R.id.imageViewProfile);
        textViewCommunityName = findViewById(R.id.textViewCommunityName);
        textViewCommunityDescription = findViewById(R.id.textViewCommunityDescription);

        textViewCommunityName.setText(currentUser.getCommunityName());

        // שליפת פרטי קהילה (תמונה + תיאור)
        communityRepo.getCommunityIdByName(currentUser.getCommunityName(),
                new CommunityRepository.FirestoreIdCallback() {
                    @Override
                    public void onSuccess(String communityId) {
                        communityRepo.getCommunityDetails(communityId,
                                new CommunityRepository.FirestoreCommunityCallback() {
                                    @Override
                                    public void onSuccess(String description, String imageUrl) {
                                        if (description != null && !description.isEmpty())
                                            textViewCommunityDescription.setText(description);
                                        if (imageUrl != null && !imageUrl.isEmpty())
                                            Glide.with(ManagerCommunityActivity.this)
                                                    .load(imageUrl)
                                                    .placeholder(R.drawable.ic_profile_placeholder)
                                                    .into(imageViewProfile);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(ManagerCommunityActivity.this,
                                                "Failed to load community info", Toast.LENGTH_SHORT).show();
                                    }
                                });

                        // שליפת הפוסטים
                        communityRepo.getFeedPosts(communityId,
                                new CommunityRepository.FirestoreReportsListCallback() {
                                    @Override
                                    public void onSuccess(List<Report> posts) {
                                        RecyclerView feedRecyclerView = findViewById(R.id.feedRecyclerView);
                                        feedRecyclerView.setLayoutManager(
                                                new LinearLayoutManager(ManagerCommunityActivity.this));
                                        FeedAdapter adapter = new FeedAdapter(posts);
                                        adapter.setCommunityData(communityId, currentUser.isManager());
                                        feedRecyclerView.setAdapter(adapter);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(ManagerCommunityActivity.this,
                                                "Failed to load bulletin: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(ManagerCommunityActivity.this,
                                "Community not found: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });

        // כפתור חברים
        Button membersButton = findViewById(R.id.buttonMembers);
        membersButton.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerCommunityActivity.this, CommunitySearchActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // כפתור צ'אט
        Button chatButton = findViewById(R.id.buttonChat);
        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerCommunityActivity.this, ChatActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // כפתור מערכת דיווח
        Button reportButton = findViewById(R.id.buttonReportSystem);
        reportButton.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerCommunityActivity.this, WritePostManagerActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // כפתור צפייה בדיווחים
        Button viewReportsButton = findViewById(R.id.buttonViewReports);
        viewReportsButton.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerCommunityActivity.this, ReportsListActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // כפתור מפת אזור (חדש)
        Button areaMapButton = findViewById(R.id.buttonAreaMap);
        areaMapButton.setOnClickListener(v -> {
            Intent intent = new Intent(ManagerCommunityActivity.this, MainActivity.class);
            intent.putExtra("EXTRA_FOCUS_COMMUNITY_NAME", currentUser.getCommunityName());
            intent.putExtra("EXTRA_FOCUS_RADIUS", 1500); // ברירת מחדל
            startActivity(intent);
        });

        // כפתור הגדרות קהילה (חדש)
        Button settingsButton = findViewById(R.id.buttonCommunitySettings);
        settingsButton.setOnClickListener(v -> {
            Intent i = new Intent(this, CommunitySettingsActivity.class);
            i.putExtra(CommunitySettingsActivity.EXTRA_CURRENT_USER, currentUser);
            startActivity(i);
        });
    }
}
