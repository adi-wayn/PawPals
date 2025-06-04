package com.example.pawpals;

import java.util.HashMap;
import java.util.Map;

public class Dog {
    private String name;
    private String breed;
    private int age;
    private boolean isSterilized;
    private String info;
    private int imageResId;

    public Dog(String name, String breed, int age, boolean isSterilized, String info, int imageResId) {
        this.name = name;
        this.breed = breed;
        this.age = age;
        this.isSterilized = isSterilized;
        this.info = info;
        this.imageResId = imageResId;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getBreed() {
        return breed;
    }

    public int getAge() {
        return age;
    }

    public boolean isSterilized() {
        return isSterilized;
    }

    public String getInfo() {
        return info;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setSterilized(boolean sterilized) {
        isSterilized = sterilized;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getImageResId() {
        return this.imageResId;
    }

    // Convert Dog to Map (for Firestore)
    public Map<String, Object> toMap() {
        Map<String, Object> dogMap = new HashMap<>();
        dogMap.put("name", name);
        dogMap.put("breed", breed);
        dogMap.put("age", age);
        dogMap.put("isSterilized", isSterilized);
        dogMap.put("info", info);
        return dogMap;
    }
}
