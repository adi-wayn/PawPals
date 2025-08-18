package com.example.pawpals;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import model.Dog;
import model.User;
import model.firebase.firestore.UserRepository; // עדכן אם הנתיב אצלך שונה

public class OtherUserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_OTHER_USER_ID = "otherUserId";

    // Top section (אותם IDs כמו אצלך)
    private TextView userName;
    private TextView bioText;
    private TextView contactText;
    private TextView communityStatus;
    private ImageView userImage;
    private View friendStatusIndicator;

    // Toggle + content (רק Dogs)
    private MaterialButton btnShowDogs;
    private ScrollView dogsScroll;     // אותו id: dogs_scroll
    private LinearLayout dogsContainer;

    // Friends action (חד-צדדי)
    private MaterialButton btnFriendAction;

    // Data
    private String myUid;
    private String otherUid;

    private UserRepository userRepo;
    private com.google.firebase.firestore.ListenerRegistration friendReg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_user_profile); // הלייאאוט המצומצם ללא Friends

        userRepo = new UserRepository();

        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        myUid = (me != null) ? me.getUid() : null;

        otherUid = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        if (otherUid == null) {
            Toast.makeText(this, "Missing otherUserId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        // מסך זה מציג רק כלבים כברירת מחדל
        showDogs();

        loadOtherUser();
        bindFriendButtonLive();
    }

    private void bindViews() {
        userImage            = findViewById(R.id.user_profile_picture);
        friendStatusIndicator= findViewById(R.id.friend_status_indicator);
        userName             = findViewById(R.id.user_name);
        bioText              = findViewById(R.id.bio_text);
        contactText          = findViewById(R.id.contact_text);
        communityStatus      = findViewById(R.id.community_status);

        btnFriendAction      = findViewById(R.id.btn_friend_action);
        btnShowDogs          = findViewById(R.id.btn_show_dogs);
        dogsScroll           = findViewById(R.id.dogs_scroll);
        dogsContainer        = findViewById(R.id.dogs_container);

        btnShowDogs.setOnClickListener(v -> showDogs());
    }

    private void showDogs() {
        // אין Friends במסך הזה, אז רק מבטיחים שה־Dogs מוצג
        dogsScroll.setVisibility(View.VISIBLE);
        btnShowDogs.setChecked(true);
    }

    private void loadOtherUser() {
        userRepo.getUserById(otherUid, new UserRepository.FirestoreUserCallback() {
            @Override public void onSuccess(User u) {
                if (u == null) {
                    Toast.makeText(OtherUserProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                bindUser(u);
                renderDogs(u.getDogs());
            }
            @Override public void onFailure(Exception e) {
                Toast.makeText(OtherUserProfileActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindUser(User user) {
        userName.setText(nn(user.getUserName()));
        bioText.setText(nn(user.getFieldsOfInterest()));     // Bio
        contactText.setText(nn(user.getContactDetails()));   // Contact
        // שמירת הקופי כמו אצלך (תוכל להחליף אם תרצה טקסט אחר)
        communityStatus.setText(getString(R.string.in_the_same_community));
        // userImage: אם יש לך URL — טען עם Glide/Picasso
    }

    private void renderDogs(List<Dog> dogs) {
        dogsContainer.removeAllViews();
        if (dogs == null || dogs.isEmpty()) {
            dogsContainer.addView(simpleDogRow("No dogs to show."));
            return;
        }

        for (Dog d : dogs) {
            View card = getLayoutInflater().inflate(R.layout.item_profile_card, dogsContainer, false);

            TextView tvName  = card.findViewById(R.id.dog_name);
            TextView tvBreed = card.findViewById(R.id.dog_breed);
            TextView tvAge   = card.findViewById(R.id.dog_age);

            tvName.setText(nn(d.getName()));
            tvBreed.setText(nn(d.getBreed()));

            // גיל – תואם ללוגיקה שלך בפרופיל (תומך בכל טיפוס)
            Object ageObj = d.getAge();
            tvAge.setText(ageObj == null ? "" : String.valueOf(ageObj));

            dogsContainer.addView(card);
        }
    }

    private View simpleDogRow(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_MaterialComponents_Body2);
        tv.setPadding(8, 12, 8, 12);
        return tv;
    }

    private void bindFriendButtonLive() {
        if (myUid == null) {
            btnFriendAction.setEnabled(false);
            return;
        }
        // מאזין בזמן אמת האם otherUid נמצא ב-friends של myUid
        friendReg = userRepo.observeIsFriend(myUid, otherUid, (doc, e) -> {
            boolean isFriend = doc != null && doc.exists();
            renderFriendButton(isFriend);
            renderFriendIndicator(isFriend);
        });
    }

    private void renderFriendButton(boolean isFriend) {
        if (!isFriend) {
            btnFriendAction.setText("Add Friend");
            btnFriendAction.setOnClickListener(v ->
                    userRepo.addFriend(myUid, otherUid, new UserRepository.FirestoreCallback() {
                        @Override public void onSuccess(String id) {
                            Toast.makeText(OtherUserProfileActivity.this, "Added to your friends", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Exception e) {
                            Toast.makeText(OtherUserProfileActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
            );
        } else {
            btnFriendAction.setText("Remove Friend");
            btnFriendAction.setOnClickListener(v ->
                    userRepo.removeFriend(myUid, otherUid, new UserRepository.FirestoreCallback() {
                        @Override public void onSuccess(String id) {
                            Toast.makeText(OtherUserProfileActivity.this, "Removed from your friends", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Exception e) {
                            Toast.makeText(OtherUserProfileActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
            );
        }
    }

    private void renderFriendIndicator(boolean isFriend) {
        // ירוק אם חבר, אפור אם לא (תואם הוויזואליות של status_indicator)
        int color = isFriend ? 0xFF4CAF50 : 0xFFBDBDBD;
        friendStatusIndicator.getBackground().setTint(color);
    }

    private String nn(String s) { return s == null ? "" : s; }

    @Override
    protected void onDestroy() {
        if (friendReg != null) friendReg.remove();
        super.onDestroy();
    }
}