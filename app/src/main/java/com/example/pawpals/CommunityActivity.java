package com.example.pawpals;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Button;

import model.User;

public class CommunityActivity extends AppCompatActivity {

    private final User currentUser = getIntent().getParcelableExtra("currentUser");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_community_screen);

        Button membersButton = findViewById(R.id.button10);
        membersButton.setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, CommunitySearchActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        Button chatButton = findViewById(R.id.button9);
        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, ChatActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        Button reportButton = findViewById(R.id.button8);
        reportButton.setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, ReportFormActivity.class);
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });
    }
}
