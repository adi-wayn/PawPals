package com.example.pawpals;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ReportFormActivity extends AppCompatActivity {

    private RadioGroup categoryTabs;
    private EditText inputSubject, inputType1, inputType2, inputPriority, inputDescription;
    private Button buttonSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_form);

        // Connect UI elements
        categoryTabs = findViewById(R.id.category_tabs);
        inputSubject = findViewById(R.id.input_subject);
        inputType1 = findViewById(R.id.input_type_1);
        inputType2 = findViewById(R.id.input_type_2);
        inputPriority = findViewById(R.id.input_priority);
        inputDescription = findViewById(R.id.input_description);
        buttonSubmit = findViewById(R.id.button_submit);

        buttonSubmit.setOnClickListener(v -> {
            String category = getSelectedCategory();
            String subject = inputSubject.getText().toString();
            String type1 = inputType1.getText().toString();
            String type2 = inputType2.getText().toString();
            String priority = inputPriority.getText().toString();
            String description = inputDescription.getText().toString();

            if (subject.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill in at least the subject and message.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Log the data (for testing)
            Log.d("ReportForm", "Category: " + category);
            Log.d("ReportForm", "Subject: " + subject);
            Log.d("ReportForm", "Category 1: " + type1);
            Log.d("ReportForm", "Category 2: " + type2);
            Log.d("ReportForm", "Priority: " + priority);
            Log.d("ReportForm", "Message: " + description);

            Toast.makeText(this, "Your request was submitted successfully!", Toast.LENGTH_LONG).show();
            clearForm();
        });
    }

    private String getSelectedCategory() {
        int selectedId = categoryTabs.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadio = findViewById(selectedId);
            return selectedRadio.getText().toString();
        }
        return "No category selected";
    }

    private void clearForm() {
        inputSubject.setText("");
        inputType1.setText("");
        inputType2.setText("");
        inputPriority.setText("");
        inputDescription.setText("");
        categoryTabs.clearCheck();
    }
}
