package com.example.pawpals;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import model.Dog;
import model.firebase.Firestore.UserRepository;
import model.firebase.Storage.StorageRepository;

public class AddDogActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "userId";
    private static final String TAG = "AddDogActivity";

    private EditText etName, etBreed, etAge, etPersonality, etMood, etNotes;
    private Switch switchNeutered;
    private Button btnSave, btnUploadDogImage;
    private ImageView imgDog;
    private Uri selectedImageUri;

    private final UserRepository repo = new UserRepository();
    private StorageRepository storageRepo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_dog);

        // Bind views
        etName = findViewById(R.id.et_dog_name);
        etBreed = findViewById(R.id.et_dog_breed);
        etAge = findViewById(R.id.et_dog_age);
        etPersonality = findViewById(R.id.et_dog_personality);
        etMood = findViewById(R.id.et_dog_mood);
        etNotes = findViewById(R.id.et_dog_notes);
        switchNeutered = findViewById(R.id.switch_neutered);
        imgDog = findViewById(R.id.img_dog);
        btnSave = findViewById(R.id.btn_save_dog);
        btnUploadDogImage = findViewById(R.id.btn_upload_dog_image);

        storageRepo = new StorageRepository();

        // Image picker
        ActivityResultLauncher<String> imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        Glide.with(this).load(uri).into(imgDog);
                    }
                }
        );
        btnUploadDogImage.setOnClickListener(v -> imagePicker.launch("image/*"));

        btnSave.setOnClickListener(v -> {
            hideKeyboard();
            saveDog();
        });
    }

    private void saveDog() {
        String name = etName.getText().toString().trim();
        String breed = etBreed.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String personality = etPersonality.getText().toString().trim();
        String mood = etMood.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        Boolean neutered = switchNeutered.isChecked();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Please enter your dog's name");
            return;
        }

        Dog dog = new Dog();
        dog.setName(name);
        dog.setBreed(TextUtils.isEmpty(breed) ? null : breed);
        dog.setPersonality(TextUtils.isEmpty(personality) ? null : personality);
        dog.setMood(TextUtils.isEmpty(mood) ? null : mood);
        dog.setNotes(TextUtils.isEmpty(notes) ? null : notes);
        dog.setNeutered(neutered);

        if (!TextUtils.isEmpty(ageStr)) {
            try {
                dog.setAge(Integer.valueOf(ageStr));
            } catch (NumberFormatException e) {
                etAge.setError("Age must be a number");
                return;
            }
        }

        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        if (selectedImageUri != null) {
            storageRepo.uploadDogPhotoCompressed(
                    AddDogActivity.this, selectedImageUri, userId, name, 1080, 80,
                    new StorageRepository.UploadCallback() {
                        @Override
                        public void onSuccess(@NonNull String downloadUrl) {
                            dog.setPhotoUrl(downloadUrl);
                            actuallySaveDog(userId, dog);
                        }

                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(AddDogActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            btnSave.setEnabled(true);
                        }
                    });
        } else {
            actuallySaveDog(userId, dog);
        }
    }

    private void actuallySaveDog(String userId, Dog dog) {
        repo.addDogToUser(userId, dog, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String documentId) {
                Toast.makeText(AddDogActivity.this, "Dog saved successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                btnSave.setEnabled(true);
                Toast.makeText(AddDogActivity.this, "Failed to save dog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}