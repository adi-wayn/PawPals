package com.example.pawpals;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Button;
import model.User;

public class CommunityActivity extends AppCompatActivity {

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        // נטילת המשתמש מה-Intent
        currentUser = getIntent().getParcelableExtra("currentUser");

        // כפתור חברים
        Button membersButton = findViewById(R.id.buttonMembers);
        membersButton.setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, CommunitySearchActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // כפתור צ'אט
        Button chatButton = findViewById(R.id.buttonChat);
        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, ChatActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // כפתור מערכת דיווח
        Button reportButton = findViewById(R.id.buttonReportSystem);
        reportButton.setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, ReportFormActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // כפתור מפת אזור (חדש)
        Button areaMapButton = findViewById(R.id.buttonAreaMap);
        areaMapButton.setOnClickListener(v -> {
            // כאן אפשר לפתוח Activity חדש שקשור למפה (לדוגמה AreaMapActivity)
            //יוד לא מימשנוא את כל הפונקציות
            Intent intent = new Intent(CommunityActivity.this, AreaMapActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });
    }
}
