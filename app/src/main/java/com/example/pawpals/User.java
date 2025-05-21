package com.example.pawpals;

import java.util.ArrayList;

public class User {
    public  String name;
    public  String password;
    public  Community community;
    public ArrayList<Dog >dogs;
    public User(String name,String password,Community community){
        this.name=name;
        this.password=password;
        this.community =community;
        this.dogs=null;
    }
    public void setName(String name){this.name=name;}
    public void setCommunity(Community community){this.community =community;}

    public String getName() {
        return this.name;
    }

}
