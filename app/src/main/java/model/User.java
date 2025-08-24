package model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class User implements Parcelable {

    private String userName;
    private String communityName;

    // נכתוב את הכלבים גם ל-Firestore כשנרצה (אין @Exclude)
    private ArrayList<Dog> dogs;

    public boolean isManager;

    // שדות פרופיל
    private String contactDetails;
    private String fieldsOfInterest;

    public User() {
        this.dogs = new ArrayList<>();
        this.isManager = false;
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
        if (dogs == null) dogs = new ArrayList<>();
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
    public ArrayList<Dog> getDogs() {
        if (dogs == null) dogs = new ArrayList<>();
        return dogs;
    }
    @PropertyName("dogs")
    public void setDogs(ArrayList<Dog> dogs) { this.dogs = dogs; }
    public void addDogLocal(Dog dog) { if (dog != null) getDogs().add(dog); }

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
    /** ברירת מחדל: בלי כלבים (כמו שהיה) */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", userName);
        map.put("communityName", communityName);
        map.put("isManager", isManager);
        if (contactDetails != null && !contactDetails.isEmpty()) map.put("contactDetails", contactDetails);
        if (fieldsOfInterest != null && !fieldsOfInterest.isEmpty()) map.put("fieldsOfInterest", fieldsOfInterest);
        return map;
    }

    /** גרסה עם כלבים embedded – השתמשי בה כשאת *כן* רוצה לשמור את המערך במסמך המשתמש */
    public Map<String, Object> toMapWithDogs() {
        Map<String, Object> map = toMap();
        if (dogs != null && !dogs.isEmpty()) {
            ArrayList<Map<String, Object>> dogsList = new ArrayList<>();
            for (Dog dog : dogs) {
                if (dog != null) {
                    Map<String, Object> m = dog.toMap();
                    if (m != null && !m.isEmpty()) dogsList.add(m);
                }
            }
            if (!dogsList.isEmpty()) map.put("dogs", dogsList);
        } else {
            // אם חשוב שתמיד יהיה שדה, אפשר לשים רשימה ריקה:
            // map.put("dogs", new ArrayList<>());
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

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "userName='" + userName + '\'' +
                ", communityName='" + communityName + '\'' +
                ", dogs=" + (dogs != null ? dogs.size() : 0) +
                ", isManager=" + isManager +
                ", contactDetails='" + contactDetails + '\'' +
                ", fieldsOfInterest='" + fieldsOfInterest + '\'' +
                '}';
    }
}
