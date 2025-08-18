package model.firebase.firestore;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.CommunityManager;
import model.User;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final FirebaseFirestore db;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // יצירת/עדכון פרופיל
    public void createUserProfile(String userId, User user, FirestoreCallback callback) {
        Map<String, Object> userMap = user.toMap();

        db.collection("users")
                .document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created for: " + userId);
                    callback.onSuccess(userId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user profile", e);
                    callback.onFailure(e);
                });
    }

    // בדיקה אם משתמש קיים
    public void checkIfUserProfileExists(String userId, FirestoreExistCallback callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> callback.onResult(snapshot.exists()))
                .addOnFailureListener(callback::onError);
    }

    // קבלת משתמש לפי ID (כולל קריאה של contactDetails ו-fieldsOfInterest)
    public void getUserById(String userId, FirestoreUserCallback callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Log.w(TAG, "User not found for ID: " + userId);
                        callback.onFailure(new Exception("User not found"));
                        return;
                    }

                    // לוג לכל התוכן שהגיע
                    Log.d(TAG, "Raw Firestore snapshot: " + snapshot.getData());

                    Boolean isManager     = snapshot.getBoolean("isManager");
                    String  name          = snapshot.getString("userName");
                    String  community     = snapshot.getString("communityName");
                    String  contact       = snapshot.getString("contactDetails");
                    String  bioOrFields   = snapshot.getString("fieldsOfInterest");

                    Log.d(TAG, String.format(
                            "Extracted fields: userName='%s', communityName='%s', isManager=%s, contact='%s', fields='%s'",
                            name, community, isManager, contact, bioOrFields
                    ));

                    User user = null;

                    if (name != null && community != null) {
                        // בנייה ידנית בטוחה עם כל השדות החדשים
                        if (Boolean.TRUE.equals(isManager)) {
                            user = new CommunityManager(name, community, contact != null ? contact : "", bioOrFields != null ? bioOrFields : "");
                            Log.d(TAG, "Created CommunityManager manually: " + user);
                        } else {
                            user = new User(name, community, contact != null ? contact : "", bioOrFields != null ? bioOrFields : "");
                            Log.d(TAG, "Created regular User manually: " + user);
                        }
                    } else {
                        // fallback ל-toObject אם חסר מידע
                        if (Boolean.TRUE.equals(isManager)) {
                            user = snapshot.toObject(CommunityManager.class);
                            Log.w(TAG, "Used toObject fallback (CommunityManager): " + user);
                        } else {
                            user = snapshot.toObject(User.class);
                            Log.w(TAG, "Used toObject fallback (User): " + user);
                        }
                    }

                    if (user != null) {
                        callback.onSuccess(user);
                    } else {
                        Log.e(TAG, "Failed to construct User object (null result)");
                        callback.onFailure(new Exception("Failed to parse user data"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore access failure", e);
                    callback.onFailure(e);
                });
    }

    // קבלת כל המשתמשים
    public void getAllUsers(FirestoreUsersListCallback callback) {
        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        users.add(doc.toObject(User.class));
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // קבלת כל המנהלים
    public void getAllManagers(FirestoreUsersListCallback callback) {
        db.collection("users")
                .whereEqualTo("isManager", true)
                .get()
                .addOnSuccessListener(query -> {
                    List<User> managers = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        managers.add(doc.toObject(CommunityManager.class));
                    }
                    callback.onSuccess(managers);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // קבלת משתמשים לפי קהילה
    public void getUsersByCommunity(String communityName, FirestoreUsersListCallback callback) {
        db.collection("users")
                .whereEqualTo("communityName", communityName)
                .get()
                .addOnSuccessListener(query -> {
                    List<User> members = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        members.add(doc.toObject(User.class));
                    }
                    callback.onSuccess(members);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // מפת userId->userName עבור קהילה
    public void getUserNamesByCommunity(String communityName, FirestoreUserNamesCallback callback) {
        db.collection("users")
                .whereEqualTo("communityName", communityName)
                .get()
                .addOnSuccessListener(query -> {
                    Map<String, String> userNames = new HashMap<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String id   = doc.getId();
                        String name = doc.getString("userName");
                        if (id != null && name != null) {
                            userNames.put(id, name);
                        }
                    }
                    callback.onSuccess(userNames);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // === ממשקי callback ===
    public interface FirestoreUserNamesCallback {
        void onSuccess(Map<String, String> userNamesById);
        void onFailure(Exception e);
    }

    public interface FirestoreCallback {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }

    public interface FirestoreExistCallback {
        void onResult(boolean exists);
        void onError(Exception e);
    }

    public interface FirestoreUserCallback {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    public interface FirestoreUsersListCallback {
        void onSuccess(List<User> users);
        void onFailure(Exception e);
    }
}