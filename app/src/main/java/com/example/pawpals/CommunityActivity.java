package com.example.pawpals;

import static android.content.Intent.getIntent;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import model.FeedAdapter;
import model.Report;
import model.User;
import model.firebase.Firestore.CommunityRepository;

public class CommunityActivity extends AppCompatActivity {

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);
        // נטילת המשתמש מה-Intent
        currentUser = getIntent().getParcelableExtra("currentUser");
        CommunityRepository communityRepo = new CommunityRepository();

        TextView textViewCommunityName = findViewById(R.id.textViewCommunityName);
        textViewCommunityName.setText(currentUser.getCommunityName());

        communityRepo.getCommunityIdByName(currentUser.getCommunityName(), new CommunityRepository.FirestoreIdCallback() {
            @Override
            public void onSuccess(String communityId) {
                communityRepo.getFeedPosts(communityId, new CommunityRepository.FirestoreReportsListCallback() {
                    @Override
                    public void onSuccess(List<Report> posts) {
                        RecyclerView feedRecyclerView = findViewById(R.id.feedRecyclerView);
                        feedRecyclerView.setLayoutManager(new LinearLayoutManager(CommunityActivity.this));
                        FeedAdapter adapter = new FeedAdapter(posts);
                        feedRecyclerView.setAdapter(adapter);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(CommunityActivity.this, "Failed to load bulletin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CommunityActivity.this, "Community not found: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // כפתור חברים
        Button membersButton = findViewById(R.id.buttonMembers);
        membersButton.setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, CommunitySearchActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // כפתור צ'אט
        Button chatButton = findViewById(R.id.buttonChat);
        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, ChatActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // כפתור מערכת דיווח
        Button reportButton = findViewById(R.id.buttonReportSystem);
        reportButton.setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, ReportFormActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // כפתור מפת אזור (חדש)
        Button areaMapButton = findViewById(R.id.buttonAreaMap);
        areaMapButton.setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, MainActivity.class);
            intent.putExtra("EXTRA_FOCUS_COMMUNITY_NAME", currentUser.getCommunityName());
            intent.putExtra("EXTRA_FOCUS_RADIUS", 1500); // ברירת מחדל
            startActivity(intent);
        });
    }
}
