package com.example.pawpals;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import model.Dog;
import model.FriendsAdapter;
import model.User;
import model.firebase.Firestore.UserRepository;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    // Intent keys
    public static final String EXTRA_CURRENT_USER = "currentUser";
    public static final String EXTRA_DOG = "extra_dog";
    public static final String EXTRA_OWNER_ID = "extra_owner_id";

    // Top section
    private TextView userName;
    private TextView bioText;
    private TextView contactText;
    private TextView communityStatus;

    // Toggle + content
    private MaterialButton btnShowFriends, btnShowDogs;
    private RecyclerView friendsRecycler;
    private View dogsScroll;          // ScrollView
    private LinearLayout dogsContainer;

    // Add dog (FAB)
    private MaterialButton fabAddDog;

    // --- Friends filter UI (אופציונלי) ---
    private SearchView searchViewFriends;                // @id/searchViewFriends
    private ChipGroup filterChipGroupFriends;            // @id/filterChipGroupFriends
    private CircularProgressIndicator friendsProgress;   // @id/friendsProgress

    // Data
    private User displayedUser;
    private String displayedUserId;
    private String myUid;

    // סינון/תצוגת חברים (כמו CommunitySearch)
    private final List<Pair<String, User>> masterRows   = new ArrayList<>();
    private final List<Pair<String, User>> filteredRows = new ArrayList<>();
    private final Set<String> myFriendIds               = new HashSet<>();
    private final List<User> viewUsers                  = new ArrayList<>();
    private FriendsAdapter friendsAdapter;

    private final UserRepository repo = new UserRepository();

    // מאזינים בזמן אמת
    @Nullable private ListenerRegistration displayedFriendsReg; // חברים של המשתמש המוצג
    @Nullable private ListenerRegistration myFriendsIdsReg;     // ids של החברים שלי (ל-chipFriends)

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

        // (אופציונלי – רק אם יש את ה־IDs ב־XML)
        //searchViewFriends      = findViewById(R.id.searchViewFriends);
      //  filterChipGroupFriends = findViewById(R.id.filterChipGroupFriends);
       // friendsProgress        = findViewById(R.id.friendsProgress);

        // ===== מי המשתמש המחובר? =====
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        myUid = (me != null) ? me.getUid() : null;

        // ===== קבלת המשתמש שמוצג =====
        displayedUser = getIntent().getParcelableExtra(EXTRA_CURRENT_USER);
        displayedUserId = (displayedUser != null && nn(displayedUser.getUid()).length() > 0)
                ? displayedUser.getUid()
                : myUid;

        // Bind UI
        if (displayedUser != null) {
            bindUser(displayedUser);
        } else if (myUid != null && myUid.equals(displayedUserId)) {
            communityStatus.setText(getString(R.string.user_community) + " ");
        }

        // ===== Friends: Recycler + מאזיני סינון =====
        setupFriendsSection();

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

        // טען כלבים של בעל הפרופיל שמוצג
        if (displayedUserId != null) {
            loadDogs(displayedUserId);
        }

        // כפתור "הוסף כלב"
        boolean isMyProfile = (myUid != null && myUid.equals(displayedUserId));
        fabAddDog.setVisibility(isMyProfile ? View.VISIBLE : View.GONE);
        fabAddDog.setOnClickListener(v -> {
            if (!isMyProfile) return;
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

    @Override
    protected void onStart() {
        super.onStart();

        // 1) מאזין בזמן אמת לחברי המשתמש המוצג (תמיד רלוונטי למסך)
        if (displayedFriendsReg != null) { displayedFriendsReg.remove(); displayedFriendsReg = null; }
        if (displayedUserId != null && !displayedUserId.isEmpty()) {
            showFriendsLoading(true);
            displayedFriendsReg = repo.observeFriendsUsers(displayedUserId, new UserRepository.FirestoreUsersListCallback() {
                @Override public void onSuccess(List<User> users) {
                    // מתעדכן אוטומטית בכל שינוי add/remove
                    masterRows.clear();
                    if (users != null) {
                        for (User u : users) {
                            if (u != null && nn(u.getUid()).length() > 0) {
                                masterRows.add(new Pair<>(u.getUid(), u));
                            }
                        }
                    }
                    showFriendsLoading(false);
                    filterFriends(getCurrentQuery());
                }
                @Override public void onFailure(Exception e) {
                    Log.e(TAG, "observeFriendsUsers failed", e);
                    showFriendsLoading(false);
                    // אם תרצי – אפשר לבצע כאן fallback לקהילה. כרגע נשאיר ריק (רשימה ריקה).
                    filterFriends(getCurrentQuery());
                }
            });
        }

        // 2) מאזין לרשימת החברים שלי – רק לשם פילטר "חברים שלי"
        if (myFriendsIdsReg != null) { myFriendsIdsReg.remove(); myFriendsIdsReg = null; }
        if (myUid != null) {
            myFriendsIdsReg = repo.observeFriendsIds(myUid, (qs, err) -> {
                myFriendIds.clear();
                if (err == null && qs != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot d : qs.getDocuments()) {
                        myFriendIds.add(d.getId());
                    }
                }
                filterFriends(getCurrentQuery());
            });
        }
    }

    @Override
    protected void onStop() {
        if (displayedFriendsReg != null) { displayedFriendsReg.remove(); displayedFriendsReg = null; }
        if (myFriendsIdsReg != null)   { myFriendsIdsReg.remove();   myFriendsIdsReg = null; }
        super.onStop();
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
        bioText.setText(nn(user.getFieldsOfInterest()));
        contactText.setText(nn(user.getContactDetails()));
        String community = nn(user.getCommunityName());
        communityStatus.setText(getString(R.string.user_community) + (community.isEmpty() ? "" : (" " + community)));
    }

    // ====== Friends (כמו CommunitySearch) ======
    private void setupFriendsSection() {
        friendsRecycler.setLayoutManager(new LinearLayoutManager(this));
        friendsAdapter = new FriendsAdapter(this, viewUsers, FriendsAdapter.defaultNavigator(this));
        friendsRecycler.setAdapter(friendsAdapter);

        if (searchViewFriends != null) {
            searchViewFriends.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String q)  { filterFriends(q);     return true; }
                @Override public boolean onQueryTextChange(String q) { filterFriends(q);     return true; }
            });
        }
        if (filterChipGroupFriends != null) {
            filterChipGroupFriends.setOnCheckedStateChangeListener((g, ids) -> filterFriends(getCurrentQuery()));
        }
    }

    /** סינון טקסט + צ'יפים ("חברים שלי" / "כלב אחד" / "2+") */
    private void filterFriends(String query) {
        final String lowerQuery = (query == null ? "" : query).toLowerCase(Locale.ROOT);
        final List<Integer> checkedIds = (filterChipGroupFriends != null)
                ? filterChipGroupFriends.getCheckedChipIds()
                : java.util.Collections.emptyList();

        final boolean requireFriend =
                checkedIds != null && checkedIds.contains(R.id.chipFriends);
        final boolean needDogCount =
                checkedIds != null && (checkedIds.contains(R.id.chipOneDog) || checkedIds.contains(R.id.chipTwoPlusDogs));

        filteredRows.clear();

        // שלב א' – סינון טקסט + "חברים שלי"
        List<Pair<String, User>> preFiltered = new ArrayList<>();
        for (Pair<String, User> row : masterRows) {
            if (row == null) continue;
            String uid = row.first;
            User user = row.second;
            if (uid == null || user == null || user.getUserName() == null) continue;

            if (requireFriend && !myFriendIds.contains(uid)) continue;

            boolean matchesText = user.getUserName().toLowerCase(Locale.ROOT).contains(lowerQuery);
            if (!matchesText) continue;

            preFiltered.add(row);
        }

        if (!needDogCount) {
            filteredRows.addAll(preFiltered);
            updateFriendsAdapter();
            return;
        }

        // שלב ב' – סינון לפי כמות כלבים (קריאות אסינכרוניות)
        if (preFiltered.isEmpty()) {
            updateFriendsAdapter();
            return;
        }

        final int total = preFiltered.size();
        final int[] completed = {0};
        filteredRows.clear();

        for (Pair<String, User> row : preFiltered) {
            String userId = row.first;
            repo.getDogsForUser(userId, new UserRepository.FirestoreDogsListCallback() {
                @Override public void onSuccess(List<Dog> dogs) {
                    int dogCount = (dogs != null) ? dogs.size() : 0;
                    if (passesDogCountFilters(dogCount, checkedIds)) filteredRows.add(row);
                    if (++completed[0] == total) updateFriendsAdapter();
                }
                @Override public void onFailure(Exception e) {
                    if (++completed[0] == total) updateFriendsAdapter();
                }
            });
        }
    }

    private boolean passesDogCountFilters(int dogCount, List<Integer> checkedIds) {
        if (checkedIds == null || checkedIds.isEmpty()) return true;
        for (int id : checkedIds) {
            if (id == R.id.chipTwoPlusDogs && dogCount < 2) return false;
            else if (id == R.id.chipOneDog && dogCount != 1) return false;
        }
        return true;
    }

    private void updateFriendsAdapter() {
        viewUsers.clear();
        for (Pair<String, User> p : filteredRows) {
            if (p != null && p.second != null) viewUsers.add(p.second);
        }
        if (friendsAdapter != null) friendsAdapter.notifyDataSetChanged();
    }

    private String getCurrentQuery() {
        if (searchViewFriends == null || searchViewFriends.getQuery() == null) return "";
        return searchViewFriends.getQuery().toString();
    }

    private void showFriendsLoading(boolean show) {
        if (friendsProgress != null) {
            friendsProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            friendsProgress.setIndeterminate(show);
        }
    }

    // ====== Dogs ======
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
            TextView tvBreed = card.findViewById(R.id.dog_breed);
            TextView tvAge   = card.findViewById(R.id.dog_age);

            if (tvName == null || tvBreed == null || tvAge == null) {
                Log.e(TAG, "item_dog_card.xml חסר IDs (dog_name/dog_breed/dog_age)");
                continue;
            }

            String name  = (d != null) ? d.getName()  : null;
            String breed = (d != null) ? d.getBreed() : null;
            Integer age  = (d != null) ? d.getAge()   : null;

            tvName.setText(nn(name));
            tvBreed.setText(nn(breed));
            tvAge.setText(age != null ? String.valueOf(age) : "");

            card.setOnClickListener(v -> {
                if (d == null) return;
                if (displayedUserId == null || displayedUserId.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, "לא נמצא בעלים לכלב", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Intent it = new Intent(ProfileActivity.this, DogDetailsActivity.class);
                    it.putExtra(EXTRA_DOG, d);
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
