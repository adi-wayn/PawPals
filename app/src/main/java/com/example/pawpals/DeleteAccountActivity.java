package com.example.pawpals;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import model.User;
import model.firebase.Firestore.CommunityRepository;
import model.firebase.Firestore.UserRepository;

public class DeleteAccountActivity extends AppCompatActivity {

    private TextView textSelectAdmin;
    private Spinner spinnerNewAdmin;
    private Button buttonDelete, buttonCancel;

    private String currentUserId;
    private String currentCommunity;
    private boolean isManager;
    private List<User> communityMembers = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);

        textSelectAdmin = findViewById(R.id.text_select_admin);
        spinnerNewAdmin = findViewById(R.id.spinner_new_admin);
        buttonDelete = findViewById(R.id.button_delete);
        buttonCancel = findViewById(R.id.button_cancel);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadUserInfo();

        buttonCancel.setOnClickListener(v -> finish());

        buttonDelete.setOnClickListener(v -> {
            if (isManager) {
                String newAdminId = getSelectedAdminId();
                if (newAdminId == null) {
                    Toast.makeText(this, "Please select a new admin before deleting.", Toast.LENGTH_SHORT).show();
                    return;
                }
                confirmPasswordBeforeDelete(newAdminId);
            } else {
                confirmPasswordBeforeDelete(null);
            }
        });
    }

    // 🔹 Load user info and show spinner if user is manager
    private void loadUserInfo() {
        UserRepository userRepo = new UserRepository();
        userRepo.getUserById(currentUserId, new UserRepository.FirestoreUserCallback() {
            @Override
            public void onSuccess(User user) {
                isManager = user.isManager();
                currentCommunity = user.getCommunityName();

                if (isManager) {
                    textSelectAdmin.setVisibility(TextView.VISIBLE);
                    spinnerNewAdmin.setVisibility(Spinner.VISIBLE);
                    loadCommunityMembers(userRepo);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DeleteAccountActivity.this, "Failed to load user info.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 Load members for admin transfer spinner
    private void loadCommunityMembers(UserRepository userRepo) {
        userRepo.getUsersByCommunity(currentCommunity, new UserRepository.FirestoreUsersListCallback() {
            @Override
            public void onSuccess(List<User> members) {
                List<String> memberNames = new ArrayList<>();

                // Exclude current manager from the list
                for (User u : members) {
                    if (!u.getUserName().equals(currentUserId)) {
                        //figure out how to use uid, because right now manager cant change
                        memberNames.add(u.getUserName());
                    }
                }

                if (memberNames.isEmpty()) {
                    Toast.makeText(DeleteAccountActivity.this,
                            "No other members available to assign as admin.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        DeleteAccountActivity.this,
                        android.R.layout.simple_spinner_item,
                        memberNames
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerNewAdmin.setAdapter(adapter);

                communityMembers = members;
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DeleteAccountActivity.this, "Failed to load community members", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 Get selected admin's UID, not name
    private String getSelectedAdminId() {
        int pos = spinnerNewAdmin.getSelectedItemPosition();
        if (pos == Spinner.INVALID_POSITION) return null;

        String selectedName = (String) spinnerNewAdmin.getSelectedItem();
        for (User u : communityMembers) {
            if (u.getUid().equals(selectedName)) {
                //same...find out hoe to use uid
                return u.getUid();
            }
        }
        return null;
    }

    // 🔹 Ask for password before deleting
    private void confirmPasswordBeforeDelete(String newAdminId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Password");

        final EditText input = new EditText(this);
        input.setHint("Enter your password");
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String password = input.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Password is required.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isManager && newAdminId != null) {
                transferAdminAndDelete(newAdminId, password);
            } else {
                deleteUser(password);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // 🔹 Transfer admin role, then delete
    private void transferAdminAndDelete(String newAdminId, String password) {
        UserRepository userRepo = new UserRepository();
        CommunityRepository communityRepo = new CommunityRepository();

        userRepo.updateUserProfile(newAdminId, Map.of("isManager", true), new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String id) {
                communityRepo.updateCommunityManager(currentCommunity, newAdminId, new CommunityRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess(String updatedId) {
                        deleteUser(password);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(DeleteAccountActivity.this, "Failed to update community manager.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DeleteAccountActivity.this, "Failed to assign new admin.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 Reauthenticate and delete from Firebase + Firestore
    private void deleteUser(String password) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = auth.getCurrentUser().getEmail();
        if (email == null) {
            Toast.makeText(this, "No email associated with this account.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.getCurrentUser().reauthenticate(
                EmailAuthProvider.getCredential(email, password)
        ).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                auth.getCurrentUser().delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        new UserRepository().deleteUser(currentUserId, new UserRepository.FirestoreCallback() {
                            @Override
                            public void onSuccess(String id) {
                                Toast.makeText(DeleteAccountActivity.this, "Account deleted.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(DeleteAccountActivity.this, LoginActivity.class));
                                finish();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(DeleteAccountActivity.this, "Failed to delete user from database.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(DeleteAccountActivity.this, "Failed to delete account from Firebase Auth.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(DeleteAccountActivity.this, "Reauthentication failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
