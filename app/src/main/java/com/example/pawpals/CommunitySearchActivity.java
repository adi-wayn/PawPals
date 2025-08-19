package com.example.pawpals;

import android.content.Intent; // ⬅️ חדש
import android.os.Bundle;
import android.util.Pair;
import android.widget.SearchView;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import model.CommunityAdapter;
import model.User;
import model.firebase.Firestore.UserRepository;

public class CommunitySearchActivity extends AppCompatActivity {

    // רשימות מקור/תצוגה
    private final List<Pair<String, User>> masterRows = new ArrayList<>();
    private final List<User> allUsers = new ArrayList<>();   // לתצוגה ב-Adapter
    private final List<String> allIds = new ArrayList<>();   // מקביל ל-allUsers
    @Nullable private User currentUser;

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;

    private ChipGroup filterChipGroup;
    private SearchView searchView;
    private CircularProgressIndicator progressBar;

    private UserRepository userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_dog_owner);

        bindViews();
        setupRecycler();
        setupSearchAndChips();

        userRepo = new UserRepository();

        // Try to get community from intent (via currentUser)
        currentUser = getIntent().getParcelableExtra("currentUser");

        String communityFromIntent = (currentUser != null) ? currentUser.getCommunityName() : null;
        if (communityFromIntent != null && !communityFromIntent.isEmpty()) {
            loadCommunityMembers(communityFromIntent);
        } else {
            // Fallback: get the logged-in user to resolve community
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid == null) {
                Toast.makeText(this, "No authenticated user found.", Toast.LENGTH_SHORT).show();
                return;
            }
            showLoading(true);
            userRepo.getUserById(uid, new UserRepository.FirestoreUserCallback() {
                @Override public void onSuccess(User user) {
                    String communityName = (user != null) ? user.getCommunityName() : null;
                    if (communityName == null || communityName.isEmpty()) {
                        showLoading(false);
                        Toast.makeText(CommunitySearchActivity.this, "Your community is not set.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    loadCommunityMembers(communityName);
                }
                @Override public void onFailure(Exception e) {
                    showLoading(false);
                    Toast.makeText(CommunitySearchActivity.this, "Failed to fetch user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void bindViews() {
        searchView       = findViewById(R.id.searchView);
        filterChipGroup  = findViewById(R.id.filterChipGroup);
        recyclerView     = findViewById(R.id.recyclerView);
        progressBar      = findViewById(R.id.progressBar);
    }

    private void setupRecycler() {
        adapter = new CommunityAdapter(allUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        // ⬅️ האזנה לקליקים על המשתמשים (ניווט לעמוד הפרופיל)
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
                filterProfiles(searchView.getQuery() != null ? searchView.getQuery().toString() : "");
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

        allUsers.clear();
        allIds.clear();

        for (Pair<String, User> row : masterRows) {
            User user = row.second;
            if (user == null || user.getUserName() == null) continue;

            boolean matchesText = user.getUserName().toLowerCase(Locale.ROOT).contains(lowerQuery);
            if (!matchesText) continue;

            if (!passesChipFilters(user, checkedChipIds)) continue;

            allUsers.add(row.second);
            allIds.add(row.first); // שמירת ה-id באותו אינדקס
        }

        adapter.updateData(allUsers);
    }

    private boolean passesChipFilters(User user, List<Integer> checkedChipIds) {
        // If no chips are checked, everything passes
        if (checkedChipIds == null || checkedChipIds.isEmpty()) return true;

        int dogCount = (user.getDogs() != null) ? user.getDogs().size() : 0;

        for (int id : checkedChipIds) {
            if (id == R.id.chipTwoPlusDogs && dogCount < 2) {
                return false;
            } else if (id == R.id.chipOneDog && dogCount != 1) {
                return false;
            } else if (id == R.id.chipHasPuppies) {
                // TODO: implement when "friends" relation exists
            }
        }
        return true;
    }

    private void showLoading(boolean show) {
        if (progressBar == null) return;
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setIndeterminate(show);
    }

    // ⬅️ מתודה לניווט לפרופיל
    private void navigateToProfile(@Nullable User user, int position) {
        if (user == null) {
            Toast.makeText(this, "Unknown profile.", Toast.LENGTH_SHORT).show();
            return;
        }
        String targetId = (position >= 0 && position < allIds.size()) ? allIds.get(position) : null;
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
}