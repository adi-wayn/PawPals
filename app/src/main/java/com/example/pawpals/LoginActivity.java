package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import model.firebase.Authentication.AuthHelper;
import model.firebase.Firestore.UserRepository;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private MaterialButton buttonLogin;
    private TextView linkRegister;

    private final AuthHelper authHelper = new AuthHelper();
    private final UserRepository userRepository = new UserRepository();

    // === שדות לגוגל ===
    private SignInButton btnGoogle;
    private GoogleSignInClient googleClient;
    private ActivityResultLauncher<Intent> googleLauncher;
    private AuthCredential pendingGoogleCred = null; // לשמירת אישור לקישור אחרי כניסה בסיסמה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputEmail   = findViewById(R.id.input_email);
        inputPassword= findViewById(R.id.input_password);
        buttonLogin  = findViewById(R.id.button_login);
        linkRegister = findViewById(R.id.link_register);

        // === Google button (ודא שקיים ב-XML עם @id/btnGoogleSignIn) ===
        btnGoogle = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogle != null) {
            btnGoogle.setSize(SignInButton.SIZE_WIDE);
        }

        // === הגדרת Google Sign-In ===
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // מתוך google-services.json
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        // === ActivityResult ל-Google ===
        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        enableButtons();
                        return;
                    }
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account == null) { enableButtons(); return; }

                        authHelper.signInWithGoogleIdToken(account.getIdToken(), this,
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
                                    public void onCollision(String email, AuthCredential cred) {
                                        // האימייל כבר קיים עם סיסמה — נשמור אישור ונבצע קישור אחרי כניסה בסיסמה
                                        pendingGoogleCred = cred;
                                        if (email != null) inputEmail.setText(email);
                                        Toast.makeText(LoginActivity.this,
                                                "האימייל כבר קיים עם סיסמה. התחבר/י בסיסמה ואז נקשר את Google.",
                                                Toast.LENGTH_LONG).show();
                                        enableButtons();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(LoginActivity.this, "Google auth failed", Toast.LENGTH_SHORT).show();
                                        enableButtons();
                                    }
                                });

                    } catch (ApiException e) {
                        Toast.makeText(LoginActivity.this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                        enableButtons();
                    }
                });

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> {
                disableButtons();
                googleLauncher.launch(googleClient.getSignInIntent());
            });
        }

        buttonLogin.setOnClickListener(v -> attemptLogin());

        linkRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

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

                // היה Collision קודם? נקשר עכשיו את Google לחשבון
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