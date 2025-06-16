package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import model.FeedAdapter;
import model.Report;
import model.User;
import model.firebase.CommunityRepository;

public class ManagerCommunityActivity extends AppCompatActivity {

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_community);

        CommunityRepository communityRepo = new CommunityRepository();

        communityRepo.getCommunityIdByName(currentUser.getCommunity().getName(), new CommunityRepository.FirestoreIdCallback() {
            @Override
            public void onSuccess(String communityId) {
                communityRepo.getFeedPosts(communityId, new CommunityRepository.FirestoreReportsListCallback() {
                    @Override
                    public void onSuccess(List<Report> posts) {
                        RecyclerView feedRecyclerView = findViewById(R.id.feedRecyclerView);
                        feedRecyclerView.setLayoutManager(new LinearLayoutManager(ManagerCommunityActivity.this));
                        FeedAdapter adapter = new FeedAdapter(posts);
                        feedRecyclerView.setAdapter(adapter);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(ManagerCommunityActivity.this, "Failed to load bulletin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ManagerCommunityActivity.this, "Community not found: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // קבלת המשתמש מה-Intent
        currentUser = getIntent().getParcelableExtra("currentUser");

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
            Intent intent = new Intent(ManagerCommunityActivity.this, ReportFormActivity.class);
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

//        // כפתור מפת אזור
//        Button areaMapButton = findViewById(R.id.buttonAreaMap);
//        areaMapButton.setOnClickListener(v -> {
//            Intent intent = new Intent(ManagerCommunityActivity.this, AreaMapActivity.class);
//            intent.putExtra("currentUser", currentUser);
//            startActivity(intent);
//        });
//
//        // כפתור הגדרות קהילה (חדש)
//        Button settingsButton = findViewById(R.id.buttonCommunitySettings);
//        settingsButton.setOnClickListener(v -> {
//            Intent intent = new Intent(ManagerCommunityActivity.this, CommunitySettingsActivity.class);
//            intent.putExtra("currentUser", currentUser);
//            startActivity(intent);
//        });
    }
}
