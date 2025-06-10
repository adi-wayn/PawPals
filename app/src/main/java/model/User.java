package model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User  implements Parcelable {
    protected String name;

    protected Community community;
    protected ArrayList<Dog> dogs;
    protected boolean isManager;

    public User(String name, Community community) {
        this.name = name;
        this.community = community;
        this.dogs = new ArrayList<>();
        this.isManager = false;
    }

    protected User(Parcel in) {
        name = in.readString();
        isManager = in.readByte() != 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public Community getCommunity() {
        return community;
    }

    public ArrayList<Dog> getDogs() {
        return dogs;
    }

    public void setDogs(ArrayList<Dog> dogs) {
        this.dogs = dogs;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeByte((byte) (isManager ? 1 : 0));
    }
}
