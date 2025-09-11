package com.example.pawpals;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import model.Report;
import model.ReportsAdapter;
import model.User;
import model.firebase.firestore.CommunityRepository;

public class ReportsListActivity extends AppCompatActivity {

    private RecyclerView reportsRecyclerView;
    private EditText searchEditText;

    private ReportsAdapter adapter;
    private final List<Report> allReports = new ArrayList<>();
    private final List<Report> filteredReports = new ArrayList<>();
    private User currentUser;
    private CommunityRepository communityRepo;

    private static String s(String v) {
        return v == null ? "" : v;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        // קבלת המשתמש הנוכחי
        currentUser = getIntent().getParcelableExtra("currentUser");
        communityRepo = new CommunityRepository();

        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // סינון תוך כדי הקלדה
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterReports(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // טען מהשרת
        loadReportsFromFirebase();
    }

    private void loadReportsFromFirebase() {
        if (currentUser == null || currentUser.getCommunityName() == null) {
            Toast.makeText(this, "Missing current user or community name", Toast.LENGTH_LONG).show();
            return;
        }

        String communityName = currentUser.getCommunityName();

        communityRepo.getCommunityIdByName(communityName, new CommunityRepository.FirestoreIdCallback() {
            @Override
            public void onSuccess(String communityId) {
                communityRepo.getReportsByCommunity(communityId, new CommunityRepository.FirestoreReportsListCallback() {
                    @Override
                    public void onSuccess(List<Report> reports) {
                        allReports.clear();
                        allReports.addAll(reports);

                        // שולחים ל־Adapter גם את isManager
                        adapter = new ReportsAdapter(
                                filteredReports,
                                communityId,
                                ReportsListActivity.this,
                                currentUser.isManager()
                        );
                        reportsRecyclerView.setAdapter(adapter);

                        // ✅ סנכרון רשימות - גם למחוק וגם לעדכן את המסוננים
                        adapter.setOnReportRemovedListener(report -> {
                            allReports.remove(report);
                            filterReports(searchEditText.getText().toString());
                        });

                        // הצגת כל הדיווחים בהתחלה
                        filterReports("");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(ReportsListActivity.this,
                                "Failed to load reports: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ReportsListActivity.this,
                        "Failed to find community: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filterReports(String query) {
        String q = query == null ? "" : query.toLowerCase();
        filteredReports.clear();

        if (q.isEmpty()) {
            filteredReports.addAll(allReports);
        } else {
            for (Report r : allReports) {
                if (s(r.getType()).toLowerCase().contains(q) ||
                        s(r.getSubject()).toLowerCase().contains(q) ||
                        s(r.getText()).toLowerCase().contains(q)) {
                    filteredReports.add(r);
                }
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }
}
