package com.example.pawpals;

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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import model.FeedAdapter;
import model.Report;
import model.User;
import model.firebase.Firestore.CommunityRepository;

public class CommunityActivity extends AppCompatActivity {

    private User currentUser;
    private ImageView imageViewProfile;
    private TextView textViewCommunityName, textViewCommunityDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

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
                                            Glide.with(CommunityActivity.this)
                                                    .load(imageUrl)
                                                    .placeholder(R.drawable.ic_profile_placeholder)
                                                    .into(imageViewProfile);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(CommunityActivity.this,
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
                                                new LinearLayoutManager(CommunityActivity.this));
                                        FeedAdapter adapter = new FeedAdapter(posts);
                                        adapter.setCommunityData(communityId, currentUser.isManager());
                                        feedRecyclerView.setAdapter(adapter);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(CommunityActivity.this,
                                                "Failed to load bulletin: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(CommunityActivity.this,
                                "Community not found: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });

        // מעבר למסך חברים
        Button membersButton = findViewById(R.id.buttonMembers);
        membersButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunitySearchActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // מעבר לצ'אט
        Button chatButton = findViewById(R.id.buttonChat);
        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // מעבר למסך יצירת דיווח
        Button reportButton = findViewById(R.id.buttonReportSystem);
        reportButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportFormActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // מעבר למפת האזור
        Button areaMapButton = findViewById(R.id.buttonAreaMap);
        areaMapButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_FOCUS_COMMUNITY_NAME,
                    currentUser.getCommunityName());
            intent.putExtra(MainActivity.EXTRA_FOCUS_RADIUS, 1500);
            startActivity(intent);
        });
    }
}
