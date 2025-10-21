package com.example.pawpals;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

import model.Dog;
import model.User;
import model.firebase.Firestore.UserRepository;

public class OtherUserProfileActivity extends AppCompatActivity {

    private static final String TAG = "OtherUserProfileActivity";
    public static final String EXTRA_OTHER_USER_ID = "otherUserId";

    // Top section
    private TextView userName, bioText, contactText, communityStatus;
    private View friendStatusIndicator;
    private MaterialButton btnFriendAction;
    private ShapeableImageView userProfilePicture;  // ✅ תמונת פרופיל

    // Content
    private MaterialButton btnShowDogs;
    private ScrollView dogsScroll;
    private LinearLayout dogsContainer;

    // Data
    private String myUid;
    private String otherUid;

    private final UserRepository repo = new UserRepository();
    private ListenerRegistration friendReg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_user_profile);

        // ===== Views =====
        userName = findViewById(R.id.user_name);
        bioText = findViewById(R.id.bio_text);
        contactText = findViewById(R.id.contact_text);
        communityStatus = findViewById(R.id.community_status);
        friendStatusIndicator = findViewById(R.id.friend_status_indicator);
        btnFriendAction = findViewById(R.id.btn_friend_action);
        userProfilePicture = findViewById(R.id.user_profile_picture); // ✅ תמונת פרופיל

        btnShowDogs = findViewById(R.id.btn_show_dogs);
        dogsScroll = findViewById(R.id.dogs_scroll);
        dogsContainer = findViewById(R.id.dogs_container);

        // ===== IDs =====
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        myUid = (me != null) ? me.getUid() : null;

        otherUid = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        if (otherUid == null || otherUid.isEmpty()) {
            Toast.makeText(this, "Missing user ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ===== Load User =====
        loadOtherUser();

        // ===== Bind actions =====
        btnShowDogs.setOnClickListener(v -> showDogs());
        showDogs(); // כברירת מחדל
        bindFriendButtonLive();
    }

    // ===== UI Control =====
    private void showDogs() {
        dogsScroll.setVisibility(View.VISIBLE);
        btnShowDogs.setChecked(true);
    }

    private void bindUser(User user) {
        userName.setText(nn(user.getUserName()));
        bioText.setText(nn(user.getFieldsOfInterest()));
        contactText.setText(nn(user.getContactDetails()));
        String community = nn(user.getCommunityName());
        communityStatus.setText(getString(R.string.user_community) + (community.isEmpty() ? "" : (" " + community)));

        // ✅ טעינת תמונת פרופיל מה־Firestore
        String userId = user.getUid();
        if (userId != null && !userId.isEmpty() && userProfilePicture != null) {
            repo.getUserProfileImage(userId, new UserRepository.FirestoreStringCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(OtherUserProfileActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .into(userProfilePicture);
                    } else {
                        userProfilePicture.setImageResource(R.drawable.ic_profile_placeholder);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.w(TAG, "Failed to load profile image: " + e.getMessage());
                    userProfilePicture.setImageResource(R.drawable.ic_profile_placeholder);
                }
            });
        } else if (userProfilePicture != null) {
            userProfilePicture.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    // ===== Load User =====
    private void loadOtherUser() {
        repo.getUserById(otherUid, new UserRepository.FirestoreUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user == null) {
                    Toast.makeText(OtherUserProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                bindUser(user);

                repo.getDogsForUser(otherUid, new UserRepository.FirestoreDogsListCallback() {
                    @Override
                    public void onSuccess(List<Dog> dogs) {
                        renderDogs(dogs);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(OtherUserProfileActivity.this,
                                "Failed to load dogs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        renderDogs(null);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(OtherUserProfileActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ===== Dogs =====
    @SuppressLint("MissingInflatedId")
    private void renderDogs(@Nullable List<Dog> dogs) {
        dogsContainer.removeAllViews();
        if (dogs == null || dogs.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText(getString(R.string.no_dogs_to_show));
            dogsContainer.addView(tv);
            return;
        }

        for (Dog d : dogs) {
            View card = getLayoutInflater().inflate(R.layout.item_dog_card, dogsContainer, false);
            if (card == null) {
                Log.e(TAG, "inflate item_dog_card returned null");
                continue;
            }

            TextView tvName = card.findViewById(R.id.dog_name);
            TextView tvBreed = card.findViewById(R.id.dog_breed);
            TextView tvAge = card.findViewById(R.id.dog_age);

            tvName.setText(nn(d.getName()));
            tvBreed.setText(nn(d.getBreed()));
            tvAge.setText(d.getAge() != null ? String.valueOf(d.getAge()) : "");

            card.setOnClickListener(v -> {
                try {
                    Intent it = new Intent(OtherUserProfileActivity.this, DogDetailsActivity.class);
                    it.putExtra(ProfileActivity.EXTRA_DOG, d);
                    it.putExtra(ProfileActivity.EXTRA_OWNER_ID, otherUid);
                    startActivity(it);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open DogDetailsActivity", e);
                    Toast.makeText(this, "Error opening dog details", Toast.LENGTH_SHORT).show();
                }
            });

            dogsContainer.addView(card);
        }
    }

    // ===== Friends =====
    private void bindFriendButtonLive() {
        if (myUid == null || myUid.equals(otherUid)) {
            btnFriendAction.setVisibility(View.GONE);
            friendStatusIndicator.setVisibility(View.GONE);
            return;
        }

        friendReg = repo.observeIsFriend(myUid, otherUid, (doc, e) -> {
            boolean isFriend = (doc != null && doc.exists());
            renderFriendButton(isFriend);
            renderFriendIndicator(isFriend);
        });
    }

    private void renderFriendButton(boolean isFriend) {
        if (isFriend) {
            btnFriendAction.setText(R.string.remove_friend);
            btnFriendAction.setOnClickListener(v -> {
                btnFriendAction.setEnabled(false);
                repo.removeFriend(myUid, otherUid, new UserRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess(String s) {
                        Toast.makeText(OtherUserProfileActivity.this, R.string.removed_from_friends, Toast.LENGTH_SHORT).show();
                        btnFriendAction.setEnabled(true);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        btnFriendAction.setEnabled(true);
                    }
                });
            });
        } else {
            btnFriendAction.setText(R.string.add_friend);
            btnFriendAction.setOnClickListener(v -> {
                btnFriendAction.setEnabled(false);
                repo.addFriend(myUid, otherUid, new UserRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess(String s) {
                        Toast.makeText(OtherUserProfileActivity.this, R.string.added_to_friends, Toast.LENGTH_SHORT).show();
                        btnFriendAction.setEnabled(true);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        btnFriendAction.setEnabled(true);
                    }
                });
            });
        }
    }

    private void renderFriendIndicator(boolean isFriend) {
        if (friendStatusIndicator == null || friendStatusIndicator.getBackground() == null) return;
        int color = isFriend
                ? getColor(R.color.status_online_green)
                : getColor(R.color.status_offline_gray);
        friendStatusIndicator.getBackground().setTint(color);
    }

    private String nn(String s) {
        return (s == null) ? "" : s;
    }

    @Override
    protected void onStop() {
        if (friendReg != null) {
            friendReg.remove();
            friendReg = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (friendReg != null) {
            friendReg.remove();
            friendReg = null;
        }
        super.onDestroy();
    }
}