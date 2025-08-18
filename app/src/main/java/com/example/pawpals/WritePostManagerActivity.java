package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import model.Report;
import model.User;
import model.firebase.firestore.CommunityRepository;

public class WritePostManagerActivity extends AppCompatActivity {

    private EditText inputSenderName, inputSubject, inputText;
    private Button buttonSubmit;
    private User currentUser;
    private final String TAG = "SingleReportForm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_post_manager);

        currentUser = getIntent().getParcelableExtra("currentUser");

        inputSenderName = findViewById(R.id.input_sender_name);
        inputSubject = findViewById(R.id.input_subject);
        inputText = findViewById(R.id.input_text);
        buttonSubmit = findViewById(R.id.button_submit);

        buttonSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        String senderName = inputSenderName.getText().toString().trim();
        String subject = inputSubject.getText().toString().trim();
        String text = inputText.getText().toString().trim();

        if (subject.isEmpty() || text.isEmpty()) {
            Toast.makeText(this, "Please fill in the subject and message.", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = "Post"; // רק סוג אחד

        Report report = new Report(type, senderName, subject, text);
        CommunityRepository repo = new CommunityRepository();

        repo.getCommunityIdByName(currentUser.getCommunityName(), new CommunityRepository.FirestoreIdCallback() {
            @Override
            public void onSuccess(String communityId) {
                repo.createReport(communityId, report, new CommunityRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess(String documentId) {
                        Toast.makeText(WritePostManagerActivity.this, "Report submitted!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Report created with ID: " + documentId);

                        Intent intent = new Intent(WritePostManagerActivity.this, ManagerCommunityActivity.class);
                        intent.putExtra("currentUser", currentUser);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(WritePostManagerActivity.this, "Error saving report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to save report", e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(WritePostManagerActivity.this, "Community not found: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Community fetch failed", e);
            }
        });

        clearForm();
    }

    private void clearForm() {
        inputSenderName.setText("");
        inputSubject.setText("");
        inputText.setText("");
    }
}
