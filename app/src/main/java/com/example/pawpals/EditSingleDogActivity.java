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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.List;

import model.Dog;
import model.firebase.Firestore.UserRepository;
import model.firebase.Storage.StorageRepository;

public class EditSingleDogActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "USER_ID";
    public static final String EXTRA_DOG_ID = "DOG_ID";

    private static final String TAG = "EditSingleDogActivity";

    private EditText etName, etBreed, etAge, etPersonality, etMood, etNotes;
    private Switch switchNeutered;
    private Button btnSave, btnUploadDogImage, btnDelete;
    private ImageView imgDog;
    private Uri selectedImageUri;

    private final UserRepository repo = new UserRepository();
    private StorageRepository storageRepo;

    private String userId, dogId;
    private Dog currentDog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_single_dog); // נשתמש באותו layout!

        // === Bind views ===
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
        btnDelete = findViewById(R.id.btn_delete_dog);

        storageRepo = new StorageRepository();

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        dogId = getIntent().getStringExtra(EXTRA_DOG_ID);

        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(dogId)) {
            Toast.makeText(this, "Missing dog/user ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // === טען נתוני הכלב לעריכה ===
        loadDog();

        // === בורר תמונה ===
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

        // === שמירה ===
        btnSave.setText("Save Changes");
        btnSave.setOnClickListener(v -> {
            hideKeyboard();
            saveChanges();
        });

        // === מחיקה ===
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    /** שליפת נתוני הכלב */
    private void loadDog() {
        repo.getDogsForUser(userId, new UserRepository.FirestoreDogsListCallback() {
            @Override
            public void onSuccess(List<Dog> dogs) {
                for (Dog d : dogs) {
                    if (dogId.equals(d.getId())) {
                        currentDog = d;
                        fillFields(d);
                        return;
                    }
                }
                Toast.makeText(EditSingleDogActivity.this, "Dog not found", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditSingleDogActivity.this, "Failed to load dog", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /** הצגת הערכים בשדות */
    private void fillFields(@NonNull Dog dog) {
        etName.setText(dog.getName());
        etBreed.setText(dog.getBreed());
        etAge.setText(dog.getAge() != null ? String.valueOf(dog.getAge()) : "");
        etPersonality.setText(dog.getPersonality());
        etMood.setText(dog.getMood());
        etNotes.setText(dog.getNotes());
        switchNeutered.setChecked(Boolean.TRUE.equals(dog.getNeutered()));

        if (dog.getPhotoUrl() != null && !dog.getPhotoUrl().isEmpty()) {
            Glide.with(this).load(dog.getPhotoUrl()).placeholder(R.drawable.rex_image).into(imgDog);
        }
    }

    /** שמירת שינויים */
    private void saveChanges() {
        String name = etName.getText().toString().trim();
        String breed = etBreed.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String personality = etPersonality.getText().toString().trim();
        String mood = etMood.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        boolean neutered = switchNeutered.isChecked();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name required");
            return;
        }

        Dog updatedDog = new Dog();
        updatedDog.setName(name);
        updatedDog.setBreed(breed);
        updatedDog.setPersonality(personality);
        updatedDog.setMood(mood);
        updatedDog.setNotes(notes);
        updatedDog.setNeutered(neutered);

        if (!TextUtils.isEmpty(ageStr)) {
            try {
                updatedDog.setAge(Integer.parseInt(ageStr));
            } catch (NumberFormatException e) {
                etAge.setError("Age must be a number");
                return;
            }
        }

        btnSave.setEnabled(false);

        if (selectedImageUri != null) {
            // העלאת תמונה חדשה
            storageRepo.uploadDogPhotoCompressed(
                    this, selectedImageUri, userId, name, 1080, 80,
                    new StorageRepository.UploadCallback() {
                        @Override
                        public void onSuccess(@NonNull String downloadUrl) {
                            updatedDog.setPhotoUrl(downloadUrl);
                            updateDog(updatedDog);
                        }

                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(EditSingleDogActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                            btnSave.setEnabled(true);
                        }
                    });
        } else {
            // שמירה ללא שינוי תמונה
            updatedDog.setPhotoUrl(currentDog != null ? currentDog.getPhotoUrl() : null);
            updateDog(updatedDog);
        }
    }

    private void updateDog(Dog dog) {
        repo.updateDogForUser(userId, dogId, dog, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String documentId) {
                Toast.makeText(EditSingleDogActivity.this, "Dog updated successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditSingleDogActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Dog")
                .setMessage("Are you sure you want to delete this dog?")
                .setPositiveButton("Delete", (d, w) -> deleteDog())
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    private void deleteDog() {
        repo.deleteDogForUser(userId, dogId, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String documentId) {
                Toast.makeText(EditSingleDogActivity.this, "Dog deleted", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditSingleDogActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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