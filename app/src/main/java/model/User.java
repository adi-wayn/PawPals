package model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class User implements Parcelable {
    public String userName;

    public String communityName;

    public ArrayList<Dog> dogs;

    public boolean isManager;

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

    @PropertyName("userName")
    public String getUserName() {
        return userName;
    }

    @PropertyName("userName")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @PropertyName("communityName")
    public String getCommunityName() {
        return communityName;
    }

    @PropertyName("communityName")
    public void setCommunityName(String communityName) {
        this.communityName = communityName;
    }

    @PropertyName("dogs")
    public ArrayList<Dog> getDogs() {
        return dogs;
    }

    @PropertyName("dogs")
    public void setDogs(ArrayList<Dog> dogs) {
        this.dogs = dogs;
    }

    @PropertyName("isManager")
    public boolean isManager() {
        return isManager;
    }

    @PropertyName("isManager")
    public void setIsManager(boolean manager) {
        this.isManager = manager;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", userName);
        map.put("communityName", communityName);
        map.put("isManager", isManager);

        if (dogs != null && !dogs.isEmpty()) {
            List<Map<String, Object>> dogsList = new ArrayList<>();
            for (Dog dog : dogs) {
                dogsList.add(dog.toMap());
            }
            map.put("dogs", dogsList);
        }

        return map;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(userName);
        dest.writeString(communityName);
        dest.writeTypedList(dogs);
        dest.writeByte((byte) (isManager ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
