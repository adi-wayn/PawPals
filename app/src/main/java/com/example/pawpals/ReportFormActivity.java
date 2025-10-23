package com.example.pawpals;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import model.Report;
import model.SelectedImagesAdapter;
import model.User;
import model.firebase.Storage.StorageRepository;
import model.firebase.Firestore.CommunityRepository;

public class ReportFormActivity extends AppCompatActivity {
    private static final String TAG = "ReportFormActivity";

    private RadioGroup typeTabs;
    private EditText inputSenderName, inputSubject, inputText;
    private Button buttonSubmit;
    private User currentUser;

    // תמונות
    private final List<Uri> selectedUris = new ArrayList<>();
    private androidx.recyclerview.widget.RecyclerView selectedImagesRv;
    private View addImagesBtn;
    private SelectedImagesAdapter previewAdapter;

    // בחירת תמונות (מקסימום 4)
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImages =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(4), uris -> {
                Log.d(TAG, "pickImages callback: uris=" + (uris != null ? uris.size() : 0));
                selectedUris.clear();
                if (uris != null) selectedUris.addAll(uris);
                previewAdapter.submit(selectedUris);
                selectedImagesRv.setVisibility(selectedUris.isEmpty() ? View.GONE : View.VISIBLE);
                Toast.makeText(this, "Selected: " + selectedUris.size(), Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_form);

        currentUser = getIntent().getParcelableExtra("currentUser");
        if (currentUser == null) {
            Log.e(TAG, "currentUser is null — finishing activity.");
            finish();
            return;
        }

        typeTabs = findViewById(R.id.type_tabs);
        inputSenderName = findViewById(R.id.input_sender_name);
        inputSubject = findViewById(R.id.input_subject);
        inputText = findViewById(R.id.input_text);
        buttonSubmit = findViewById(R.id.button_submit);
        addImagesBtn = findViewById(R.id.button_add_images);
        selectedImagesRv = findViewById(R.id.selectedImagesRv);

        // === RecyclerView & Adapter לתמונות ===
        selectedImagesRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        previewAdapter = new SelectedImagesAdapter(selectedUris, pos -> {
            if (pos >= 0 && pos < selectedUris.size()) {
                Log.d(TAG, "Removing image at position " + pos);
                selectedUris.remove(pos);
                previewAdapter.notifyItemRemoved(pos);
                selectedImagesRv.setVisibility(selectedUris.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
        selectedImagesRv.setAdapter(previewAdapter);
        selectedImagesRv.setVisibility(View.GONE);

        // כפתור בחירת תמונות
        addImagesBtn.setOnClickListener(v -> {
            Log.d(TAG, "Launching image picker...");
            pickImages.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        CommunityRepository repo = new CommunityRepository();

        // בדיקת זמינות של בקשות מנהל
        repo.getCommunityIdByName(currentUser.getCommunityName(), new CommunityRepository.FirestoreIdCallback() {
            @Override
            public void onSuccess(String communityId) {
                repo.getManagerApplicationsOpen(communityId, new CommunityRepository.FirestoreBooleanCallback() {
                    @Override
                    public void onSuccess(boolean value) {
                        RadioButton tabManagerApp = findViewById(R.id.tab_manager_application);
                        Log.d(TAG, "getManagerApplicationsOpen success. value=" + value);
                        if (tabManagerApp != null) {
                            tabManagerApp.setVisibility(value ? View.VISIBLE : View.GONE);
                            Log.d(TAG, "Manager Application tab visibility set to " +
                                    (value ? "GONE" : "VISIBLE"));
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to check manager applications", e);
                        RadioButton tabManagerApp = findViewById(R.id.tab_manager_application);
                        if (tabManagerApp != null) tabManagerApp.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Community not found", e);
                Toast.makeText(ReportFormActivity.this, "Community not found: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        buttonSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        String type = getSelectedType();
        String senderName = inputSenderName.getText().toString().trim();
        String subject = inputSubject.getText().toString().trim();
        String text = inputText.getText().toString().trim();

        Log.d(TAG, "Submitting report. Type=" + type + ", images=" + selectedUris.size());

        if (subject.isEmpty() || text.isEmpty() || type.equals("No type selected")) {
            Toast.makeText(this, "Please fill in at least the type, subject and message.", Toast.LENGTH_SHORT).show();
            return;
        }

        Report report = new Report(type, senderName, subject, text);
        String uid = FirebaseAuth.getInstance().getUid();
        report.setSenderId(uid);

        if ("Manager Application".equalsIgnoreCase(type)) {
            report.setApplicantUserId(uid);
            report.setType(Report.TYPE_MANAGER_APPLICATION);
        }

        CommunityRepository repo = new CommunityRepository();

        repo.getCommunityIdByName(currentUser.getCommunityName(), new CommunityRepository.FirestoreIdCallback() {
            @Override
            public void onSuccess(String communityId) {
                Log.d(TAG, "Community found: " + communityId);
                repo.createReport(communityId, report, new CommunityRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess(String reportId) {
                        Log.d(TAG, "Report created. id=" + reportId);
                        if (selectedUris.isEmpty()) {
                            finishAfterSubmit();
                            return;
                        }

                        StorageRepository storageRepo = new StorageRepository();
                        List<String> urls = new ArrayList<>();

                        uploadAllImagesSequentially(0, selectedUris, urls,
                                (u, c) -> storageRepo.uploadReportImageCompressed(
                                        ReportFormActivity.this, communityId, reportId, u, 1280, 82, null, c),
                                () -> repo.updateReportImages(communityId, reportId, null, urls,
                                        new CommunityRepository.FirestoreCallback() {
                                            @Override
                                            public void onSuccess(String id) {
                                                Log.d(TAG, "Images updated successfully");
                                                finishAfterSubmit();
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                Log.e(TAG, "Failed to update images", e);
                                                finishAfterSubmit();
                                            }
                                        }),
                                e -> {
                                    Log.e(TAG, "Image upload failed", e);
                                    Toast.makeText(ReportFormActivity.this,
                                            "Image upload failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Error saving report", e);
                        Toast.makeText(ReportFormActivity.this,
                                "Error saving report: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Community not found", e);
                Toast.makeText(ReportFormActivity.this,
                        "Community not found: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // === Helpers ===
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
        if (idx >= uris.size()) {
            onDone.run();
            return;
        }
        Uri u = uris.get(idx);
        Log.d(TAG, "Uploading image " + (idx + 1) + "/" + uris.size() + ": " + u);
        uploader.go(u, new StorageRepository.UploadCallback() {
            @Override
            public void onSuccess(@NonNull String downloadUrl) {
                Log.d(TAG, "Image uploaded. URL=" + downloadUrl);
                outUrls.add(downloadUrl);
                uploadAllImagesSequentially(idx + 1, uris, outUrls, uploader, onDone, onError);
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Image upload failed at index " + idx, e);
                onError.accept(e);
            }
        });
    }

    private void finishAfterSubmit() {
        Toast.makeText(this, "Report submitted!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, CommunityActivity.class);
        intent.putExtra("currentUser", currentUser);
        startActivity(intent);
        finish();
    }

    private String getSelectedType() {
        int selectedId = typeTabs.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadio = findViewById(selectedId);
            return selectedRadio.getText().toString();
        }
        return "No type selected";
    }
}