package com.example.pawpals;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import model.Dog;
import model.FriendsAdapter;
import model.User;
import model.firebase.Firestore.UserRepository;

/**
 * ProfileActivity – מסך פרופיל משתמש (שלי או של משתמש אחר)
 * תיקונים עיקריים:
 * 1) שימוש עקבי ב displayedUserId לכל טעינות הכלבים ולמעבר למסך פרטי כלב.
 * 2) רשימת חברים נטענת לפי friendsIds אם יש, אחרת לפי קהילה.
 * 3) הסתרת כפתור הוספת כלב כשזה לא הפרופיל שלי.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    // Intent keys (שימי לב – כאן מחרוזות מקומיות כדי למנוע תלות):
    public static final String EXTRA_CURRENT_USER = "currentUser"; // User implements Parcelable
    public static final String EXTRA_DOG = "extra_dog";            // ב-DogDetailsActivity שמרי על אותו שם
    public static final String EXTRA_OWNER_ID = "extra_owner_id";  // ב-DogDetailsActivity שמרי על אותו שם

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

    // Add dog (Extended FAB בתחתית)
    private ExtendedFloatingActionButton fabAddDog;

    // Data
    private User displayedUser;              // האובייקט של המשתמש שמוצג
    private String displayedUserId;          // ה-UID של המשתמש שמוצג
    private String myUid;                    // ה-UID של המשתמש המחובר (אם יש)

    private final List<User> friends = new ArrayList<>();
    private RecyclerView.Adapter<?> friendsAdapter;
    private final UserRepository repo = new UserRepository();

    // Result launcher לרענון כלבים אחרי שמירה
    private ActivityResultLauncher<Intent> addDogLauncher;

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

        fabAddDog       = findViewById(R.id.fab_add_dog);

        // ===== מי המשתמש המחובר? =====
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        myUid = (me != null) ? me.getUid() : null;

        // ===== קבלת המשתמש שמוצג (מומלץ להעביר Parcelable) =====
        displayedUser = getIntent().getParcelableExtra(EXTRA_CURRENT_USER);

        // קבעי displayedUserId: עדיפות ל-UID שמגיע מהמשתמש שמוצג; אחרת נפילה חכמה ל-myUid
        if (displayedUser != null && nn(displayedUser.getUid()).length() > 0) {
            displayedUserId = displayedUser.getUid();
        } else {
            displayedUserId = myUid; // fallback – תצוגת הפרופיל שלי
        }

        // Bind UI (אם יש אובייקט מלא)
        if (displayedUser != null) {
            bindUser(displayedUser);
        } else {
            // אם אין אובייקט, לפחות נציג שהמסך שלי
            if (myUid != null && myUid.equals(displayedUserId)) {
                communityStatus.setText(getString(R.string.user_community) + " ");
            }
        }

        // ===== הכנת רשימת חברים (תמיד נקנפג את ה-RecyclerView) =====
        setupFriendsList();

        // ===== ActivityResultLauncher =====
        addDogLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && displayedUserId != null) {
                        Log.d(TAG, "Dog added, reloading dogs for uid=" + displayedUserId);
                        loadDogs(displayedUserId);
                        showDogs();
                    }
                }
        );

        // ===== Toggle default =====
        showFriends();

        btnShowFriends.setOnClickListener(v -> showFriends());
        btnShowDogs.setOnClickListener(v -> {
            showDogs();
            if (dogsContainer.getChildCount() == 0 && displayedUserId != null) {
                loadDogs(displayedUserId);
            }
        });

        // ===== טען כלבים של בעל הפרופיל שמוצג =====
        if (displayedUserId != null) {
            loadDogs(displayedUserId);
        }

        // ===== כפתור "הוסף כלב" – מופיע רק אם זה הפרופיל שלי =====
        boolean isMyProfile = (myUid != null && myUid.equals(displayedUserId));
        fabAddDog.setVisibility(isMyProfile ? View.VISIBLE : View.GONE);
        fabAddDog.setOnClickListener(v -> {
            if (!isMyProfile) return; // ביטחון
            if (myUid == null) {
                Toast.makeText(this, "לא נמצא משתמש מחובר", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent i = new Intent(ProfileActivity.this, AddDogActivity.class);
                i.putExtra(AddDogActivity.EXTRA_USER_ID, myUid);
                addDogLauncher.launch(i);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open AddDogActivity", e);
                Toast.makeText(this, "שגיאה בפתיחת מסך הוספת כלב: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ====== UI helpers ======

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
        String community = nn(user.getCommunityName());
        communityStatus.setText(getString(R.string.user_community) + (community.isEmpty() ? "" : (" " + community)));
    }

    // ====== Friends ======

    private void setupFriendsList() {
        friendsRecycler.setLayoutManager(new LinearLayoutManager(this));

        // ✅ החלפה ל-FriendsAdapter החדש
        friendsAdapter = new FriendsAdapter(this, friends, FriendsAdapter.defaultNavigator(this));

        friendsRecycler.setAdapter(friendsAdapter);

        // אם יש לנו friendsIds על המשתמש שמוצג – נטען לפיהם
        if (displayedUser != null && displayedUser.getFriendsIds() != null && !displayedUser.getFriendsIds().isEmpty()) {
            repo.getUsersByIds(displayedUser.getFriendsIds(), new UserRepository.FirestoreUsersListCallback() {
                @Override public void onSuccess(List<User> users) {
                    friends.clear();
                    friends.addAll(users);
                    friendsAdapter.notifyDataSetChanged();
                }
                @Override public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to load friends by IDs", e);
                    // fallback לפי קהילה
                    loadFriendsByCommunityFallback();
                }
            });
        } else {
            // fallback לפי קהילה
            loadFriendsByCommunityFallback();
        }
    }

    private void loadFriendsByCommunityFallback() {
        String communityName = (displayedUser != null) ? nn(displayedUser.getCommunityName()) : "";
        if (communityName.isEmpty()) {
            friends.clear();
            friendsAdapter.notifyDataSetChanged();
            return;
        }
        repo.getUsersByCommunity(communityName, new UserRepository.FirestoreUsersListCallback() {
            @Override public void onSuccess(List<User> users) {
                friends.clear();
                // אל תציגי את המשתמש עצמו ברשימה
                String selfName = (displayedUser != null) ? nn(displayedUser.getUserName()) : "";
                for (User u : users) {
                    if (!nn(u.getUserName()).equals(selfName)) friends.add(u);
                }
                friendsAdapter.notifyDataSetChanged();
            }
            @Override public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load friends by community", e);
            }
        });
    }

    // ====== Dogs ======

    /** שולף כלבים מתת-קולקציה; אם אין — מנסה לקרוא embedded מהמסמך של המשתמש */
    private void loadDogs(String userId) {
        repo.getDogsForUser(userId, new UserRepository.FirestoreDogsListCallback() {
            @Override public void onSuccess(List<Dog> dogs) {
                if (dogs != null && !dogs.isEmpty()) {
                    Log.d(TAG, "Loaded " + dogs.size() + " dogs from subcollection for uid=" + userId);
                    renderDogs(dogs);
                } else {
                    Log.d(TAG, "No dogs in subcollection, trying embedded 'dogs' on user doc");
                    repo.getUserById(userId, new UserRepository.FirestoreUserCallback() {
                        @Override public void onSuccess(User user) {
                            List<Dog> embedded = (user != null) ? user.getDogs() : null;
                            Log.d(TAG, "Embedded dogs count = " + (embedded != null ? embedded.size() : 0));
                            renderDogs(embedded);
                        }
                        @Override public void onFailure(Exception e) {
                            Log.e(TAG, "Failed to load embedded dogs", e);
                            renderDogs(null);
                        }
                    });
                }
            }
            @Override public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load dogs", e);
                Toast.makeText(ProfileActivity.this,
                        "שגיאה בטעינת כלבים: " + (e != null && e.getMessage() != null ? e.getMessage() : "לא ידועה"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** רינדור + קליק על כרטיס כלב -> מסך פרטים */
    @SuppressLint("MissingInflatedId")
    private void renderDogs(@Nullable List<Dog> dogs) {
        if (dogsContainer == null) {
            Log.e(TAG, "dogsContainer is null. בדקי את activity_profile_view.xml (id: dogs_container)");
            return;
        }
        dogsContainer.removeAllViews();
        if (dogs == null || dogs.isEmpty()) return;

        for (Dog d : dogs) {
            View card = getLayoutInflater().inflate(R.layout.item_dog_card, dogsContainer, false);
            if (card == null) { Log.e(TAG, "inflate item_dog_card returned null"); continue; }

            TextView tvName  = card.findViewById(R.id.dog_name);
            TextView tvBreed = card.findViewById(R.id.dog_breed1);
            if (tvBreed == null) tvBreed = card.findViewById(R.id.dog_breed);
            TextView tvAge   = card.findViewById(R.id.dog_age);

            if (tvName == null || tvBreed == null || tvAge == null) {
                Log.e(TAG, "item_dog_card.xml חסר IDs (dog_name/dog_breed1|dog_breed/dog_age)");
                continue;
            }

            String name  = (d != null) ? d.getName()  : null;
            String breed = (d != null) ? d.getBreed() : null;
            Integer age  = (d != null) ? d.getAge()   : null;

            tvName.setText(nn(name));
            tvBreed.setText(nn(breed));
            tvAge.setText(age != null ? String.valueOf(age) : "");

            // קליק -> DogDetailsActivity עם displayedUserId כבעלים
            card.setOnClickListener(v -> {
                if (d == null) return;
                if (displayedUserId == null || displayedUserId.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, "לא נמצא בעלים לכלב", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Intent it = new Intent(ProfileActivity.this, DogDetailsActivity.class);
                    it.putExtra(EXTRA_DOG, d);            // ודאי ש-Dog implements Parcelable
                    it.putExtra(EXTRA_OWNER_ID, displayedUserId);
                    startActivity(it);
                } catch (Exception e) {
                    Log.e(TAG, "Open DogDetailsActivity failed", e);
                    Toast.makeText(ProfileActivity.this, "שגיאה בניווט לפרטי כלב", Toast.LENGTH_SHORT).show();
                }
            });

            dogsContainer.addView(card);
        }

        showDogs();
    }

    private String nn(String s) { return (s == null) ? "" : s; }
}
