package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pawpals.firebase.AuthHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword, inputConfirmPassword;
    private MaterialButton buttonRegister;
    private TextView linkLogin;

    private AuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // ודא שזה תואם לשם הקובץ שלך

        // קישור לרכיבי ה־XML
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);
        buttonRegister = findViewById(R.id.button_register);
        linkLogin = findViewById(R.id.link_login);

        authHelper = new AuthHelper();

        // לחיצה על "Sign Up"
        buttonRegister.setOnClickListener(v -> attemptRegistration());

        // מעבר למסך התחברות
        linkLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegistration() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString();
        String confirmPassword = inputConfirmPassword.getText().toString();

        // בדיקות תקינות
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Invalid email");
            inputEmail.requestFocus();
            return;
        }

        if (password.length() < 6) {
            inputPassword.setError("Password must be at least 6 characters");
            inputPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Passwords do not match");
            inputConfirmPassword.requestFocus();
            return;
        }

        // רישום בפועל
        buttonRegister.setEnabled(false); // למנוע לחיצות כפולות

        authHelper.registerUser(email, password, this, new AuthHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // כאן אפשר לשמור גם פרופיל ב-Firestore אם נרצה
                // נניח שנעבור למסך בית לאחר הרשמה
                startActivity(new Intent(RegisterActivity.this, RegistrationDetailsActivity.class));
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                buttonRegister.setEnabled(true); // לאפשר לנסות שוב
            }
        });
    }
}
