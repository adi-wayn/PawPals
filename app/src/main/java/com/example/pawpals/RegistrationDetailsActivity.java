package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pawpals.firebase.FirestoreHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegistrationDetailsActivity extends AppCompatActivity {

    private EditText inputName, inputCommunity;
    private CheckBox checkboxCreateCommunity;
    private MaterialButton buttonContinue;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // המשתמש הנוכחי

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_details);

        inputName = findViewById(R.id.input_name);
        inputCommunity = findViewById(R.id.input_community);
        checkboxCreateCommunity = findViewById(R.id.checkbox_create_community);
        buttonContinue = findViewById(R.id.button_continue);

        buttonContinue.setOnClickListener(v -> handleRegistrationDetails());
    }

    private void handleRegistrationDetails() {
        String name = inputName.getText().toString().trim();
        String communityName = inputCommunity.getText().toString().trim();
        boolean wantsToCreate = checkboxCreateCommunity.isChecked();

        if (name.isEmpty() || communityName.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // בדיקה אם קהילה כבר קיימת
        db.collection("communities")
                .document(communityName)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean communityExists = snapshot.exists();

                    if (communityExists && wantsToCreate) {
                        Toast.makeText(this, "Community already exists!", Toast.LENGTH_SHORT).show();
                    } else if (!communityExists && !wantsToCreate) {
                        Toast.makeText(this, "Community doesn't exist yet. Please check 'create' to create it.", Toast.LENGTH_SHORT).show();
                    } else {
                        saveUserAndCommunity(name, communityName, wantsToCreate);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking community", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserAndCommunity(String name, String communityName, boolean isManager) {
        FirestoreHelper firestoreHelper = new FirestoreHelper();

        if (isManager) {
            firestoreHelper.createCommunity(communityName, userId, new FirestoreHelper.FirestoreCallback() {
                @Override
                public void onSuccess(String id) {
                    saveUser(name, communityName, isManager);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(RegistrationDetailsActivity.this, "Failed to create community", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            saveUser(name, communityName, false);
        }
    }

    private void saveUser(String name, String communityName, boolean isManager) {
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        Community community = new Community(communityName);
        User user = new User(name, null, community);
        user.setManager(isManager);

        firestoreHelper.createUserProfile(userId, user, new FirestoreHelper.FirestoreCallback() {
            @Override
            public void onSuccess(String documentId) {
                Toast.makeText(RegistrationDetailsActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegistrationDetailsActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(RegistrationDetailsActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
