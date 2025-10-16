package model.firebase.Firestore;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
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
import java.util.concurrent.atomic.AtomicInteger;

import model.CommunityManager;
import model.User;
import model.Dog;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final int WHERE_IN_MAX = 10;

    private final FirebaseFirestore db;
    private CollectionReference friendsCol(String uid) {
        return db.collection("users").document(uid).collection("friends");
    }

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }
    public UserRepository(FirebaseFirestore db) {
        this.db = db;
    }

    // ===== Utils =====

    private static <T> List<List<T>> chunk(List<T> src, int size) {
        List<List<T>> out = new ArrayList<>();
        if (src == null || src.isEmpty() || size <= 0) return out;
        for (int i = 0; i < src.size(); i += size) {
            out.add(new ArrayList<>(src.subList(i, Math.min(i + size, src.size()))));
        }
        return out;
    }

    // ===== Users =====

    // יצירת/עדכון פרופיל
    public void createUserProfile(String userId, User user, FirestoreCallback callback) {
        Map<String, Object> userMap = user.toMap();
        // נשמור גם את ה-uid במסמך (נוח לצריכה)
        userMap.put("uid", userId);

        db.collection("users")
                .document(userId)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created/updated for: " + userId);
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

    // קבלת משתמש לפי ID (כולל contactDetails + fieldsOfInterest + friendsIds + uid)
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
                    Log.d(TAG, "Raw Firestore snapshot: " + snapshot.getData());

                    Boolean isManager   = snapshot.getBoolean("isManager");
                    String  name        = snapshot.getString("userName");
                    String  community   = snapshot.getString("communityName");
                    String  contact     = snapshot.getString("contactDetails");
                    String  fields      = snapshot.getString("fieldsOfInterest");

                    User user;
                    if (name != null && community != null) {
                        if (Boolean.TRUE.equals(isManager)) {
                            user = new CommunityManager(
                                    name,
                                    community,
                                    contact != null ? contact : "",
                                    fields  != null ? fields  : ""
                            );
                        } else {
                            user = new User(
                                    name,
                                    community,
                                    contact != null ? contact : "",
                                    fields  != null ? fields  : ""
                            );
                        }
                    } else {
                        if (Boolean.TRUE.equals(isManager)) {
                            user = snapshot.toObject(CommunityManager.class);
                        } else {
                            user = snapshot.toObject(User.class);
                        }
                    }

                    if (user == null) {
                        callback.onFailure(new Exception("Failed to parse user data"));
                        return;
                    }

                    // קבע UID באובייקט (גם אם שמור בשדה)
                    user.setUid(snapshot.getId());

                    // פריסת friendsIds בבטחה (יכול להיות null / לא קיים / לא רשימה)
                    Object f = snapshot.get("friendsIds");
                    if (f instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> lst = (List<String>) f;
                        user.setFriendsIds(new ArrayList<>(lst));
                    } else {
                        user.setFriendsIds(new ArrayList<>());
                    }

                    callback.onSuccess(user);
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
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            u.setUid(doc.getId());
                            // friendsIds לא ייקבע אוטומטית ע"י toObject אם חסר; נשאיר ככה
                            users.add(u);
                        }
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
                        User u = doc.toObject(CommunityManager.class);
                        if (u != null) {
                            u.setUid(doc.getId());
                            managers.add(u);
                        }
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
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            u.setUid(doc.getId());
                            members.add(u);
                        }
                    }
                    callback.onSuccess(members);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // קבלת משתמשים לפי קהילה + מזהים (מהגרסת dev)
    public void getUsersByCommunityWithIds(String communityName, FirestoreUsersWithIdsCallback callback) {
        db.collection("users")
                .whereEqualTo("communityName", communityName)
                .get()
                .addOnSuccessListener(query -> {
                    List<Pair<String, User>> rows = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            u.setUid(doc.getId());
                            rows.add(new Pair<>(doc.getId(), u)); // userId + האובייקט
                        }
                    }
                    callback.onSuccess(rows);
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

    // === חדש: קבלת משתמשים לפי רשימת IDs (עם chunking עד 10 בכל whereIn)
    public void getUsersByIds(List<String> ids, FirestoreUsersListCallback cb) {
        if (ids == null || ids.isEmpty()) { cb.onSuccess(new ArrayList<>()); return; }

        List<List<String>> parts = chunk(ids, WHERE_IN_MAX);
        List<User> acc = new ArrayList<>();
        AtomicInteger pending = new AtomicInteger(parts.size());

        for (List<String> part : parts) {
            db.collection("users")
                    .whereIn(FieldPath.documentId(), part)
                    .get()
                    .addOnSuccessListener(snap -> {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            User u = doc.toObject(User.class);
                            if (u != null) {
                                u.setUid(doc.getId());
                                acc.add(u);
                            }
                        }
                        if (pending.decrementAndGet() == 0) cb.onSuccess(acc);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "getUsersByIds part failed", e);
                        if (pending.decrementAndGet() == 0) cb.onSuccess(acc); // אפשר גם cb.onFailure(e) אם רוצים להכשיל
                    });
        }
    }

    // ===== Dogs (תת־אוסף תחת המשתמש) =====

    /** יצירת כלב חדש תחת המשתמש (users/{userId}/dogs/{autoId}) */
    public void addDogToUser(String userId, Dog dog, FirestoreCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("userId is empty"));
            return;
        }
        db.collection("users")
                .document(userId)
                .collection("dogs")
                .add(dog.toMap())
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    /** שליפת כל הכלבים של המשתמש */
    public void getDogsForUser(String userId, FirestoreDogsListCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("userId is empty"));
            return;
        }
        db.collection("users")
                .document(userId)
                .collection("dogs")
                .get()
                .addOnSuccessListener(qs -> {
                    List<Dog> list = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Map<String, Object> m = d.getData();
                        Dog dog = new Dog();
                        if (m != null) {
                            dog.setName((String) m.get("name"));
                            dog.setBreed((String) m.get("breed"));
                            Object age = m.get("age");
                            if (age instanceof Number) dog.setAge(((Number) age).intValue());
                            Object neut = m.get("neutered");
                            if (neut instanceof Boolean) dog.setNeutered((Boolean) neut);
                            dog.setPersonality((String) m.get("personality"));
                            dog.setMood((String) m.get("mood"));
                            dog.setNotes((String) m.get("notes"));
                            dog.setPhotoUrl((String) m.get("photoUrl"));
                        }
                        list.add(dog);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /** עדכון כלב קיים לפי dogId */
    public void updateDogForUser(String userId, String dogId, Dog dog, FirestoreCallback callback) {
        if (userId == null || userId.isEmpty() || dogId == null || dogId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("userId/dogId is empty"));
            return;
        }
        db.collection("users")
                .document(userId)
                .collection("dogs")
                .document(dogId)
                .update(dog.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(dogId))
                .addOnFailureListener(callback::onFailure);
    }

    /** מחיקת כלב לפי dogId */
    public void deleteDogForUser(String userId, String dogId, FirestoreCallback callback) {
        if (userId == null || userId.isEmpty() || dogId == null || dogId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("userId/dogId is empty"));
            return;
        }
        db.collection("users")
                .document(userId)
                .collection("dogs")
                .document(dogId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(dogId))
                .addOnFailureListener(callback::onFailure);
    }

    // ===== Friends =====
    // נשמר גם בתת-האסופה users/{uid}/friends/{friendUid}
    // וגם במערך friendsIds במסמך המשתמש (נוח לקריאות מרוכזות ולטעינה מהירה)

    public void addFriend(String meUserId, String otherUserId, FirestoreCallback cb) {
        if (meUserId == null || meUserId.isEmpty() || otherUserId == null || otherUserId.isEmpty()) {
            cb.onFailure(new IllegalArgumentException("meUserId/otherUserId is empty"));
            return;
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("createdAt", FieldValue.serverTimestamp());

        friendsCol(meUserId).document(otherUserId)
                .set(doc, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    // עדכני גם את מערך friendsIds במסמך
                    db.collection("users").document(meUserId)
                            .update("friendsIds", FieldValue.arrayUnion(otherUserId))
                            .addOnSuccessListener(vv -> cb.onSuccess(otherUserId))
                            .addOnFailureListener(cb::onFailure);
                })
                .addOnFailureListener(cb::onFailure);
    }

    public void removeFriend(String meUserId, String otherUserId, FirestoreCallback cb) {
        if (meUserId == null || meUserId.isEmpty() || otherUserId == null || otherUserId.isEmpty()) {
            cb.onFailure(new IllegalArgumentException("meUserId/otherUserId is empty"));
            return;
        }

        friendsCol(meUserId).document(otherUserId)
                .delete()
                .addOnSuccessListener(v -> {
                    // עדכני גם את מערך friendsIds במסמך
                    db.collection("users").document(meUserId)
                            .update("friendsIds", FieldValue.arrayRemove(otherUserId))
                            .addOnSuccessListener(vv -> cb.onSuccess(otherUserId))
                            .addOnFailureListener(cb::onFailure);
                })
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

    // קבלת רשימת חברים כ-Users, עם טעינה מרוכזת
    public ListenerRegistration observeFriendsUsers(String uid, FirestoreUsersListCallback cb) {
        return observeFriendsIds(uid, (qs, err) -> {
            if (err != null) { cb.onFailure(err); return; }
            if (qs == null || qs.isEmpty()) {
                cb.onSuccess(new ArrayList<>());
                return;
            }

            List<String> ids = new ArrayList<>();
            for (DocumentSnapshot d : qs) ids.add(d.getId());

            // טעינה מרוכזת במקום N קריאות
            getUsersByIds(ids, cb);
        });
    }

    // שליפה חד-פעמית של מזהי חברים מתוך תת-האוסף
    public void getFriendIdsOnce(String uid, FirestoreIdsListCallback cb) {
        friendsCol(uid).orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot d : q) ids.add(d.getId());
                    cb.onSuccess(ids);
                })
                .addOnFailureListener(cb::onFailure);
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

    public interface FirestoreDogsListCallback {
        void onSuccess(List<Dog> dogs);
        void onFailure(Exception e);
    }

    public interface FirestoreUsersWithIdsCallback {
        void onSuccess(List<Pair<String, User>> rows);
        void onFailure(Exception e);
    }

    public interface FirestoreIdsListCallback {
        void onSuccess(List<String> ids);
        void onFailure(Exception e);
    }
}