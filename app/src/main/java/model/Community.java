package model;

import java.util.ArrayList;
public class Community {
    private String name;
    private CommunityManager Manager;
    public ArrayList<User> Members;

    public Community(String name) {
        this.name = name;
        this.Members = new ArrayList<>();
    }

    public Community(String name, CommunityManager manager) {
        this.name = name;
        this.Manager = manager;
        this.Members = new ArrayList<>();
    }


    public String getName() { return name; }

    public CommunityManager getManager() {
        return Manager;
    }

    public void setManager(CommunityManager manager) {
        this.Manager = manager;
    }

    @Override
    public String toString() {
        return name;
    }
}
