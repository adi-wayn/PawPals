package model.firebase.firestore;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

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
    public void createCommunity(String communityName, String managerUserId,
                                double latitude, double longitude,
                                List<Report> reports, FirestoreCallback callback) {
        Map<String, Object> communityData = new HashMap<>();
        communityData.put("name", communityName);
        communityData.put("managerId", managerUserId);
        communityData.put("reports", reports);
        communityData.put("latitude", latitude);
        communityData.put("longitude", longitude);

        db.collection("communities")
                .document(communityName)
                .set(communityData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Community created: " + communityName);
                    callback.onSuccess(communityName);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // יצירת הודעה בצ'אט
    public void createMessage(String communityId, Message message, FirestoreCallback callback) {
        Map<String, Object> messageMap = message.toMap();

        db.collection("communities")
                .document(communityId)
                .collection("messages")
                .add(messageMap)
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    // ✅ יצירת Report חדש תחת קהילה
    public void createReport(String communityId, Report report, FirestoreCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("reports")
                .add(report.toMap())
                .addOnSuccessListener(docRef -> {
                    report.setId(docRef.getId()); // שומר את ה-id באובייקט
                    callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ✅ עדכון תמונות בדו"ח
    public void updateReportImages(String communityId,
                                   String reportId,
                                   String singleUrl,
                                   List<String> urls,
                                   FirestoreCallback callback) {
        Map<String, Object> updateData = new HashMap<>();
        if (singleUrl != null) {
            updateData.put("imageUrl", singleUrl);
        }
        if (urls != null && !urls.isEmpty()) {
            updateData.put("imageUrls", urls);
        }

        db.collection("communities")
                .document(communityId)
                .collection("reports")
                .document(reportId)
                .update(updateData)
                .addOnSuccessListener(v -> callback.onSuccess(reportId))
                .addOnFailureListener(callback::onFailure);
    }

    // ✅ מחיקת הודעה מהצ'אט
    public void deleteMessage(String communityId, String messageId, FirestoreCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("messages")
                .document(messageId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(messageId))
                .addOnFailureListener(callback::onFailure);
    }

    // האזנה בזמן אמת לצ'אט
    public ListenerRegistration listenToMessagesStream(
            String communityId,
            FirestoreMessagesChangeCallback callback
    ) {
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
                    callback.onChanges(snap.getDocumentChanges());
                });
    }

    // 🔹 שליפת communityId לפי שם
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
                        callback.onFailure(new Exception("No community found with that name"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // 🔹 שליפת כל ה־Reports מתוך הקהילה
    public void getReportsByCommunity(String communityId, FirestoreReportsListCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("reports")
                .get()
                .addOnSuccessListener(query -> {
                    List<Report> reports = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Report r = doc.toObject(Report.class);
                        if (r != null) r.setId(doc.getId());
                        reports.add(r);
                    }
                    callback.onSuccess(reports);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // 🔹 שליפת פוסטים מהפיד
    public void getFeedPosts(String communityId, FirestoreReportsListCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("feed")
                .get()
                .addOnSuccessListener(query -> {
                    List<Report> posts = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Report r = doc.toObject(Report.class);
                        if (r != null) r.setId(doc.getId());
                        posts.add(r);
                    }
                    callback.onSuccess(posts);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // 🔹 בדיקת אפשרות הגשת מועמדות לניהול
    public void getManagerApplicationsOpen(String communityId, FirestoreBooleanCallback cb) {
        db.collection("communities").document(communityId).get()
                .addOnSuccessListener(s -> cb.onSuccess(Boolean.TRUE.equals(s.getBoolean("managerApplicationsOpen"))))
                .addOnFailureListener(cb::onFailure);
    }

    public void setManagerApplicationsOpen(String communityId, boolean open, FirestoreCallback cb) {
        db.collection("communities").document(communityId)
                .update("managerApplicationsOpen", open)
                .addOnSuccessListener(v -> cb.onSuccess(communityId))
                .addOnFailureListener(cb::onFailure);
    }

    // 🔹 שליפת מיקום קהילה לפי שם
    public void getCommunityCenterAndRadiusByName(String communityName, CommunityGeoCallback cb) {
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
                            cb.onFailure(new Exception("Missing lat/lng"));
                        }
                    } else {
                        cb.onFailure(new Exception("Community not found"));
                    }
                })
                .addOnFailureListener(cb::onFailure);
    }

    // 🔹 העברת ניהול
    public void transferManager(String communityId, String oldManagerUid, String newManagerUid, FirestoreCallback cb) {
        com.google.firebase.firestore.WriteBatch b = db.batch();
        b.update(db.collection("communities").document(communityId), "managerId", newManagerUid);
        b.update(db.collection("users").document(oldManagerUid), "isManager", false);
        b.update(db.collection("users").document(newManagerUid), "isManager", true);

        b.commit().addOnSuccessListener(v -> cb.onSuccess(communityId))
                .addOnFailureListener(cb::onFailure);
    }

    // 🔹 יצירת פוסט בפיד
    public void createFeedPost(String communityId, Report report, FirestoreCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("feed")
                .add(report.toMap())
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    // 🔹 מחיקת פוסט מה-Feed
    public void deleteFeedPost(String communityId, String postId, FirestoreCallback callback) {
        if (communityId == null || communityId.isEmpty() ||
                postId == null || postId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("communityId or postId is empty"));
            return;
        }

        db.collection("communities")
                .document(communityId)
                .collection("feed")
                .document(postId)
                .delete()
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Feed post deleted: " + postId);
                    callback.onSuccess(postId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to delete feed post: " + postId, e);
                    callback.onFailure(e);
                });
    }

    // 🔹 מחיקת דיווח
    public void deleteReport(String communityId, String reportId, FirestoreCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("reports")
                .document(reportId)
                .delete()
                .addOnSuccessListener(v -> callback.onSuccess(reportId))
                .addOnFailureListener(callback::onFailure);
    }

    // ===================== ממשקי Callback =====================
    public interface FirestoreCallback {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }

    public interface FirestoreMessagesListCallback {
        void onSuccess(List<Message> messages);
        void onFailure(Exception e);
    }

    public interface FirestoreMessagesChangeCallback {
        void onChanges(List<com.google.firebase.firestore.DocumentChange> changes);
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

    public interface FirestoreExistCallback {
        void onResult(boolean exists);
        void onError(Exception e);
    }

    public interface FirestoreBooleanCallback {
        void onSuccess(boolean value);
        void onFailure(Exception e);
    }

    public interface CommunityGeoCallback {
        void onSuccess(double lat, double lng, int radiusMeters);
        void onFailure(Exception e);
    }
}
