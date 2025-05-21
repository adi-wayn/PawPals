package com.example.pawpals;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private LinearLayout dogsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view);

        dogsContainer = findViewById(R.id.dogs_container);

        List<Dog> dogList = new ArrayList<>();
        dogList.add(new Dog("Rex", "German Shepherd", R.drawable.rex_image));
        dogList.add(new Dog("Luna", "Golden Retriever", R.drawable.luna_image));
        dogList.add(new Dog("Milo", "Poodle", R.drawable.milo_image));

        for (Dog dog : dogList) {
            View dogCard = LayoutInflater.from(this).inflate(R.layout.activity_dog_details, dogsContainer, false);

            ImageView dogImage = dogCard.findViewById(R.id.dog_picture);
            TextView dogName = dogCard.findViewById(R.id.dog_name);
            TextView dogBreed = dogCard.findViewById(R.id.dog_breed);

            dogImage.setImageResource(dog.getImageResId());
            dogName.setText(dog.getName());
            dogBreed.setText(dog.getBreed());

            // Optional: Set click listener
            dogCard.setOnClickListener(v -> {
                Toast.makeText(this, "Clicked " + dog.getName(), Toast.LENGTH_SHORT).show();
                // You can also open a new activity or fragment here
            });

            dogsContainer.addView(dogCard);
        }
    }
}
