package com.example.pawpals;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import model.Dog;

public class DogDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_DOG = "extra_dog";
    public static final String EXTRA_OWNER_ID = "extra_owner_id"; // אופציונלי לשימוש עתידי

    private ImageView dogPicture;
    private TextView dogName, dogAge, dogBreed, dogNeutered, dogPersonality, dogMood, dogNotes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dog_details);

        dogPicture     = findViewById(R.id.dog_picture);
        dogName        = findViewById(R.id.dog_name);
        dogAge         = findViewById(R.id.dog_age);
        dogBreed       = findViewById(R.id.dog_breed);
        dogNeutered    = findViewById(R.id.dog_neutered);
        dogPersonality = findViewById(R.id.dog_personality);
        dogMood        = findViewById(R.id.dog_mood);
        dogNotes       = findViewById(R.id.dog_notes);

        Dog dog = getIntent().getParcelableExtra(EXTRA_DOG);
        if (dog == null) {
            Toast.makeText(this, "לא נמצאו פרטי כלב", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dogName.setText(nn(dog.getName()));

        Integer age = dog.getAge();
        dogAge.setText(age != null ? "Age: " + age : "Age: —");

        dogBreed.setText(nn(dog.getBreed()));

        Boolean neut = dog.getNeutered();
        dogNeutered.setText(neut == null ? "—" : (neut ? "כן" : "לא"));

        dogPersonality.setText(nn(dog.getPersonality()));
        dogMood.setText(nn(dog.getMood()));
        dogNotes.setText(nn(dog.getNotes()));

        String photoUrl = dog.getPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            // אם יש לך Glide בפרויקט, אפשר לפתוח ולהשתמש:
            // Glide.with(this).load(photoUrl).into(dogPicture);
        }
    }

    private String nn(String s) { return s == null ? "" : s; }
}
