package com.example.pawpals;

import android.os.Bundle;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

// ReportMapActivity.java – מסך הבחירה בלבד
public class ReportMapActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_reports);

        findViewById(R.id.button_inspector).setOnClickListener(v -> returnWithType("Dog Patrol"));
        findViewById(R.id.button_bin).setOnClickListener(v -> returnWithType("Trash Bin"));
        findViewById(R.id.button_danger).setOnClickListener(v -> returnWithType("Danger"));
        findViewById(R.id.button_help).setOnClickListener(v -> returnWithType("Help"));
    }

    private void returnWithType(String type) {
        Intent result = new Intent();
        result.putExtra("selectedType", type);
        setResult(RESULT_OK, result);
        finish();
    }
}
