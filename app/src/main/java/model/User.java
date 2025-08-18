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

    private String userName;
    private String communityName;
    private ArrayList<Dog> dogs;
    public boolean isManager;

    // השדות החדשים באות קטנה
    private String contactDetails;
    private String fieldsOfInterest;

    public User() {
        // נדרש לפיירבייס
    }

    public User(String userName, String communityName, String contactDetails, String fieldsOfInterest) {
        this.userName = userName;
        this.communityName = communityName;
        this.dogs = new ArrayList<>();
        this.isManager = false;
        this.contactDetails = contactDetails;
        this.fieldsOfInterest = fieldsOfInterest;
    }

    protected User(Parcel in) {
        userName = in.readString();
        communityName = in.readString();
        dogs = in.createTypedArrayList(Dog.CREATOR);
        isManager = in.readByte() != 0;
        contactDetails = in.readString();
        fieldsOfInterest = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override public User createFromParcel(Parcel in) { return new User(in); }
        @Override public User[] newArray(int size) { return new User[size]; }
    };

    // ----- Getters/Setters -----

    @PropertyName("userName")
    public String getUserName() { return userName; }

    @PropertyName("userName")
    public void setUserName(String userName) { this.userName = userName; }

    @PropertyName("communityName")
    public String getCommunityName() { return communityName; }

    @PropertyName("communityName")
    public void setCommunityName(String communityName) { this.communityName = communityName; }

    @PropertyName("dogs")
    public ArrayList<Dog> getDogs() { return dogs; }

    @PropertyName("dogs")
    public void setDogs(ArrayList<Dog> dogs) { this.dogs = dogs; }

    @PropertyName("isManager")
    public boolean isManager() { return isManager; }

    @PropertyName("isManager")
    public void setIsManager(boolean manager) { this.isManager = manager; }

    @PropertyName("contactDetails")
    public String getContactDetails() { return contactDetails; }

    @PropertyName("contactDetails")
    public void setContactDetails(String contactDetails) { this.contactDetails = contactDetails; }

    @PropertyName("fieldsOfInterest")
    public String getFieldsOfInterest() { return fieldsOfInterest; }

    @PropertyName("fieldsOfInterest")
    public void setFieldsOfInterest(String fieldsOfInterest) { this.fieldsOfInterest = fieldsOfInterest; }

    // ----- סיריאליזציה למפה -----

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", userName);
        map.put("communityName", communityName);
        map.put("isManager", isManager);

        if (contactDetails != null && !contactDetails.isEmpty()) {
            map.put("contactDetails", contactDetails);
        }
        if (fieldsOfInterest != null && !fieldsOfInterest.isEmpty()) {
            map.put("fieldsOfInterest", fieldsOfInterest);
        }

        if (dogs != null && !dogs.isEmpty()) {
            List<Map<String, Object>> dogsList = new ArrayList<>();
            for (Dog dog : dogs) {
                dogsList.add(dog.toMap());
            }
            map.put("dogs", dogsList);
        }

        return map;
    }

    // ----- Parcelable -----

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(userName);
        dest.writeString(communityName);
        dest.writeTypedList(dogs);
        dest.writeByte((byte) (isManager ? 1 : 0));
        dest.writeString(contactDetails);
        dest.writeString(fieldsOfInterest);
    }

    @Override
    public int describeContents() { return 0; }
}
