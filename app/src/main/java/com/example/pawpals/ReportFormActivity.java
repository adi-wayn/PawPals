package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import model.Report;
import model.User;
import model.firebase.firestore.CommunityRepository;

public class ReportFormActivity extends AppCompatActivity {

    private RadioGroup typeTabs;
    private EditText inputSenderName, inputSubject, inputText;
    private Button buttonSubmit;
    private User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = getIntent().getParcelableExtra("currentUser");
        setContentView(R.layout.activity_report_form);

        // Connect UI elements
        typeTabs = findViewById(R.id.type_tabs);
        inputSenderName = findViewById(R.id.input_sender_name);
        inputSubject = findViewById(R.id.input_subject);
        inputText = findViewById(R.id.input_text);
        buttonSubmit = findViewById(R.id.button_submit);

        buttonSubmit.setOnClickListener(v -> {
            String type = getSelectedType();
            String senderName = inputSenderName.getText().toString();
            String subject = inputSubject.getText().toString();
            String text = inputText.getText().toString();


            if (subject.isEmpty() || text.isEmpty()) {
                Toast.makeText(this, "Please fill in at least the subject and message.", Toast.LENGTH_SHORT).show();
                return;
            }

            // צור אובייקט Report
            Report report = new Report(type, senderName, subject, text);

            // שמור במסד הנתונים תחת הקהילה
            CommunityRepository repo = new CommunityRepository();
            repo.getCommunityIdByName(currentUser.getCommunityName(), new CommunityRepository.FirestoreIdCallback() {
                @Override
                public void onSuccess(String communityId) {
                    repo.createReport(communityId, report, new CommunityRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess(String documentId) {
                            Toast.makeText(ReportFormActivity.this, "Report submitted!", Toast.LENGTH_SHORT).show();
                            // מעבר לעמוד הקהילה
                            Intent intent = new Intent(ReportFormActivity.this, CommunityActivity.class);
                            intent.putExtra("currentUser", currentUser);
                            startActivity(intent);
                            finish();
                            finish(); // חזור למסך הקודם
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(ReportFormActivity.this, "Error saving report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(ReportFormActivity.this, "Community not found: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            // Log the data (for testing)
            Log.d("ReportForm", "type: " + type);
            Log.d("ReportForm", "sender name: " + senderName);
            Log.d("ReportForm", "subject: " + subject);
            Log.d("ReportForm", "text: " + text);

            clearForm();
        });
    }

    private String getSelectedType() {
        int selectedId = typeTabs.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadio = findViewById(selectedId);
            return selectedRadio.getText().toString();
        }
        return "No type selected";
    }

    private void clearForm() {
        typeTabs.clearCheck();
        inputSenderName.setText("");
        inputSubject.setText("");
        inputText.setText("");
    }
}
