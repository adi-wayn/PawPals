package com.example.pawpals;

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
import model.User;
import model.firebase.firestore.UserRepository;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

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
    private User currentUser;
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

        // ===== ActivityResultLauncher =====
        addDogLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                        if (me != null) {
                            Log.d(TAG, "Dog added, reloading dogs for uid=" + me.getUid());
                            loadDogs(me.getUid());
                            showDogs(); // הציגי את הטאב של הכלבים אחרי שמירה
                        }
                    }
                }
        );

        // ===== Toggle default =====
        showFriends(); // ברירת מחדל: חברים

        btnShowFriends.setOnClickListener(v -> showFriends());
        btnShowDogs.setOnClickListener(v -> showDogs());

        // ===== קבלת המשתמש (מומלץ להעביר כ-Parcelable) =====
        currentUser = getIntent().getParcelableExtra("currentUser");
        if (currentUser != null) {
            bindUser(currentUser);
            setupFriendsList(currentUser.getCommunityName());
        }

        // טען כלבים של המשתמש המחובר (הנתונים יושבים בתת-קולקציה; יש גם Fallback ל-embedded)
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me != null) {
            loadDogs(me.getUid());
        }

        // ===== כפתור "הוסף כלב" – מעבר למסך ההוספה =====
        fabAddDog.setOnClickListener(v -> {
            FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
            String myUid = (cur != null) ? cur.getUid() : null;
            if (myUid == null) {
                Toast.makeText(this, "לא נמצא משתמש מחובר", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent i = new Intent(ProfileActivity.this, AddDogActivity.class);
                i.putExtra(AddDogActivity.EXTRA_USER_ID, myUid); // שימוש בקבוע מה-AddDogActivity
                addDogLauncher.launch(i);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open AddDogActivity", e);
                Toast.makeText(this, "שגיאה בפתיחת מסך הוספת כלב: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** שולף כלבים מתת-קולקציה; אם אין — מנסה לקרוא embedded מהמסמך של המשתמש */
    private void loadDogs(String userId) {
        repo.getDogsForUser(userId, new UserRepository.FirestoreDogsListCallback() {
            @Override public void onSuccess(List<Dog> dogs) {
                if (dogs != null && !dogs.isEmpty()) {
                    Log.d(TAG, "Loaded " + dogs.size() + " dogs from subcollection");
                    renderDogs(dogs);
                } else {
                    Log.d(TAG, "No dogs in subcollection, trying embedded 'dogs' on user doc");
                    // Fallback: embedded dogs במידת הצורך
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
        communityStatus.setText(getString(R.string.user_community) + " " + nn(user.getCommunityName()));
    }

    private void setupFriendsList(String communityName) {
        friendsRecycler.setLayoutManager(new LinearLayoutManager(this));
        friendsAdapter = new model.CommunityAdapter(friends);
        friendsRecycler.setAdapter(friendsAdapter);

        if (communityName == null || communityName.isEmpty()) return;

        repo.getUsersByCommunity(communityName, new UserRepository.FirestoreUsersListCallback() {
            @Override public void onSuccess(List<User> users) {
                friends.clear();
                if (currentUser != null && currentUser.getUserName() != null) {
                    for (User u : users) {
                        if (u.getUserName() != null && u.getUserName().equals(currentUser.getUserName())) continue;
                        friends.add(u);
                    }
                } else {
                    friends.addAll(users);
                }
                friendsAdapter.notifyDataSetChanged();
            }
            @Override public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load friends", e);
            }
        });
    }

    /** רינדור בטוח: לא קורס על IDs חסרים/ערכים null */
    private void renderDogs(List<Dog> dogs) {
        if (dogsContainer == null) {
            Log.e(TAG, "dogsContainer is null. בדקי את activity_profile_view.xml (id: dogs_container)");
            return;
        }
        dogsContainer.removeAllViews();
        if (dogs == null || dogs.isEmpty()) return;

        for (Dog d : dogs) {
            View card = getLayoutInflater().inflate(R.layout.item_profile_card, dogsContainer, false);
            if (card == null) { Log.e(TAG, "inflate item_profile_card returned null"); continue; }

            TextView tvName  = card.findViewById(R.id.dog_name);
            TextView tvBreed = card.findViewById(R.id.dog_breed);
            TextView tvAge   = card.findViewById(R.id.dog_age);

            if (tvName == null || tvBreed == null || tvAge == null) {
                Log.e(TAG, "item_profile_card.xml חסר IDs (dog_name/dog_breed/dog_age) – בדקי את ה-XML");
                continue; // לא מפילים מסך על layout שגוי
            }

            String name  = d != null ? d.getName()  : null;
            String breed = d != null ? d.getBreed() : null;
            Integer age  = d != null ? d.getAge()   : null;

            tvName.setText(nn(name));
            tvBreed.setText(nn(breed));
            tvAge.setText(age != null ? String.valueOf(age) : "");

            dogsContainer.addView(card);
        }
    }

    private String nn(String s) { return s == null ? "" : s; }
}
