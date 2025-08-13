package model.firebase;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Community;
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

    // שליפה של כל הדיווחים של קהילה
    public void getReportsByCommunity(String communityId, FirestoreReportsListCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("reports")
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
}
