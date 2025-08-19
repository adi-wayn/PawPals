package com.example.pawpals;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

import model.Dog;
import model.User;
import model.firebase.firestore.UserRepository;

public class OtherUserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_OTHER_USER_ID = "otherUserId";

    // Top section
    private TextView userName, bioText, contactText, communityStatus;
    private ImageView userImage;
    private View friendStatusIndicator;

    // Content – Dogs only
    private MaterialButton btnShowDogs;
    private ScrollView dogsScroll;
    private LinearLayout dogsContainer;

    // One-sided “friends” action
    private MaterialButton btnFriendAction;

    // Loading
    private CircularProgressIndicator progress;

    // Data
    private String myUid;
    private String otherUid;

    private UserRepository userRepo;
    private ListenerRegistration friendReg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_user_profile); // אותו לייאאוט מצומצם

        userRepo = new UserRepository();

        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        myUid = (me != null) ? me.getUid() : null;

        otherUid = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        if (TextUtils.isEmpty(otherUid)) {
            Toast.makeText(this, "Missing otherUserId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        showDogs();            // במסך הזה רואים רק Dogs
        setLoading(true);
        loadOtherUser();
        bindFriendButtonLive();
    }

    private void bindViews() {
        userImage             = findViewById(R.id.user_profile_picture);
        friendStatusIndicator = findViewById(R.id.friend_status_indicator);
        userName              = findViewById(R.id.user_name);
        bioText               = findViewById(R.id.bio_text);
        contactText           = findViewById(R.id.contact_text);
        communityStatus       = findViewById(R.id.community_status);

        btnFriendAction       = findViewById(R.id.btn_friend_action);
        btnShowDogs           = findViewById(R.id.btn_show_dogs);
        dogsScroll            = findViewById(R.id.dogs_scroll);
        dogsContainer         = findViewById(R.id.dogs_container);

        progress              = findViewById(R.id.progressBar); // הוסף ל-XML: CircularProgressIndicator עם id זה

        btnShowDogs.setOnClickListener(v -> showDogs());
    }

    private void setLoading(boolean show) {
        if (progress != null) {
            progress.setVisibility(show ? View.VISIBLE : View.GONE);
            progress.setIndeterminate(show);
        }
    }

    private void showDogs() {
        dogsScroll.setVisibility(View.VISIBLE);
        btnShowDogs.setChecked(true);
    }

    private void loadOtherUser() {
        userRepo.getUserById(otherUid, new UserRepository.FirestoreUserCallback() {
            @Override public void onSuccess(User u) {
                setLoading(false);
                if (u == null) {
                    Toast.makeText(OtherUserProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                bindUser(u);
                renderDogs(u.getDogs());
            }
            @Override public void onFailure(Exception e) {
                setLoading(false);
                Toast.makeText(OtherUserProfileActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindUser(User user) {
        userName.setText(nn(user.getUserName()));
        bioText.setText(nn(user.getFieldsOfInterest()));     // Bio
        contactText.setText(nn(user.getContactDetails()));   // Contact

        // טקסט סטטוס – נשמר תואם לעיצוב שלך
        communityStatus.setText(getString(R.string.in_the_same_community));

        // תמונת פרופיל (אם בעתיד יהיה URL): Glide/Picasso כאן
    }

    private void renderDogs(List<Dog> dogs) {
        dogsContainer.removeAllViews();
        if (dogs == null || dogs.isEmpty()) {
            dogsContainer.addView(simpleRow(getString(R.string.no_dogs_to_show)));
            return;
        }

        for (Dog d : dogs) {
            View card = getLayoutInflater().inflate(R.layout.item_profile_card, dogsContainer, false);

            TextView tvName  = card.findViewById(R.id.dog_name);
            TextView tvBreed = card.findViewById(R.id.dog_breed);
            TextView tvAge   = card.findViewById(R.id.dog_age);

            tvName.setText(nn(d.getName()));
            tvBreed.setText(nn(d.getBreed()));
            Object ageObj = d.getAge();
            tvAge.setText(ageObj == null ? "" : String.valueOf(ageObj));

            dogsContainer.addView(card);
        }
    }

    private View simpleRow(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);

        // בלי דפריקציה, עובד בכל הגרסאות
        TextViewCompat.setTextAppearance(
                tv,
                // אם אתה על Material 3:
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
        );

        float d = getResources().getDisplayMetrics().density;
        int padH = (int) (8 * d);   // 8dp
        int padV = (int) (12 * d);  // 12dp (במקום p+4 פיקסלים שהם לא dp)
        tv.setPadding(padH, padV, padH, padV);

        return tv;
    }

    private void bindFriendButtonLive() {
        // ביקשת חד-צדדי וללא אישור – כאן אנחנו רק מסמנים/מוסיפים/מסירים בפרופיל שלי
        if (TextUtils.isEmpty(myUid) || myUid.equals(otherUid)) {
            // אם זה אני – אין כפתור חברות
            btnFriendAction.setVisibility(View.GONE);
            if (friendStatusIndicator != null)
                friendStatusIndicator.setVisibility(View.GONE);
            return;
        }

        // האזנה בזמן אמת האם otherUid קיים תחת friends/myUid/following/otherUid
        friendReg = userRepo.observeIsFriend(myUid, otherUid, (doc, e) -> {
            boolean isFriend = (doc != null && doc.exists());
            renderFriendButton(isFriend);
            renderFriendIndicator(isFriend);
        });
    }

    private void renderFriendButton(boolean isFriend) {
        if (!isFriend) {
            btnFriendAction.setText(R.string.add_friend);
            btnFriendAction.setEnabled(true);
            btnFriendAction.setOnClickListener(v -> {
                btnFriendAction.setEnabled(false); // חסימת דאבל-טאפ
                userRepo.addFriend(myUid, otherUid, new UserRepository.FirestoreCallback() {
                    @Override public void onSuccess(String id) {
                        Toast.makeText(OtherUserProfileActivity.this,
                                R.string.added_to_friends, Toast.LENGTH_SHORT).show();
                        btnFriendAction.setEnabled(true);
                    }
                    @Override public void onFailure(Exception e) {
                        Toast.makeText(OtherUserProfileActivity.this,
                                "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnFriendAction.setEnabled(true);
                    }
                });
            });
        } else {
            btnFriendAction.setText(R.string.remove_friend);
            btnFriendAction.setEnabled(true);
            btnFriendAction.setOnClickListener(v -> {
                btnFriendAction.setEnabled(false);
                userRepo.removeFriend(myUid, otherUid, new UserRepository.FirestoreCallback() {
                    @Override public void onSuccess(String id) {
                        Toast.makeText(OtherUserProfileActivity.this,
                                R.string.removed_from_friends, Toast.LENGTH_SHORT).show();
                        btnFriendAction.setEnabled(true);
                    }
                    @Override public void onFailure(Exception e) {
                        Toast.makeText(OtherUserProfileActivity.this,
                                "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnFriendAction.setEnabled(true);
                    }
                });
            });
        }
    }

    private void renderFriendIndicator(boolean isFriend) {
        if (friendStatusIndicator == null || friendStatusIndicator.getBackground() == null) return;
        int color = isFriend ? 0xFF4CAF50 : 0xFFBDBDBD; // ירוק / אפור
        friendStatusIndicator.getBackground().setTint(color);
    }

    private String nn(String s) { return s == null ? "" : s; }

    @Override
    protected void onStop() {
        if (friendReg != null) { friendReg.remove(); friendReg = null; }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (friendReg != null) { friendReg.remove(); friendReg = null; }
        super.onDestroy();
    }
}