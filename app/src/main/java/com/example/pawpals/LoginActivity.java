package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import model.firebase.AuthHelper;
import model.firebase.UserRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private MaterialButton buttonLogin;
    private TextView linkRegister;

    private final AuthHelper authHelper = new AuthHelper();
    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // ודא שזה תואם לשם הקובץ

        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        buttonLogin = findViewById(R.id.button_login);
        linkRegister = findViewById(R.id.link_register);

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

        buttonLogin.setEnabled(false);

        authHelper.loginUser(email, password, this, new AuthHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                checkUserProfile(user.getUid());
            }

            @Override
            public void onFailure(Exception e) {
                buttonLogin.setEnabled(true);
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
                            buttonLogin.setEnabled(true);
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
                buttonLogin.setEnabled(true);
            }
        });
    }

}
