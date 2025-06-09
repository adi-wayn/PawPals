package com.example.pawpals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    protected String name;
    protected String password;
    protected Community community;
    protected ArrayList<Dog> dogs;
    protected boolean isManager;

    public User(String name, String password, Community community) {
        this.name = name;
        this.password = password;
        this.community = community;
        this.dogs = new ArrayList<>();
        this.isManager = false;
    }

    public void setName(String name){this.name=name;}
    public void setCommunity(Community community){this.community =community;}

    public String getName() {
        return this.name;
    }
    public void setManager(boolean manager) {
        this.isManager = manager;
    }

    public boolean isManager() {
        return isManager;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", this.name);
        map.put("password", this.password);
        map.put("community", this.community.toString());
        map.put("isManager", this.isManager);

        if (this.dogs != null && !this.dogs.isEmpty()) {
            List<Map<String, Object>> dogsList = new ArrayList<>();
            for (Dog dog : dogs) {
                dogsList.add(dog.toMap());
            }
            map.put("dogs", dogsList);
        }

        return map;
    }
}
