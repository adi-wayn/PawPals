// DeleteAccountActivity.java
package com.example.pawpals;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import model.firebase.Firestore.UserRepository;

public class DeleteAccountActivity extends AppCompatActivity {

    private Button buttonDelete, buttonCancel;
    private String currentUserId;
    private UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);

        buttonDelete = findViewById(R.id.button_delete);
        buttonCancel = findViewById(R.id.button_cancel);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        buttonCancel.setOnClickListener(v -> finish());

        buttonDelete.setOnClickListener(v -> confirmPasswordBeforeDelete());
    }

    /**
     * Confirm user's password before deleting account.
     */
    private void confirmPasswordBeforeDelete() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Password");

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
                            actuallyDeleteAccount();
                        } else {
                            Toast.makeText(DeleteAccountActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * Delete the user from Firestore and Firebase Auth.
     */
    private void actuallyDeleteAccount() {
        userRepository.deleteUser(currentUserId, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String documentId) {
                FirebaseAuth.getInstance().getCurrentUser().delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(DeleteAccountActivity.this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();

                                // Sign out and redirect to LoginActivity
                                FirebaseAuth.getInstance().signOut();
                                Intent intent = new Intent(DeleteAccountActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(DeleteAccountActivity.this, "Failed to delete account from Auth.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DeleteAccountActivity.this, "Failed to delete user from database.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
