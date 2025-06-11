package model.firebase;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
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

    // יצירת פרופיל
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

    // קבלת משתמש לפי ID
    public void getUserById(String userId, FirestoreUserCallback callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
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
                .whereEqualTo("isManager", "true")
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
                .whereEqualTo("community", communityName)
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

    // === ממשקי callback ===

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
