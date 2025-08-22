package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// Credential Manager + Google ID
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException;
import androidx.credentials.exceptions.GetCredentialUnsupportedException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.material.button.MaterialButton;
import com.google.android.gms.common.SignInButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import model.firebase.Authentication.AuthHelper;
import model.firebase.Firestore.UserRepository;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private MaterialButton buttonLogin;
    private TextView linkRegister;

    private final AuthHelper authHelper = new AuthHelper();
    private final UserRepository userRepository = new UserRepository();

    // === Google (UI + Credential Manager) ===
    private SignInButton btnGoogle;
    private CredentialManager credentialManager;

    // Holds a pending Google credential to link after password sign-in
    private AuthCredential pendingGoogleCred = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputEmail   = findViewById(R.id.input_email);
        inputPassword= findViewById(R.id.input_password);
        buttonLogin  = findViewById(R.id.button_login);
        linkRegister = findViewById(R.id.link_register);

        // Google button (UI only)
        btnGoogle = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogle != null) btnGoogle.setSize(SignInButton.SIZE_WIDE);

        // Credential Manager
        credentialManager = CredentialManager.create(this);

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> startGoogleSignIn());
        }

        buttonLogin.setOnClickListener(v -> attemptLogin());

        linkRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    // ==== Google Sign-In with Credential Manager ====
    private void startGoogleSignIn() {
        disableButtons();

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId(getString(R.string.default_web_client_id)) // from google-services.json
                .setFilterByAuthorizedAccounts(false) // also show accounts that haven't previously authorized
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                /* cancellationSignal */ null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        Credential cred = response.getCredential();
                        if (cred instanceof CustomCredential &&
                                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                        .equals(((CustomCredential) cred).getType())) {
                            try {
                                GoogleIdTokenCredential googleCred =
                                        GoogleIdTokenCredential.createFrom(((CustomCredential) cred).getData());
                                String idToken = googleCred.getIdToken();

                                authHelper.signInWithGoogleIdToken(idToken, LoginActivity.this,
                                        new AuthHelper.GoogleAuthCallback() {
                                            @Override
                                            public void onSuccess(FirebaseUser user, boolean isNewUser) {
                                                if (isNewUser) {
                                                    startActivity(new Intent(LoginActivity.this, RegistrationDetailsActivity.class));
                                                    finish();
                                                } else if (user != null) {
                                                    checkUserProfile(user.getUid());
                                                } else {
                                                    enableButtons();
                                                }
                                            }

                                            @Override
                                            public void onCollision(String email, AuthCredential pending) {
                                                // Email already exists with password — link Google after password sign-in
                                                pendingGoogleCred = pending;
                                                if (email != null) inputEmail.setText(email);
                                                Toast.makeText(LoginActivity.this,
                                                        "This email already exists with a password. Sign in with your password and we'll link Google.",
                                                        Toast.LENGTH_LONG).show();
                                                enableButtons();
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                Toast.makeText(LoginActivity.this, "Google auth failed", Toast.LENGTH_SHORT).show();
                                                enableButtons();
                                            }
                                        });

                            } catch (Exception e) {
                                Toast.makeText(LoginActivity.this, "Failed to parse Google credential", Toast.LENGTH_SHORT).show();
                                enableButtons();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "No Google credential returned", Toast.LENGTH_SHORT).show();
                            enableButtons();
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        String msg = "Google Sign-In failed";
                        if (e instanceof NoCredentialException) {
                            msg = "No available Google provider or no accounts on device";
                        } else if (e instanceof GetCredentialCancellationException) {
                            msg = "Sign-in cancelled";
                        } else if (e instanceof GetCredentialProviderConfigurationException) {
                            msg = "Provider misconfigured / out-of-date";
                        } else if (e instanceof GetCredentialUnsupportedException) {
                            msg = "Credential type unsupported on this device";
                        }
                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                        enableButtons();
                    }
                }
        );
    }

    // ==== Email/Password login + link if there was a prior collision ====
    private void attemptLogin() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Invalid email");
            inputEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            inputPassword.setError("Password too short");
            inputPassword.requestFocus();
            return;
        }

        disableButtons();

        authHelper.loginUser(email, password, this, new AuthHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (user == null) { enableButtons(); return; }

                // If we had a pending Google credential from a collision — link it now
                if (pendingGoogleCred != null) {
                    authHelper.linkWithCredential(pendingGoogleCred, LoginActivity.this, new AuthHelper.AuthCallback() {
                        @Override public void onSuccess(FirebaseUser u) {
                            pendingGoogleCred = null;
                            checkUserProfile(u.getUid());
                        }
                        @Override public void onFailure(Exception e) {
                            pendingGoogleCred = null;
                            Toast.makeText(LoginActivity.this, "Linking Google failed", Toast.LENGTH_SHORT).show();
                            checkUserProfile(user.getUid());
                        }
                    });
                } else {
                    checkUserProfile(user.getUid());
                }
            }

            @Override
            public void onFailure(Exception e) {
                enableButtons();
                Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserProfile(String userId) {
        userRepository.checkIfUserProfileExists(userId, new UserRepository.FirestoreExistCallback(){
            @Override
            public void onResult(boolean exists) {
                if (exists) {
                    userRepository.getUserById(userId, new UserRepository.FirestoreUserCallback() {
                        @Override
                        public void onSuccess(model.User user) {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("currentUser", user);
                            startActivity(intent);
                            finish();
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(LoginActivity.this, "Error retrieving user data", Toast.LENGTH_SHORT).show();
                            enableButtons();
                        }
                    });
                } else {
                    startActivity(new Intent(LoginActivity.this, RegistrationDetailsActivity.class));
                    finish();
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(LoginActivity.this, "Error checking profile", Toast.LENGTH_SHORT).show();
                enableButtons();
            }
        });
    }

    private void disableButtons() {
        buttonLogin.setEnabled(false);
        if (btnGoogle != null) btnGoogle.setEnabled(false);
    }
    private void enableButtons() {
        buttonLogin.setEnabled(true);
        if (btnGoogle != null) btnGoogle.setEnabled(true);
    }
}