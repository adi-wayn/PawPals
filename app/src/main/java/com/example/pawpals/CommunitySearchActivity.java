package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import model.CommunityAdapter;
import model.Dog;
import model.User;
import model.firebase.Firestore.UserRepository;

public class CommunitySearchActivity extends AppCompatActivity {

    private final List<Pair<String, User>> masterRows = new ArrayList<>();
    private final List<Pair<String, User>> filteredRows = new ArrayList<>();

    @Nullable private User currentUser;
    @Nullable private String selfId;
    private final Set<String> myFriendIds = new HashSet<>();
    @Nullable private ListenerRegistration friendReg;

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;
    private ChipGroup filterChipGroup;
    private SearchView searchView;
    private CircularProgressIndicator progressBar;
    private UserRepository userRepo;
    private boolean communityLoaded = false;
    private boolean friendsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_dog_owner);

        selfId = FirebaseAuth.getInstance().getUid();

        bindViews();
        setupRecycler();
        setupSearchAndChips();

        userRepo = new UserRepository();
        currentUser = getIntent().getParcelableExtra("currentUser");

        // מעקב אחרי רשימת חברים
        if (selfId != null) {
            friendReg = userRepo.observeFriendsIds(selfId, (qs, err) -> {
                myFriendIds.clear();
                if (err == null && qs != null) {
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        myFriendIds.add(d.getId());
                        Log.d("CommunitySearch", "Friend ID added: " + d.getId());
                    }
                }
                Log.d("CommunitySearch", "Total friends loaded: " + myFriendIds.size());
                friendsLoaded = true;
                filterProfiles(searchView.getQuery() != null ? searchView.getQuery().toString() : "");
            });
        }

        // טעינת משתמשים מהקהילה
        String communityFromIntent = (currentUser != null) ? currentUser.getCommunityName() : null;
        if (communityFromIntent != null && !communityFromIntent.isEmpty()) {
            loadCommunityMembers(communityFromIntent);
        } else {
            // אם אין בקהילה, נטען את המשתמש המחובר
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid == null) {
                Toast.makeText(this, "No authenticated user found.", Toast.LENGTH_SHORT).show();
                return;
            }
            showLoading(true);
            userRepo.getUserById(uid, new UserRepository.FirestoreUserCallback() {
                @Override
                public void onSuccess(User user) {
                    String communityName = (user != null) ? user.getCommunityName() : null;
                    if (communityName == null || communityName.isEmpty()) {
                        showLoading(false);
                        Toast.makeText(CommunitySearchActivity.this, "Your community is not set.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    loadCommunityMembers(communityName);
                }

                @Override
                public void onFailure(Exception e) {
                    showLoading(false);
                    Toast.makeText(CommunitySearchActivity.this, "Failed to fetch user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void bindViews() {
        searchView      = findViewById(R.id.searchView);
        filterChipGroup = findViewById(R.id.filterChipGroup);
        recyclerView    = findViewById(R.id.recyclerView);
        progressBar     = findViewById(R.id.progressBar);
    }

    private void setupRecycler() {
        adapter = new CommunityAdapter(filteredRows);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        adapter.setOnUserClickListener(this::navigateToProfile);
    }

    private void setupSearchAndChips() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                filterProfiles(query);
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) {
                filterProfiles(newText);
                return true;
            }
        });

        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) ->
                filterProfiles(searchView.getQuery() != null ? searchView.getQuery().toString() : "")
        );
    }

    private void loadCommunityMembers(String communityName) {
        showLoading(true);
        userRepo.getUsersByCommunityWithIds(communityName, new UserRepository.FirestoreUsersWithIdsCallback() {
            @Override
            public void onSuccess(List<Pair<String, User>> rows) {
                showLoading(false);
                masterRows.clear();
                if (rows != null) masterRows.addAll(rows);
                communityLoaded = true;
                if (friendsLoaded) { // רק אם גם רשימת חברים כבר נטענה
                    filterProfiles(searchView.getQuery() != null ? searchView.getQuery().toString() : "");
                }
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(CommunitySearchActivity.this, "Failed to fetch community members: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterProfiles(String query) {
        final String lowerQuery = (query == null ? "" : query).toLowerCase(Locale.ROOT);
        final List<Integer> checkedChipIds = filterChipGroup.getCheckedChipIds();
        final boolean requireFriend = checkedChipIds != null && checkedChipIds.contains(R.id.chipFriends);

        Log.d("CommunitySearch", "Filtering profiles... requireFriend=" + requireFriend
                + ", myFriendIds=" + myFriendIds
                + ", masterRows=" + masterRows.size());

        filteredRows.clear();

        // רשימה זמנית לאסוף מי שעובר את הפילטרים הראשוניים
        List<Pair<String, User>> preFiltered = new ArrayList<>();

        for (Pair<String, User> row : masterRows) {
            String userId = row.first;
            User user = row.second;

            if (selfId != null && selfId.equals(userId)) continue;
            if (requireFriend && !myFriendIds.contains(userId)) continue;
            if (user == null || user.getUserName() == null) continue;

            boolean matchesText = user.getUserName().toLowerCase(Locale.ROOT).contains(lowerQuery);
            if (!matchesText) continue;

            // נעביר לשלב הבא (בדיקה לפי כלבים)
            preFiltered.add(row);
        }


        // במידה ויש פילטר לפי כמות כלבים – נטען זאת דרך Firestore
        UserRepository repo = new UserRepository();
        final int total = preFiltered.size();
        final int[] completed = {0};

        if (checkedChipIds == null || checkedChipIds.isEmpty() ||
                (!checkedChipIds.contains(R.id.chipOneDog) && !checkedChipIds.contains(R.id.chipTwoPlusDogs))) {
            // אם לא מסונן לפי כלבים בכלל — אל תיכנס ללולאת ה-getDogsForUser
            filteredRows.addAll(preFiltered);
            adapter.updateData(filteredRows);
            return;
        }

        for (Pair<String, User> row : preFiltered) {
            String userId = row.first;
            User user = row.second;

            repo.getDogsForUser(userId, new UserRepository.FirestoreDogsListCallback() {
                @Override
                public void onSuccess(List<Dog> dogs) {
                    int dogCount = (dogs != null) ? dogs.size() : 0;
                    if (passesDogCountFilters(dogCount, checkedChipIds)) {
                        filteredRows.add(row);
                    }
                    if (++completed[0] == total) {
                        adapter.updateData(filteredRows);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (++completed[0] == total) {
                        adapter.updateData(filteredRows);
                    }
                }
            });
        }
    }

    private boolean passesDogCountFilters(int dogCount, List<Integer> checkedChipIds) {
        if (checkedChipIds == null || checkedChipIds.isEmpty()) return true;
        for (int id : checkedChipIds) {
            if (id == R.id.chipTwoPlusDogs && dogCount < 2) return false;
            else if (id == R.id.chipOneDog && dogCount != 1) return false;
        }
        return true;
    }

    private void showLoading(boolean show) {
        if (progressBar == null) return;
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setIndeterminate(show);
    }

    private void navigateToProfile(@Nullable User user, int position) {
        if (user == null || position < 0 || position >= filteredRows.size()) {
            Toast.makeText(this, "Unknown profile.", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetId = filteredRows.get(position).first;
        if (targetId == null || targetId.isEmpty()) {
            Toast.makeText(this, "Missing user id.", Toast.LENGTH_SHORT).show();
            return;
        }

        String selfId = FirebaseAuth.getInstance().getUid();
        if (selfId != null && selfId.equals(targetId)) {
            Intent i = new Intent(this, ProfileActivity.class);
            i.putExtra("currentUser", user);
            startActivity(i);
        } else {
            Intent i = new Intent(this, OtherUserProfileActivity.class);
            i.putExtra(OtherUserProfileActivity.EXTRA_OTHER_USER_ID, targetId);
            startActivity(i);
        }
    }

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