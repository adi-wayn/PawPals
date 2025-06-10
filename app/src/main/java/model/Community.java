package model;

import java.util.ArrayList;

public class Community {
    private String name;
    public ArrayList<User> Members;

    public Community(String name) {
        this.name = name;
        this.Members = new ArrayList<>();
    }

    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}
