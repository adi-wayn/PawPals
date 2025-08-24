package model.firebase.firestore;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
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
import model.Dog;
import model.User;

public class UserRepository {
    private static final String TAG = "UserRepository";

    // --- Collection / doc helpers ---
    private static final String COL_USERS   = "users";
    private static final String COL_DOGS    = "dogs";
    private static final String COL_FRIENDS = "friends";

    private final FirebaseFirestore db;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    private CollectionReference usersCol() { return db.collection(COL_USERS); }
    private DocumentReference userDoc(@NonNull String uid) { return usersCol().document(uid); }
    private CollectionReference dogsCol(@NonNull String uid) { return userDoc(uid).collection(COL_DOGS); }
    private DocumentReference dogDoc(@NonNull String uid, @NonNull String dogId) { return dogsCol(uid).document(dogId); }
    private CollectionReference friendsCol(@NonNull String uid) { return userDoc(uid).collection(COL_FRIENDS); }

    // ===== Users =====

    /** יצירת/עדכון פרופיל (merge כדי לא לדרוס שדות קיימים) */
    public void createUserProfile(String userId, User user, FirestoreCallback callback) {
        Map<String, Object> userMap = user.toMap();
        usersCol()
                .document(userId)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created/updated for: " + userId);
                    callback.onSuccess(userId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating/updating user profile", e);
                    callback.onFailure(e);
                });
    }

    /** בדיקה אם משתמש קיים */
    public void checkIfUserProfileExists(String userId, FirestoreExistCallback callback) {
        userDoc(userId)
                .get()
                .addOnSuccessListener(snapshot -> callback.onResult(snapshot.exists()))
                .addOnFailureListener(callback::onError);
    }

    /** קבלת משתמש לפי ID (כולל contactDetails + fieldsOfInterest) */
    public void getUserById(String userId, FirestoreUserCallback callback) {
        userDoc(userId)
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

                    if (user != null) {
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure(new Exception("Failed to parse user data"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore access failure", e);
                    callback.onFailure(e);
                });
    }

    /** קבלת כל המשתמשים */
    public void getAllUsers(FirestoreUsersListCallback callback) {
        usersCol()
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

    /** קבלת כל המנהלים */
    public void getAllManagers(FirestoreUsersListCallback callback) {
        usersCol()
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

    /** קבלת משתמשים לפי קהילה */
    public void getUsersByCommunity(String communityName, FirestoreUsersListCallback callback) {
        usersCol()
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

    /** קבלת משתמשים לפי קהילה + מזהים */
    public void getUsersByCommunityWithIds(String communityName, FirestoreUsersWithIdsCallback callback) {
        usersCol()
                .whereEqualTo("communityName", communityName)
                .get()
                .addOnSuccessListener(query -> {
                    List<Pair<String, User>> rows = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            rows.add(new Pair<>(doc.getId(), u)); // userId + האובייקט
                        }
                    }
                    callback.onSuccess(rows);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /** מפת userId->userName עבור קהילה */
    public void getUserNamesByCommunity(String communityName, FirestoreUserNamesCallback callback) {
        usersCol()
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

    // ===== Dogs (תת־אוסף תחת המשתמש) =====

    /** יצירת כלב חדש תחת המשתמש (users/{userId}/dogs/{autoId}) + ownerUid + createdAt */
    public void addDogToUser(String userId, Dog dog, FirestoreCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("userId is empty"));
            return;
        }
        DocumentReference newDogRef = dogsCol(userId).document(); // autoId

        Map<String, Object> data = (dog != null ? dog.toMap() : new HashMap<>());
        if (data == null) data = new HashMap<>();
        data.put("ownerUid", userId);
        data.put("createdAt", FieldValue.serverTimestamp());

        newDogRef.set(data, SetOptions.merge())
                .addOnSuccessListener(v -> callback.onSuccess(newDogRef.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    /** גרסה טרנזאקציונית: מוסיף כלב ומגדיל dogsCount באטומיות (תיקון: base final) */
    public void addDogToUserWithCounter(String userId, Dog dog, FirestoreCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("userId is empty"));
            return;
        }
        DocumentReference userRef = userDoc(userId);
        DocumentReference dogRef  = dogsCol(userId).document();

        // בונים מפה final מראש (למניעת "effectively final" שגיאה)
        final Map<String, Object> base = new HashMap<>();
        if (dog != null) {
            Map<String, Object> dogMap = dog.toMap();
            if (dogMap != null) base.putAll(dogMap);
        }
        base.put("ownerUid", userId);
        base.put("createdAt", FieldValue.serverTimestamp());

        db.runTransaction(trx -> {
                    trx.set(dogRef, base, SetOptions.merge());
                    trx.update(userRef, "dogsCount", FieldValue.increment(1));
                    return dogRef.getId();
                }).addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onFailure);
    }

    /** שליפת כל הכלבים של המשתמש */
    public void getDogsForUser(String userId, FirestoreDogsListCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("userId is empty"));
            return;
        }
        dogsCol(userId)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Dog> list = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Map<String, Object> m = d.getData();
                        Dog dog = new Dog();
                        if (m != null) {
                            Object name = m.get("name");
                            Object breed = m.get("breed");
                            Object age = m.get("age");
                            Object neut = m.get("neutered");
                            Object personality = m.get("personality");
                            Object mood = m.get("mood");
                            Object notes = m.get("notes");
                            Object photoUrl = m.get("photoUrl");

                            if (name instanceof String) dog.setName((String) name);
                            if (breed instanceof String) dog.setBreed((String) breed);
                            if (age instanceof Number)  dog.setAge(((Number) age).intValue()); // ודאי setAge(Integer)
                            if (neut instanceof Boolean) dog.setNeutered((Boolean) neut);
                            if (personality instanceof String) dog.setPersonality((String) personality);
                            if (mood instanceof String) dog.setMood((String) mood);
                            if (notes instanceof String) dog.setNotes((String) notes);
                            if (photoUrl instanceof String) dog.setPhotoUrl((String) photoUrl);
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
        Map<String, Object> map = (dog != null ? dog.toMap() : new HashMap<>());
        if (map == null) map = new HashMap<>();
        dogDoc(userId, dogId)
                .update(map)
                .addOnSuccessListener(aVoid -> callback.onSuccess(dogId))
                .addOnFailureListener(callback::onFailure);
    }

    /** מחיקת כלב לפי dogId */
    public void deleteDogForUser(String userId, String dogId, FirestoreCallback callback) {
        if (userId == null || userId.isEmpty() || dogId == null || dogId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("userId/dogId is empty"));
            return;
        }
        dogDoc(userId, dogId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(dogId))
                .addOnFailureListener(callback::onFailure);
    }

    // ===== Friends =====

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

    public interface FirestoreDogsListCallback {
        void onSuccess(List<Dog> dogs);
        void onFailure(Exception e);
    }

    public interface FirestoreUsersWithIdsCallback {
        void onSuccess(List<Pair<String, User>> rows);
        void onFailure(Exception e);
    }
}
