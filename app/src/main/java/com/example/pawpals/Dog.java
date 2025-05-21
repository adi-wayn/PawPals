package com.example.pawpals;

public class Dog {
    private String name;
    private String breed;
    private int imageResId; // e.g., R.drawable.ic_dog_placeholder

    public Dog(String name, String breed, int imageResId) {
        this.name = name;
        this.breed = breed;
        this.imageResId = imageResId;
    }

    public String getName() { return name; }
    public String getBreed() { return breed; }
    public int getImageResId() { return imageResId; }
}
