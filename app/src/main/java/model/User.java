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

    // מזהה המסמך/משתמש (נוח שיהיה גם בשדה במסמך; אם אין לך צורך – אפשר להשמיט מה-toMap)
    private String uid;

    private String userName;
    private String communityName;

    // רשימת מזהי חברים (UIDs)
    private ArrayList<String> friendsIds;

    // נכתוב את הכלבים גם ל-Firestore כשנרצה (אין @Exclude)
    private ArrayList<Dog> dogs;

    public boolean isManager;

    // שדות פרופיל
    private String contactDetails;
    private String fieldsOfInterest;

    // ====== בנאים ======
    public User() {
        this.dogs = new ArrayList<>();
        this.friendsIds = new ArrayList<>();
        this.isManager = false;
    }

    public User(String userName, String communityName, String contactDetails, String fieldsOfInterest) {
        this.userName = userName;
        this.communityName = communityName;
        this.dogs = new ArrayList<>();
        this.friendsIds = new ArrayList<>();
        this.isManager = false;
        this.contactDetails = contactDetails;
        this.fieldsOfInterest = fieldsOfInterest;
    }

    protected User(Parcel in) {
        uid = in.readString();
        userName = in.readString();
        communityName = in.readString();

        // friendsIds
        int fSize = in.readInt();
        if (fSize >= 0) {
            friendsIds = new ArrayList<>(fSize);
            for (int i = 0; i < fSize; i++) friendsIds.add(in.readString());
        } else {
            friendsIds = new ArrayList<>();
        }

        // dogs
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

    // ====== Getters / Setters ======

    @PropertyName("uid")
    public String getUid() { return uid; }
    @PropertyName("uid")
    public void setUid(String uid) { this.uid = uid; }

    @PropertyName("userName")
    public String getUserName() { return userName; }
    @PropertyName("userName")
    public void setUserName(String userName) { this.userName = userName; }

    @PropertyName("communityName")
    public String getCommunityName() { return communityName; }
    @PropertyName("communityName")
    public void setCommunityName(String communityName) { this.communityName = communityName; }

    @PropertyName("friendsIds")
    public ArrayList<String> getFriendsIds() {
        if (friendsIds == null) friendsIds = new ArrayList<>();
        return friendsIds;
    }
    @PropertyName("friendsIds")
    public void setFriendsIds(ArrayList<String> friendsIds) { this.friendsIds = friendsIds; }

    public void addFriend(String friendUid) {
        if (friendUid == null || friendUid.isEmpty()) return;
        ArrayList<String> list = getFriendsIds();
        if (!list.contains(friendUid)) list.add(friendUid);
    }

    public void removeFriend(String friendUid) {
        if (friendUid == null) return;
        ArrayList<String> list = getFriendsIds();
        list.remove(friendUid);
    }

    public boolean hasFriend(String friendUid) {
        return friendUid != null && getFriendsIds().contains(friendUid);
    }

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

    // ====== סיריאליזציה למפה ======
    /** ברירת מחדל: בלי כלבים; כן נוסיף friendsIds ו־uid אם קיימים */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (uid != null && !uid.isEmpty()) map.put("uid", uid);
        map.put("userName", userName);
        map.put("communityName", communityName);
        map.put("isManager", isManager);

        // רשימת חברים
        if (friendsIds != null && !friendsIds.isEmpty()) {
            map.put("friendsIds", new ArrayList<>(friendsIds));
        } else {
            // אם חשוב שתמיד יהיה שדה:
            // map.put("friendsIds", new ArrayList<String>());
        }

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
            // אם חשוב שתמיד יהיה שדה:
            // map.put("dogs", new ArrayList<>());
        }
        return map;
    }

    // ====== Parcelable ======
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(userName);
        dest.writeString(communityName);

        // friendsIds
        ArrayList<String> f = getFriendsIds();
        dest.writeInt(f != null ? f.size() : -1);
        if (f != null) {
            for (String s : f) dest.writeString(s);
        }

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
                "uid='" + uid + '\'' +
                ", userName='" + userName + '\'' +
                ", communityName='" + communityName + '\'' +
                ", friendsIds=" + (friendsIds != null ? friendsIds.size() : 0) +
                ", dogs=" + (dogs != null ? dogs.size() : 0) +
                ", isManager=" + isManager +
                ", contactDetails='" + contactDetails + '\'' +
                ", fieldsOfInterest='" + fieldsOfInterest + '\'' +
                '}';
    }
}
