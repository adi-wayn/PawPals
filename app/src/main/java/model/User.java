package model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class User implements Parcelable {
    protected String userName;
    protected String communityName;
    protected ArrayList<Dog> dogs;
    protected boolean isManager;

    public User() {
        // נדרש לפיירבייס
    }

    public User(String userName, String communityName) {
        this.userName = userName;
        this.communityName = communityName;
        this.dogs = new ArrayList<>();
        this.isManager = false;
    }

    protected User(Parcel in) {
        userName = in.readString();
        communityName = in.readString();
        dogs = in.createTypedArrayList(Dog.CREATOR);
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

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCommunityName() {
        return communityName;
    }

    public void setCommunityName(String communityName) {
        this.communityName = communityName;
    }

    public ArrayList<Dog> getDogs() {
        return dogs;
    }

    public void setDogs(ArrayList<Dog> dogs) {
        this.dogs = dogs;
    }

    public boolean isManager() {
        return isManager;
    }

    public void setIsManager(boolean manager) {
        this.isManager = manager;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", this.userName);
        map.put("communityName", this.communityName);
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
        dest.writeString(userName);
        dest.writeString(communityName);
        dest.writeTypedList(dogs);
        dest.writeByte((byte) (isManager ? 1 : 0));
    }
}
