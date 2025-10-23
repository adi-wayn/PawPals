package com.example.pawpals;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        TextView policyText = findViewById(R.id.policy_text);

        String fullPolicy = "📄 PawPals Privacy Policy\n\n"
                + "Last updated: August 31, 2025\n\n"

                + "1. Information We Collect\n"
                + "- Account details (name, email, and profile picture)\n"
                + "- Dog profiles (name, breed, age, compatibility info)\n"
                + "- Optional location data for community features\n"
                + "- Usage data (interactions inside the app)\n\n"

                + "2. How We Use Your Information\n"
                + "- To provide community and social features\n"
                + "- To help match dog owners and track compatibility\n"
                + "- To report issues in parks and communities\n"
                + "- To send notifications you control in settings\n\n"

                + "3. Data Sharing\n"
                + "- We do not sell your personal data.\n"
                + "- Data may be shared only with service providers who support app functionality (e.g., hosting).\n\n"

                + "4. Data Security\n"
                + "- Your information is protected with industry-standard security.\n"
                + "- You are responsible for keeping your password secure.\n\n"

                + "5. Your Rights\n"
                + "- Access, edit, or delete your account information anytime.\n"
                + "- Adjust privacy and notification settings in the app.\n"
                + "- Request account deletion by contacting us.\n\n"

                + "6. Children’s Privacy\n"
                + "- This app is intended for users 16 and older.\n"
                + "- We do not knowingly collect data from children under 16.\n\n"

                + "7. Updates to This Policy\n"
                + "- We may update this policy from time to time.\n"
                + "- We will notify users of significant changes within the app.\n\n"

                + "📧 Contact Us\n"
                + "- adiwayn@gmail.com\n"
                + "- avigezerhalel@gmail.com\n"
                + "- tmtiktak35@gmail.com\n\n"

                + "By using PawPals, you agree to this Privacy Policy.";

        policyText.setText(fullPolicy);
    }
}
