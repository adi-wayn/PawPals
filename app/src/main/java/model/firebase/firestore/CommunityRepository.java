package model.firebase.firestore;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Community;
import model.Message;
import model.Report;

public class CommunityRepository {
    private static final String TAG = "CommunityRepository";
    private final FirebaseFirestore db;
    public CommunityRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // יצירת קהילה חדשה
    public void createCommunity(String communityName, String managerUserId,double latitude, double longitude, List<Report> reports, FirestoreCallback callback) {
        Map<String, Object> communityData = new HashMap<>();
        communityData.put("name", communityName);
        communityData.put("managerId", managerUserId);
        communityData.put("reports", reports);
        communityData.put("latitude", latitude); // חדש
        communityData.put("longitude", longitude); // חדש

        db.collection("communities")
                .document(communityName)
                .set(communityData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Community created: " + communityName);
                    callback.onSuccess(communityName);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to create community", e);
                    callback.onFailure(e);
                });
    }

    // קבלת קהילה לפי ID (שם)
    public void getCommunityById(String communityId, FirestoreCommunityCallback callback) {
        db.collection("communities")
                .document(communityId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Community community = snapshot.toObject(Community.class);
                        callback.onSuccess(community);
                    } else {
                        callback.onFailure(new Exception("Community not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // קבלת כל הקהילות
    public void getAllCommunities(FirestoreCommunitiesListCallback callback) {
        db.collection("communities")
                .get()
                .addOnSuccessListener(query -> {
                    List<Community> communities = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        communities.add(doc.toObject(Community.class));
                    }
                    callback.onSuccess(communities);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // קהילה לפי מזהה מנהל
    public void getCommunityByManager(String managerUserId, FirestoreCommunityCallback callback) {
        db.collection("communities")
                .whereEqualTo("managerId", managerUserId)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        Community community = query.getDocuments().get(0).toObject(Community.class);
                        callback.onSuccess(community);
                    } else {
                        callback.onFailure(new Exception("Manager has no community"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }


    // בדיקה אם קהילה קיימת
    public void checkIfCommunityExists(String communityId, FirestoreExistCallback callback) {
        db.collection("communities")
                .document(communityId)
                .get()
                .addOnSuccessListener(snapshot -> callback.onResult(snapshot.exists()))
                .addOnFailureListener(callback::onError);
    }

    // החזרת documentId לפי שם הקהילה
    public void getCommunityIdByName(String communityName, FirestoreIdCallback callback) {
        db.collection("communities")
                .whereEqualTo("name", communityName)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String id = query.getDocuments().get(0).getId();
                        callback.onSuccess(id);
                    } else {
                        callback.onFailure(new Exception("No community found with the given name"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }


    // יצירת דיווח בתוך קהילה
    public void createReport(String communityId, Report report, FirestoreCallback callback) {
        Map<String, Object> reportMap = report.toMap();

        db.collection("communities")
                .document(communityId)
                .collection("reports")
                .add(reportMap)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Report created under community " + communityId);
                    callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to create report", e);
                    callback.onFailure(e);
                });
    }
    public void createMessage(String communityId, Message message, FirestoreCallback callback) {
        Map<String, Object> MessageMap = message.toMap();

        db.collection("communities")
                .document(communityId)
                .collection("messages")
                .add(MessageMap)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Message created under community " + communityId);
                    callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to create message", e);
                    callback.onFailure(e);
                });
    }

    // שליפה של כל הדיווחים של קהילה
    public void getReportsByCommunity(String communityId, FirestoreReportsListCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("reports")
                .get()
                .addOnSuccessListener(query -> {
                    List<Report> reports = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Report r = doc.toObject(Report.class);
                        if (r != null) r.setId(doc.getId());   // <<< חשוב
                        reports.add(r);
                    }
                    callback.onSuccess(reports);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // מחיקת דיווח לפי id
    public void deleteReport(String communityId, String reportId, FirestoreCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("reports")
                .document(reportId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(reportId))
                .addOnFailureListener(callback::onFailure);
    }

    // שליפה של דיווחים לפי משתמש בתוך קהילה
    public void getReportsByUserInCommunity(String communityId, String senderName, FirestoreReportsListCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("reports")
                .whereEqualTo("sender name", senderName)
                .get()
                .addOnSuccessListener(query -> {
                    List<Report> reports = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        reports.add(doc.toObject(Report.class));
                    }
                    callback.onSuccess(reports);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void createFeedPost(String communityId, Report report, FirestoreCallback callback) {
        Map<String, Object> postMap = report.toMap();

        db.collection("communities")
                .document(communityId)
                .collection("feed")
                .add(postMap)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Feed post created under community " + communityId);
                    callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to create feed post", e);
                    callback.onFailure(e);
                });
    }
    // שליפה חד-פעמית של הודעות מהצ'אט של קהילה (communities/{communityId}/chat)
    public void getChatMessagesOnce(String communityId, FirestoreMessagesListCallback callback) {
        if (communityId == null || communityId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("communityId is empty"));
            return;
        }
        db.collection("communities")
                .document(communityId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    List<model.Message> list = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        model.Message m = doc.toObject(model.Message.class);
                        if (m != null) list.add(m);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // האזנה בזמן אמת לצ'אט של קהילה (communities/{communityId}/chat)
    public com.google.firebase.firestore.ListenerRegistration listenToChatStream(
            String communityId,
            FirestoreMessagesChangeCallback callback
    ) {
        if (communityId == null || communityId.isEmpty()) {
            callback.onError(new IllegalArgumentException("communityId is empty"));
            return null;
        }
        return db.collection("communities")
                .document(communityId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { callback.onError(e); return; }
                    if (snap == null) return;
                    callback.onChanges(snap.getDocumentChanges());
                });
    }


    public void getFeedPosts(String communityId, FirestoreReportsListCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("feed")
                .get()
                .addOnSuccessListener(query -> {
                    List<Report> posts = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        posts.add(doc.toObject(Report.class));
                    }
                    callback.onSuccess(posts);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getCommunityCenterAndRadiusByName(String communityName, CommunityGeoCallback cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("communities")
                .whereEqualTo("name", communityName)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        DocumentSnapshot doc = qs.getDocuments().get(0);
                        Double lat = doc.getDouble("centerLat");
                        Double lng = doc.getDouble("centerLng");
                        Long radius = doc.getLong("radiusMeters");
                        if (lat != null && lng != null) {
                            cb.onSuccess(lat, lng, radius != null ? radius.intValue() : 1500);
                        } else {
                            cb.onFailure(new IllegalStateException("Missing centerLat/centerLng"));
                        }
                    } else {
                        cb.onFailure(new IllegalStateException("Community not found"));
                    }
                })
                .addOnFailureListener(cb::onFailure);
    }
    // שליחת הודעה לקהילה
    public void sendMessage(String communityId, model.Message message, FirestoreCallback callback) {
        if (communityId == null || communityId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("communityId is empty"));
            return;
        }
        // אופציונלי: נשמור גם chatId בתוך ההודעה (נוח לשאילתות/דיבוג)
        message.setChatId(communityId);

        db.collection("communities")
                .document(communityId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(callback::onFailure);

    }

    // שליפה חד־פעמית של כל ההודעות לפי קהילה (מסודר לפי זמן)
    public void getMessagesOnce(String communityId, FirestoreMessagesListCallback callback) {
        if (communityId == null || communityId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("communityId is empty"));
            return;
        }

        db.collection("communities")
                .document(communityId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    List<model.Message> list = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        model.Message m = doc.toObject(model.Message.class);
                        if (m != null) list.add(m);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }
    // האזנה בזמן אמת לשינויים בצ'אט של קהילה
// מחזיר ListenerRegistration כדי שתוכלי להסיר ב-onStop()
    public com.google.firebase.firestore.ListenerRegistration listenToMessagesStream(
            String communityId,
            FirestoreMessagesChangeCallback callback
    ) {
        if (communityId == null || communityId.isEmpty()) {
            callback.onError(new IllegalArgumentException("communityId is empty"));
            return null;
        }

        return db.collection("communities")
                .document(communityId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        callback.onError(e);
                        return;
                    }
                    if (snap == null) return;

                    // מעבירים רק שינויים אינקרמנטליים (ADD/MODIFY/REMOVE)
                    List<com.google.firebase.firestore.DocumentChange> changes = snap.getDocumentChanges();
                    callback.onChanges(changes);
                });
    }

    // הדגל: האם פתוח להגיש מועמדות
    public void setManagerApplicationsOpen(String communityId, boolean open, FirestoreCallback cb) {
        db.collection("communities").document(communityId)
                .update("managerApplicationsOpen", open)
                .addOnSuccessListener(v -> cb.onSuccess(communityId))
                .addOnFailureListener(cb::onFailure);
    }

    public void getManagerApplicationsOpen(String communityId, FirestoreBooleanCallback cb) {
        db.collection("communities").document(communityId).get()
                .addOnSuccessListener(s -> cb.onSuccess(Boolean.TRUE.equals(s.getBoolean("managerApplicationsOpen"))))
                .addOnFailureListener(cb::onFailure);
    }

    // העברת ניהול: עדכון מנהל בקהילה + isManager של שני המשתמשים (Batch אטומי)
    public void transferManager(String communityId, String oldManagerUid, String newManagerUid, FirestoreCallback cb) {
        com.google.firebase.firestore.WriteBatch b = db.batch();
        com.google.firebase.firestore.DocumentReference comm = db.collection("communities").document(communityId);
        com.google.firebase.firestore.DocumentReference oldU = db.collection("users").document(oldManagerUid);
        com.google.firebase.firestore.DocumentReference newU = db.collection("users").document(newManagerUid);

        b.update(comm, "managerId", newManagerUid);
        b.update(oldU, "isManager", false);
        b.update(newU, "isManager", true);

        b.commit().addOnSuccessListener(v -> cb.onSuccess(communityId))
                .addOnFailureListener(cb::onFailure);
    }



    // ===================== CHAT callbacks =====================
    public interface FirestoreMessagesListCallback {
        void onSuccess(List<model.Message> messages);
        void onFailure(Exception e);
    }

    public interface FirestoreMessagesChangeCallback {
        void onChanges(List<com.google.firebase.firestore.DocumentChange> changes);
        void onError(Exception e);
    }
    // === ממשקי callback ===

    public interface CommunityGeoCallback {
        void onSuccess(double lat, double lng, int radiusMeters);
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

    public interface FirestoreCommunityCallback {
        void onSuccess(Community community);
        void onFailure(Exception e);
    }

    public interface FirestoreCommunitiesListCallback {
        void onSuccess(List<Community> communities);
        void onFailure(Exception e);
    }

    public interface FirestoreIdCallback {
        void onSuccess(String id);
        void onFailure(Exception e);
    }

    public interface FirestoreReportsListCallback {
        void onSuccess(List<Report> reports);
        void onFailure(Exception e);
    }

    public interface FirestoreBooleanCallback {
        void onSuccess(boolean value);
        void onFailure(Exception e);
    }
}
