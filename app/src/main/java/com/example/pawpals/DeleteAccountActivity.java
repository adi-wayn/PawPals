// DeleteAccountActivity.java
package com.example.pawpals;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import model.User;
import model.firebase.Firestore.UserRepository;
import com.example.pawpals.utils.CommunityManagerUtils;

public class DeleteAccountActivity extends AppCompatActivity {

    private TextView textSelectAdmin;
    private Spinner spinnerNewAdmin;
    private Button buttonDelete, buttonCancel;

    private String currentUserId;
    private String currentCommunity;
    private boolean isManager;

    private UserRepository userRepository = new UserRepository();

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
                // Start manager transfer flow before deletion
                CommunityManagerUtils.startManagerTransferFlow(
                        this,
                        currentUserId,
                        currentCommunity,
                        new CommunityManagerUtils.TransferFlowCallback() {
                            @Override
                            public void onManagerTransferred(String newAdminUid) {
                                confirmPasswordBeforeDelete(newAdminUid);
                            }

                            @Override
                            public void onCancelled() {
                                Toast.makeText(DeleteAccountActivity.this,
                                        "Manager transfer cancelled.", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            } else {
                confirmPasswordBeforeDelete(null);
            }
        });
    }

    /**
     * Load current user info to determine if manager and community.
     */
    private void loadUserInfo() {
        userRepository.getUserById(currentUserId, new UserRepository.FirestoreUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    isManager = user.isManager();
                    currentCommunity = user.getCommunityName();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DeleteAccountActivity.this,
                        "Failed to load user info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Confirm user's password before deleting account.
     * @param newAdminUid If manager, the UID of the new admin to assign before deletion. Can be null.
     */
    private void confirmPasswordBeforeDelete(String newAdminUid) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Show dialog to ask for password
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Password");

        final Spinner passwordInput = new Spinner(this); // Alternatively, use EditText
        // Or EditText for password
        final EditText input = new EditText(this);
        input.setHint("Password");
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String password = input.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(DeleteAccountActivity.this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Reauthenticate
            user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), password))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            deleteUser(newAdminUid);
                        } else {
                            Toast.makeText(DeleteAccountActivity.this,
                                    "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * Delete the user from Firebase (and optionally update new manager role)
     */
    private void deleteUser(String newAdminUid) {
        if (isManager && newAdminUid != null) {
            // Update Firestore to assign new manager
            CommunityManagerUtils.transferManager(
                    this,
                    currentCommunity,
                    newAdminUid,
                    new CommunityManagerUtils.TransferCallback() {
                        @Override
                        public void onSuccess() {
                            actuallyDeleteAccount();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(DeleteAccountActivity.this,
                                    "Failed to assign new manager. Account not deleted.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        } else {
            actuallyDeleteAccount();
        }
    }

    private void actuallyDeleteAccount() {
        userRepository.deleteUser(currentUserId, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String documentId) {
                FirebaseAuth.getInstance().getCurrentUser().delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(DeleteAccountActivity.this,
                                        "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(DeleteAccountActivity.this,
                                        "Failed to delete account from Auth.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DeleteAccountActivity.this,
                        "Failed to delete user from database.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
