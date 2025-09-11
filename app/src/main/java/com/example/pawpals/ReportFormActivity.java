package com.example.pawpals;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;

import model.Report;
import model.SelectedImagesAdapter;
import model.User;
import model.firebase.firestore.CommunityRepository;
import model.firebase.Storage.StorageRepository;

public class ReportFormActivity extends AppCompatActivity {
    private RadioGroup typeTabs;
    private EditText inputSenderName, inputSubject, inputText;
    private Button buttonSubmit;
    private User currentUser;
    private final java.util.List<Uri> selectedUris = new java.util.ArrayList<>();
    private androidx.recyclerview.widget.RecyclerView selectedImagesRv;
    private android.view.View addImagesBtn;
    private SelectedImagesAdapter previewAdapter;

    private final androidx.activity.result.ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> pickImages =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(4), uris -> {
                selectedUris.clear();
                if (uris != null) selectedUris.addAll(uris);
                previewAdapter.submit(selectedUris);
                selectedImagesRv.setVisibility(selectedUris.isEmpty() ? View.GONE : View.VISIBLE);
                Toast.makeText(this, "Selected: " + selectedUris.size(), Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = getIntent().getParcelableExtra("currentUser");
        setContentView(R.layout.activity_report_form);

        typeTabs = findViewById(R.id.type_tabs);
        inputSenderName = findViewById(R.id.input_sender_name);
        inputSubject = findViewById(R.id.input_subject);
        inputText = findViewById(R.id.input_text);
        buttonSubmit = findViewById(R.id.button_submit);
        RadioButton tabManagerApp = findViewById(R.id.tab_manager_application);
        addImagesBtn = findViewById(R.id.button_add_images);
        selectedImagesRv = findViewById(R.id.selectedImagesRv);

        selectedImagesRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedImagesRv.setAdapter(new SelectedImagesAdapter(selectedUris, pos -> {
            if (pos >= 0 && pos < selectedUris.size()) {
                selectedUris.remove(pos);
                previewAdapter.notifyItemRemoved(pos);
                selectedImagesRv.setVisibility(selectedUris.isEmpty() ? View.GONE : View.VISIBLE);
            }
        }));

        buttonSubmit.setOnClickListener(v -> {
            String type = getSelectedType();
            String senderName = inputSenderName.getText().toString();
            String subject = inputSubject.getText().toString();
            String text = inputText.getText().toString();

            if (subject.isEmpty() || text.isEmpty()) {
                Toast.makeText(this, "Please fill in at least the subject and message.", Toast.LENGTH_SHORT).show();
                return;
            }

            Report report = new Report(type, senderName, subject, text);
            if ("Manager Application".equalsIgnoreCase(type)) {
                String uid = FirebaseAuth.getInstance().getUid();
                report.setApplicantUserId(uid);
                report.setType(Report.TYPE_MANAGER_APPLICATION);
            }

            CommunityRepository repo = new CommunityRepository();
            repo.getCommunityIdByName(currentUser.getCommunityName(), new CommunityRepository.FirestoreIdCallback() {
                @Override public void onSuccess(String communityId) {
                    repo.createReport(communityId, report, new CommunityRepository.FirestoreCallback() {
                        @Override public void onSuccess(String reportId) {
                            if (selectedUris.isEmpty()) { finishAfterSubmit(); return; }

                            StorageRepository storageRepo = new StorageRepository();
                            java.util.List<String> urls = new java.util.ArrayList<>();
                            uploadAllImagesSequentially(0, selectedUris, urls,
                                    (u, c) -> storageRepo.uploadReportImageCompressed(
                                            ReportFormActivity.this, communityId, reportId, u, 1280, 82, null, c),
                                    () -> repo.updateReportImages(communityId, reportId, null, urls, new CommunityRepository.FirestoreCallback() {
                                        @Override public void onSuccess(String id) { finishAfterSubmit(); }
                                        @Override public void onFailure(Exception e) { finishAfterSubmit(); }
                                    }),
                                    e -> Toast.makeText(ReportFormActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                        @Override public void onFailure(Exception e) {
                            Toast.makeText(ReportFormActivity.this, "Error saving report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
                @Override public void onFailure(Exception e) {
                    Toast.makeText(ReportFormActivity.this, "Community not found: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private interface Uploader { @Nullable com.google.firebase.storage.UploadTask go(Uri u, StorageRepository.UploadCallback cb); }
    private void uploadAllImagesSequentially(int idx, java.util.List<Uri> uris, java.util.List<String> outUrls,
                                             Uploader uploader, Runnable onDone,
                                             java.util.function.Consumer<Exception> onError) {
        if (idx >= uris.size()) { onDone.run(); return; }
        Uri u = uris.get(idx);
        uploader.go(u, new StorageRepository.UploadCallback() {
            @Override public void onSuccess(@NonNull String downloadUrl) {
                outUrls.add(downloadUrl);
                uploadAllImagesSequentially(idx+1, uris, outUrls, uploader, onDone, onError);
            }
            @Override public void onFailure(@NonNull Exception e) { onError.accept(e); }
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
