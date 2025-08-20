package com.example.pawpals;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import model.Report;
import model.User;
import model.firebase.Firestore.CommunityRepository;
import model.firebase.Storage.StorageRepository;

public class WritePostManagerActivity extends AppCompatActivity {

    private EditText inputSenderName, inputSubject, inputText;
    private Button buttonSubmit;

    // תמונות
    private final List<Uri> selectedUris = new ArrayList<>();
    private androidx.recyclerview.widget.RecyclerView selectedImagesRv;
    private android.view.View addImagesBtn;

    private User currentUser;
    private final String TAG = "SingleReportForm";

    // בוחר תמונות (עד 4, תמונות בלבד)
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImages =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(4), uris -> {
                selectedUris.clear();
                if (uris != null) selectedUris.addAll(uris);
                // אפשר לחבר כאן אדפטר פריוויו; בינתיים רק מציג כמות
                Toast.makeText(this, "Selected: " + selectedUris.size(), Toast.LENGTH_SHORT).show();
                // TODO: adapter.notifyDataSetChanged() אם תוסיף אדפטר
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_post_manager);

        currentUser = getIntent().getParcelableExtra("currentUser");

        inputSenderName   = findViewById(R.id.input_sender_name);
        inputSubject      = findViewById(R.id.input_subject);
        inputText         = findViewById(R.id.input_text);
        buttonSubmit      = findViewById(R.id.button_submit);

        // תמונות
        addImagesBtn      = findViewById(R.id.button_add_images);
        selectedImagesRv  = findViewById(R.id.selectedImagesRv);
        selectedImagesRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        addImagesBtn.setOnClickListener(v ->
                pickImages.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build())
        );

        buttonSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        String senderName = inputSenderName.getText().toString().trim();
        String subject    = inputSubject.getText().toString().trim();
        String text       = inputText.getText().toString().trim();

        if (subject.isEmpty() || text.isEmpty()) {
            Toast.makeText(this, "Please fill in the subject and message.", Toast.LENGTH_SHORT).show();
            return;
        }

        Report report = new Report(Report.TYPE_POST, senderName, subject, text);
        CommunityRepository repo = new CommunityRepository();

        // 1) השג communityId לפי שם
        repo.getCommunityIdByName(currentUser.getCommunityName(), new CommunityRepository.FirestoreIdCallback() {
            @Override public void onSuccess(String communityId) {
                // 2) צור מסמך report (ללא תמונות עדיין)
                repo.createReport(communityId, report, new CommunityRepository.FirestoreCallback() {
                    @Override public void onSuccess(String reportId) {
                        Log.d(TAG, "Report created with ID: " + reportId);

                        // 3) אם אין תמונות – סיימנו
                        if (selectedUris.isEmpty()) {
                            Toast.makeText(WritePostManagerActivity.this, "Report submitted!", Toast.LENGTH_SHORT).show();
                            goBackToManagerCommunity();
                            return;
                        }

                        // 4) העלאה סדרתית ל־Storage → קבל URLs → עדכון המסמך
                        StorageRepository storage = new StorageRepository();
                        List<String> urls = new ArrayList<>();

                        uploadAllImagesSequentially(
                                0,
                                selectedUris,
                                urls,
                                (u, cb) -> storage.uploadReportImageCompressed(
                                        WritePostManagerActivity.this,
                                        communityId,
                                        reportId,
                                        u,
                                        1280,   // maxDim
                                        82,     // quality
                                        null,   // progress
                                        cb
                                ),
                                () -> {
                                    String cover = urls.isEmpty() ? null : urls.get(0);
                                    repo.updateReportImages(communityId, reportId, cover, urls,
                                            new CommunityRepository.FirestoreCallback() {
                                                @Override public void onSuccess(String ignored) {
                                                    Toast.makeText(WritePostManagerActivity.this, "Report + images submitted!", Toast.LENGTH_SHORT).show();
                                                    goBackToManagerCommunity();
                                                }
                                                @Override public void onFailure(Exception e) {
                                                    Toast.makeText(WritePostManagerActivity.this, "Saved report but failed images meta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    goBackToManagerCommunity();
                                                }
                                            });
                                },
                                e -> {
                                    Toast.makeText(WritePostManagerActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    goBackToManagerCommunity();
                                }
                        );
                    }
                    @Override public void onFailure(Exception e) {
                        Toast.makeText(WritePostManagerActivity.this, "Error saving report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to save report", e);
                    }
                });
            }
            @Override public void onFailure(Exception e) {
                Toast.makeText(WritePostManagerActivity.this, "Community not found: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Community fetch failed", e);
            }
        });

        clearForm();
    }

    // === helpers ===

    private interface Uploader {
        @Nullable com.google.firebase.storage.UploadTask go(Uri u, StorageRepository.UploadCallback cb);
    }

    private void uploadAllImagesSequentially(
            int idx,
            List<Uri> uris,
            List<String> outUrls,
            Uploader uploader,
            Runnable onDone,
            Consumer<Exception> onError
    ) {
        if (idx >= uris.size()) { onDone.run(); return; }
        Uri u = uris.get(idx);
        uploader.go(u, new StorageRepository.UploadCallback() {
            @Override public void onSuccess(@NonNull String downloadUrl) {
                outUrls.add(downloadUrl);
                uploadAllImagesSequentially(idx + 1, uris, outUrls, uploader, onDone, onError);
            }
            @Override public void onFailure(@NonNull Exception e) { onError.accept(e); }
        });
    }

    private void goBackToManagerCommunity() {
        Intent intent = new Intent(WritePostManagerActivity.this, ManagerCommunityActivity.class);
        intent.putExtra("currentUser", currentUser);
        startActivity(intent);
        finish();
    }

    private void clearForm() {
        inputSenderName.setText("");
        inputSubject.setText("");
        inputText.setText("");
        selectedUris.clear();
        // TODO: אם יש אדפטר לפריוויו: notifyDataSetChanged()
    }
}