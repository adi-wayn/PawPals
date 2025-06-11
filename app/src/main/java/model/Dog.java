package model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class Dog implements Parcelable {
    private String name;
    private String breed;
    private int age;
    private boolean isSterilized;
    private String info;

    public Dog(String name, String breed, int age, boolean isSterilized, String info) {
        this.name = name;
        this.breed = breed;
        this.age = age;
        this.isSterilized = isSterilized;
        this.info = info;
    }

    protected Dog(Parcel in) {
        name = in.readString();
        breed = in.readString();
        age = in.readInt();
        isSterilized = in.readByte() != 0;
        info = in.readString();
    }

    public static final Creator<Dog> CREATOR = new Creator<Dog>() {
        @Override
        public Dog createFromParcel(Parcel in) {
            return new Dog(in);
        }

        @Override
        public Dog[] newArray(int size) {
            return new Dog[size];
        }
    };

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

    // Parcelable methods
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(breed);
        dest.writeInt(age);
        dest.writeByte((byte) (isSterilized ? 1 : 0));
        dest.writeString(info);
    }
}
