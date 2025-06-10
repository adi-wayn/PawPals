package com.example.pawpals;

import android.os.Bundle;
import android.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import model.CommunityAdapter;
import model.User;

public class CommunitySearchActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;
    private List<User> allProfiles;
    private ChipGroup filterChipGroup;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_dog_owner);

        searchView = findViewById(R.id.searchView);
        filterChipGroup = findViewById(R.id.filterChipGroup);
        recyclerView = findViewById(R.id.recyclerView);

        //allProfiles = loadDummyProfiles();
        adapter = new CommunityAdapter(allProfiles);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // SearchView handling
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterProfiles(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterProfiles(newText);
                return true;
            }
        });

        // Chip filtering
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterProfiles(searchView.getQuery().toString());
        });
    }

    private void filterProfiles(String query) {
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        List<User> filtered = new ArrayList<>();

        for (User user : allProfiles) {
            boolean matchesText = user.getUserName().toLowerCase(Locale.ROOT).contains(lowerQuery);

            boolean matchesFilter = true;
            List<Integer> chipIds = filterChipGroup.getCheckedChipIds();

            for (int chipId : chipIds) {
                Chip chip = filterChipGroup.findViewById(chipId);
                String label = chip.getText().toString().toLowerCase(Locale.ROOT);

                // Simplified demo filters:
                if (label.contains("2+dogs") && user.getDogs().size() < 2) {
                    matchesFilter = false;
                } else if (label.contains("dog 1") && user.getDogs().size() != 1) {
                    matchesFilter = false;
                }
            }

            if (matchesText && matchesFilter) {
                filtered.add(user);
            }
        }

        adapter.updateData(filtered);
    }
}
