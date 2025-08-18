package com.example.pawpals;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import model.Dog;
import model.User;
import model.firebase.firestore.UserRepository;

public class ProfileActivity extends AppCompatActivity {

    // Top section
    private TextView userName;
    private TextView bioText;
    private TextView contactText;
    private TextView communityStatus;

    // Toggle + content
    private MaterialButton btnShowFriends;
    private MaterialButton btnShowDogs;
    private RecyclerView friendsRecycler;
    private View dogsScroll;          // ScrollView
    private LinearLayout dogsContainer;

    // Data
    private User currentUser;
    private final List<User> friends = new ArrayList<>();
    private RecyclerView.Adapter<?> friendsAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view);

        // ===== Views =====
        userName        = findViewById(R.id.user_name);
        bioText         = findViewById(R.id.bio_text);
        contactText     = findViewById(R.id.contact_text);
        communityStatus = findViewById(R.id.community_status);

        btnShowFriends  = findViewById(R.id.btn_show_friends);
        btnShowDogs     = findViewById(R.id.btn_show_dogs);

        friendsRecycler = findViewById(R.id.friends_recycler);
        dogsScroll      = findViewById(R.id.dogs_scroll);
        dogsContainer   = findViewById(R.id.dogs_container);

        // ===== Toggle default =====
        showFriends(); // ברירת מחדל: חברים

        btnShowFriends.setOnClickListener(v -> showFriends());
        btnShowDogs.setOnClickListener(v -> showDogs());

        // ===== קבלת המשתמש (מומלץ להעביר כ-Parcelable) =====
        currentUser = getIntent().getParcelableExtra("currentUser");

        if (currentUser != null) {
            bindUser(currentUser);
            setupFriendsList(currentUser.getCommunityName());
            renderDogs(currentUser.getDogs());
        }
    }

    private void showFriends() {
        friendsRecycler.setVisibility(View.VISIBLE);
        dogsScroll.setVisibility(View.GONE);
        btnShowFriends.setChecked(true);
        btnShowDogs.setChecked(false);
    }

    private void showDogs() {
        friendsRecycler.setVisibility(View.GONE);
        dogsScroll.setVisibility(View.VISIBLE);
        btnShowDogs.setChecked(true);
        btnShowFriends.setChecked(false);
    }

    private void bindUser(User user) {
        userName.setText(nn(user.getUserName()));
        bioText.setText(nn(user.getFieldsOfInterest()));     // זה ה-Bio
        contactText.setText(nn(user.getContactDetails()));   // יצירת קשר
        communityStatus.setText(
                getString(R.string.user_community) + " " + nn(user.getCommunityName())
        );
        // כאן ניתן להוסיף טעינת תמונת פרופיל אם תרצי
    }

    private void setupFriendsList(String communityName) {
        friendsRecycler.setLayoutManager(new LinearLayoutManager(this));
        friendsAdapter = new model.CommunityAdapter(friends);
        friendsRecycler.setAdapter(friendsAdapter);

        if (communityName == null || communityName.isEmpty()) return;

        new UserRepository().getUsersByCommunity(communityName, new UserRepository.FirestoreUsersListCallback() {
            @Override public void onSuccess(List<User> users) {
                friends.clear();
                if (currentUser != null && currentUser.getUserName() != null) {
                    for (User u : users) {
                        if (u.getUserName() != null &&
                                u.getUserName().equals(currentUser.getUserName())) continue;
                        friends.add(u);
                    }
                } else {
                    friends.addAll(users);
                }
                friendsAdapter.notifyDataSetChanged();
            }
            @Override public void onFailure(Exception e) {
                // אפשר Toast/Log אם תרצי
            }
        });
    }

    private void renderDogs(List<Dog> dogs) {
        dogsContainer.removeAllViews();
        if (dogs == null || dogs.isEmpty()) return;

        for (Dog d : dogs) {
            View card = getLayoutInflater().inflate(R.layout.item_profile_card, dogsContainer, false);

            TextView tvName  = card.findViewById(R.id.dog_name);
            TextView tvBreed = card.findViewById(R.id.dog_breed);
            TextView tvAge   = card.findViewById(R.id.dog_age);

            tvName.setText(nn(d.getName()));
            tvBreed.setText(nn(d.getBreed()));

            // גיל – עובד גם אם זה int, Integer או String
            Object ageObj = d.getAge();
            tvAge.setText(ageObj == null ? "" : String.valueOf(ageObj));

            dogsContainer.addView(card);
        }
    }

    private String nn(String s) { return s == null ? "" : s; }
}
