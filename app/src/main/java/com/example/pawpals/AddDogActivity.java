package com.example.pawpals;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import model.Dog;
import model.User;
import model.firebase.firestore.UserRepository;

public class AddDogActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "userId";
    private static final String TAG = "AddDogActivity";
    private EditText etName, etBreed, etAge;
    private Button btnSave;
    private final UserRepository repo = new UserRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_add_dog); // ודאי שהקובץ קיים וה-IDs תואמים

        etName  = findViewById(R.id.et_dog_name);
        etBreed = findViewById(R.id.et_dog_breed);
        etAge   = findViewById(R.id.et_dog_age);
        btnSave = findViewById(R.id.btn_save_dog);

        // בדיקת עשן ל-IDs לא נכונים ב-XML
        if (etName == null || etBreed == null || etAge == null || btnSave == null) {
            Toast.makeText(this, "בעיה ב-IDs של layout (activity_add_dog.xml)", Toast.LENGTH_LONG).show();
            Log.e(TAG, "One or more views are null. Check IDs in layout.");
            finish();
            return;
        }

        btnSave.setOnClickListener(v -> {
            hideKeyboard();
            saveDog();
        });
    }

    private void saveDog() {
        Log.d(TAG, "saveDog() clicked");
        String name   = etName.getText().toString().trim();
        String breed  = etBreed.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();

        // שם חובה
        if (TextUtils.isEmpty(name)) {
            etName.setError("שימי שם לכלב");
            etName.requestFocus();
            Log.w(TAG, "Validation failed: empty name");
            return;
        }

        // בניית האובייקט לפי המודל
        Dog dog = new Dog();
        dog.setName(name);
        dog.setBreed(TextUtils.isEmpty(breed) ? null : breed);

        if (!TextUtils.isEmpty(ageStr)) {
            try {
                dog.setAge(Integer.valueOf(ageStr)); // ודאי של- Dog.setAge יש פרמטר Integer (לא int)
            } catch (NumberFormatException e) {
                etAge.setError("גיל חייב להיות מספר");
                etAge.requestFocus();
                Log.w(TAG, "Age parse failed", e);
                return;
            }
        } else {
            dog.setAge(null);
        }

        // קבלת userId שנשלח ע"י ה-ProfileActivity
        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        Log.d(TAG, "Incoming userId: " + userId);
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "חסר userId לשמירה", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "userId is missing in Intent extras");
            return;
        }

        // מניעת דאבל-קליק
        btnSave.setEnabled(false);
        Log.d(TAG, "Calling repo.addDogToUser()");

        try {
            repo.addDogToUser(userId, dog, new UserRepository.FirestoreCallback() {
                @Override
                public void onSuccess(String documentId) {
                    Log.d(TAG, "Dog saved. docId=" + documentId);
                    Toast.makeText(AddDogActivity.this, "הכלב נשמר", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // לאפשר למסך קודם לרענן
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    btnSave.setEnabled(true);
                    String msg = (e != null && e.getMessage() != null) ? e.getMessage() : "שגיאה לא ידועה";
                    Log.e(TAG, "Failed to save dog: " + msg, e);
                    Toast.makeText(AddDogActivity.this, "נכשל: " + msg, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            // הגנה נוספת במקרה של חריגה מיידית לפני הקולבקים (למשל שגיאת שימוש ב-Repository)
            btnSave.setEnabled(true);
            String msg = (e.getMessage() != null) ? e.getMessage() : "שגיאה לא ידועה";
            Log.e(TAG, "Unexpected exception while saving dog: " + msg, e);
            Toast.makeText(this, "נכשל: " + msg, Toast.LENGTH_LONG).show();
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
