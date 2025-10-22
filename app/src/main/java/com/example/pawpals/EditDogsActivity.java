package com.example.pawpals;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import model.Dog;
import model.DogAdapter;
import model.firebase.Firestore.UserRepository;

public class EditDogsActivity extends AppCompatActivity {

    private static final String TAG = "EditDogsActivity";
    private RecyclerView recyclerView;
    private DogAdapter adapter;
    private final UserRepository repo = new UserRepository();
    private String userId;
    private List<Dog> currentDogs;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_dogs);

        recyclerView = findViewById(R.id.recycler_edit_dogs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(this, "No logged-in user found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadDogs();
    }

    private void loadDogs() {
        repo.getDogsForUser(userId, new UserRepository.FirestoreDogsListCallback() {
            @Override
            public void onSuccess(List<Dog> dogs) {
                if (dogs == null || dogs.isEmpty()) {
                    Toast.makeText(EditDogsActivity.this, "No dogs to display", Toast.LENGTH_SHORT).show();
                    return;
                }

                adapter = new DogAdapter(EditDogsActivity.this, dogs, new DogAdapter.OnDogActionListener() {
                    @Override
                    public void onDogClick(@NonNull Dog dog) {
                        Intent intent = new Intent(EditDogsActivity.this, EditSingleDogActivity.class);
                        intent.putExtra("DOG_ID", dog.getId());
                        intent.putExtra("USER_ID", userId);
                        startActivity(intent);
                    }

                    @Override
                    public void onDeleteDog(@NonNull Dog dog) {
                        showDeleteConfirmationDialog(dog);
                    }
                });

                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load dogs", e);
                Toast.makeText(EditDogsActivity.this, "Error loading dogs", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Delete confirmation dialog */
    private void showDeleteConfirmationDialog(@NonNull Dog dog) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Confirmation")
                .setMessage("Are you sure you want to delete " + dog.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteDog(dog);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    /** Actual deletion */
    private void deleteDog(@NonNull Dog dog) {
        repo.deleteDogForUser(userId, dog.getId(), new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String id) {
                Toast.makeText(EditDogsActivity.this, "Dog \"" + dog.getName() + "\" was deleted successfully", Toast.LENGTH_SHORT).show();

                // Immediate update in local list
                int index = currentDogs.indexOf(dog);
                if (index >= 0) {
                    currentDogs.remove(index);
                    adapter.notifyItemRemoved(index);
                }

                // Full refresh from Firestore after 1s (for sync)
                new Handler(Looper.getMainLooper()).postDelayed(EditDogsActivity.this::loadDogs, 1000);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditDogsActivity.this, "Failed to delete dog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}