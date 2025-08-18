package model.firebase.firestore;


import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.CommunityManager;
import model.User;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final FirebaseFirestore db;
    private CollectionReference friendsCol(String uid) {
        return db.collection("users").document(uid).collection("friends");
    }

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

    public void addFriend(String meUserId, String otherUserId, FirestoreCallback cb) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("createdAt", FieldValue.serverTimestamp());

        friendsCol(meUserId).document(otherUserId)
                .set(doc, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess(otherUserId)) // מחזירים את ה-friendId
                .addOnFailureListener(cb::onFailure);
    }

    public void removeFriend(String meUserId, String otherUserId, FirestoreCallback cb) {
        friendsCol(meUserId).document(otherUserId)
                .delete()
                .addOnSuccessListener(v -> cb.onSuccess(otherUserId))
                .addOnFailureListener(cb::onFailure);
    }

    public void isFriend(String meUserId, String otherUserId, FirestoreExistCallback cb) {
        friendsCol(meUserId).document(otherUserId)
                .get()
                .addOnSuccessListener(snap -> cb.onResult(snap.exists()))
                .addOnFailureListener(cb::onError);
    }

    public ListenerRegistration observeIsFriend(String meUserId,
                                                String otherUserId,
                                                EventListener<DocumentSnapshot> listener) {
        return friendsCol(meUserId).document(otherUserId).addSnapshotListener(listener);
    }

    public ListenerRegistration observeFriendsIds(String uid,
                                                  EventListener<QuerySnapshot> listener) {
        return friendsCol(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public ListenerRegistration observeFriendsUsers(String uid, FirestoreUsersListCallback cb) {
        return observeFriendsIds(uid, (qs, err) -> {
            if (err != null) { cb.onFailure(err); return; }
            if (qs == null || qs.isEmpty()) {
                cb.onSuccess(new ArrayList<>());
                return;
            }

            // אוספים את ה־IDs
            List<String> ids = new ArrayList<>();
            for (DocumentSnapshot d : qs) ids.add(d.getId());

            // מושכים את המשתמשים לפי IDs
            List<User> out = new ArrayList<>();
            final int total = ids.size();
            final int[] left = { total };
            if (total == 0) { cb.onSuccess(out); return; }

            for (String id : ids) {
                getUserById(id, new FirestoreUserCallback() {
                    @Override public void onSuccess(User u) {
                        if (u != null) out.add(u);
                        if (--left[0] == 0) cb.onSuccess(out);
                    }
                    @Override public void onFailure(Exception e) {
                        // מתעלמים משגיאה נקודתית; סוגרים כשסיימנו את כולם
                        if (--left[0] == 0) cb.onSuccess(out);
                    }
                });
            }
        });
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