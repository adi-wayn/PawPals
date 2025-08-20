package com.example.pawpals;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pawpals.R; // ודאי שזה ה-R של האפליקציה שלך

import model.Dog;
import model.firebase.firestore.UserRepository;

public class AddDogActivity extends AppCompatActivity {

    private EditText etName, etBreed, etAge;
    private Button btnSave;
    private final UserRepository repo = new UserRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_dog); // ודאי שקיים קובץ כזה עם ה-IDs למטה

        etName = findViewById(R.id.et_dog_name);
        etBreed = findViewById(R.id.et_dog_breed);
        etAge = findViewById(R.id.et_dog_age);
        btnSave = findViewById(R.id.btn_save_dog);

        btnSave.setOnClickListener(v -> saveDog());
    }

    private void saveDog() {
        String name = etName.getText().toString().trim();
        String breed = etBreed.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("שימי שם לכלב");
            etName.requestFocus();
            return;
        }

        // בונים את האובייקט עם ה-Setters (תואם למחלקה שלך שמאפשרת nulls)
        Dog dog = new Dog();
        dog.setName(name);
        dog.setBreed(TextUtils.isEmpty(breed) ? null : breed);
        if (!TextUtils.isEmpty(ageStr)) {
            try {
                dog.setAge(Integer.valueOf(ageStr));
            } catch (NumberFormatException e) {
                etAge.setError("גיל חייב להיות מספר");
                etAge.requestFocus();
                return;
            }
        } else {
            dog.setAge(null);
        }

        // השיגי userId: או מה-Intent או מה-FirebaseAuth (בחרי את מה שמתאים אצלך)
        // 1) דרך Intent:
        String userId = getIntent().getStringExtra("userId");
        // 2) או דרך FirebaseAuth:
        // String userId = FirebaseAuth.getInstance().getCurrentUser() != null
        //         ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "חסר userId לשמירה", Toast.LENGTH_SHORT).show();
            return;
        }

        repo.addDogToUser(userId, dog, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String documentId) {
                Toast.makeText(AddDogActivity.this, "הכלב נשמר", Toast.LENGTH_SHORT).show();
                finish(); // חזרה למסך הקודם
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AddDogActivity.this, "נכשל: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
