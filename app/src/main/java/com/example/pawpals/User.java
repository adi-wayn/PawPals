package com.example.pawpals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    public  String name;
    public  Community community;
    public ArrayList<Dog>dogs;

    public User(String name,Community community){
        this.name=name;
        this.community =community;
        this.dogs=null;
    }
    public void setName(String name){this.name=name;}
    public void setCommunity(Community community){this.community =community;}

    public String getName() {
        return this.name;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", this.name);
        if (this.community != null) {
            map.put("community", this.community.toString());
        }

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
