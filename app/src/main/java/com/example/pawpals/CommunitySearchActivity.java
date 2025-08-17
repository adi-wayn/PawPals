package com.example.pawpals;

import android.content.Intent; // ⬅️ חדש
import android.os.Bundle;
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
import model.firebase.UserRepository;

public class CommunitySearchActivity extends AppCompatActivity {

    @Nullable private User currentUser;

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;

    // masterProfiles: full community members (no filters)
    private final List<User> masterProfiles = new ArrayList<>();
    // allProfiles: filtered list shown in the adapter
    private final List<User> allProfiles = new ArrayList<>();

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
        adapter = new CommunityAdapter(allProfiles);
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
        userRepo.getUsersByCommunity(communityName, new UserRepository.FirestoreUsersListCallback() {
            @Override
            public void onSuccess(List<User> users) {
                showLoading(false);

                masterProfiles.clear();
                if (users != null) {
                    // Optional: exclude the current logged-in user from results
                    String selfId = FirebaseAuth.getInstance().getUid();
                    for (User u : users) {
                        if (u == null) continue;
                        // If your User has an id field, you can exclude self:
                        // if (u.getId() != null && u.getId().equals(selfId)) continue;
                        masterProfiles.add(u);
                    }
                }

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

        List<User> filtered = new ArrayList<>();
        for (User user : masterProfiles) {
            if (user == null || user.getUserName() == null) continue;

            boolean matchesText = user.getUserName().toLowerCase(Locale.ROOT).contains(lowerQuery);
            if (!matchesText) continue;

            if (!passesChipFilters(user, checkedChipIds)) continue;

            filtered.add(user);
        }

        allProfiles.clear();
        allProfiles.addAll(filtered);
        adapter.updateData(allProfiles);
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
    private void navigateToProfile(@Nullable User user) {
        if (user == null) {
            Toast.makeText(this, "Unknown profile.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("currentUser", user); // Requires User implements Parcelable
        startActivity(intent);
    }
}
