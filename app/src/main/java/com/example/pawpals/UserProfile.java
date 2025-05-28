package com.example.pawpals;

public class UserProfile {
    private String name;
    private int dogCount;
    private boolean isFriend;

    public UserProfile(String name, int dogCount, boolean isFriend) {
        this.name = name;
        this.dogCount = dogCount;
        this.isFriend = isFriend;
    }

    public String getName() {
        return name;
    }

    public int getDogCount() {
        return dogCount;
    }

    public boolean isFriend() {
        return isFriend;
    }
}
