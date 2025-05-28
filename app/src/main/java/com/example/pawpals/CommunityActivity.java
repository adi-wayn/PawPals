// CommunityActivity.java
package com.example.pawpals;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

public class CommunityActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_community_screen);
        // ניתן להוסיף לוגיקה נוספת כאן

        Button membersButton = findViewById(R.id.button10);
        membersButton.setOnClickListener(v -> {
            Intent intent = new Intent(CommunityActivity.this, CommunitySearchActivity.class);
            startActivity(intent);
        });
    }
}
