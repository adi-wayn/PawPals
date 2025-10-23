package model.firebase.Firestore;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

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
    // 🔹 קונסטרקטור נוסף – מיוחד ל־Unit Tests
    public CommunityRepository(FirebaseFirestore db) {
        this.db = db;
    }

    // יצירת קהילה חדשה
    public void createCommunity(String communityName, String managerUserId,
                                double latitude, double longitude, String description, String imageUrl,
                                List<Report> reports, FirestoreCallback callback) {
        Map<String, Object> communityData = new HashMap<>();
        communityData.put("name", communityName);
        communityData.put("managerId", managerUserId);
        communityData.put("reports", reports);
        communityData.put("latitude", latitude);
        communityData.put("longitude", longitude);
        communityData.put("description", description);
        communityData.put("imageUrl", imageUrl);

        db.collection("communities")
                .document(communityName)
                .set(communityData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Community created: " + communityName);
                    callback.onSuccess(communityName);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateCommunityManager(String communityName, String newManagerId, FirestoreCallback callback){
        db.collection("communities")
                .document(communityName)
                .update("managerId", newManagerId)
                .addOnSuccessListener(aVoid -> callback.onSuccess(newManagerId))
                .addOnFailureListener(callback::onFailure);
    }

    // קבלת קהילה לפי ID (שם)
    public void getCommunityById(String communityId, FirestoreCommunityCallback callback) {

    }


    // 🔹 עדכון תיאור הקהילה
    public void updateCommunityDescription(String communityId, String description, FirestoreCallback callback) {
        db.collection("communities")
                .document(communityId)
                .update("description", description)
                .addOnSuccessListener(v -> callback.onSuccess(communityId))
                .addOnFailureListener(callback::onFailure);
    }

    // 🔹 עדכון תמונת פרופיל של קהילה
    public void updateCommunityImage(String communityId, String imageUrl, FirestoreCallback callback) {
        db.collection("communities")
                .document(communityId)
                .update("imageUrl", imageUrl)
                .addOnSuccessListener(v -> callback.onSuccess(communityId))
                .addOnFailureListener(callback::onFailure);
    }

    public void getCommunityDetails(String communityId, FirestoreCommunityCallback callback) {
        db.collection("communities")
                .document(communityId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String description = doc.getString("description");
                        String imageUrl = doc.getString("imageUrl");
                        callback.onSuccess(description, imageUrl);
                    } else {
                        callback.onFailure(new Exception("Community not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ✅ יצירת הודעה בצ'אט (שומר את ה-id)
    public void createMessage(String communityId, Message message, FirestoreCallback callback) {
        Map<String, Object> messageMap = message.toMap();
        messageMap.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("communities")
                .document(communityId)
                .collection("messages")
                .add(messageMap)
                .addOnSuccessListener(docRef -> {
                    message.setId(docRef.getId());
                    callback.onSuccess(docRef.getId());
                })
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

    public void findCommunityNearby(double userLat, double userLng, int radiusMeters, FirestoreCommunityCallback callback) {
        db.collection("communities")
                .get()
                .addOnSuccessListener(query -> {
                    Community closest = null;
                    double closestDistance = Double.MAX_VALUE;

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        if (lat != null && lng != null) {
                            double distance = distanceInMeters(userLat, userLng, lat, lng);
                            if (distance <= radiusMeters && distance < closestDistance) {
                                closestDistance = distance;
                                closest = doc.toObject(Community.class);
                                if (closest != null) closest.setName(doc.getId());
                            }
                        }
                    }

                    if (closest != null) {
//                        callback.onSuccess(closest);
                    } else {
                        callback.onFailure(new Exception("No nearby community found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void findCommunitiesNearby(double userLat, double userLng, double radiusKm, FirestoreCommunitiesListCallback callback) {
        db.collection("communities").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Community> nearbyCommunities = new ArrayList<>();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Double lat = doc.getDouble("latitude");
                    Double lng = doc.getDouble("longitude");

                    if (lat != null && lng != null) {
                        double distanceKm = haversineDistance(userLat, userLng, lat, lng) / 1000.0; // convert to km

                        Log.d("CommunityRepo", "Community: " + doc.getId() + " distance: " + distanceKm + "km");

                        if (distanceKm <= radiusKm) {
                            Community community = doc.toObject(Community.class);
                            if (community != null) {
                                community.setName(doc.getId()); // in case name is missing
                                nearbyCommunities.add(community);
                            }
                        }
                    }
                }

                Log.d("CommunityRepo", "Nearby communities found: " + nearbyCommunities.size());
                callback.onSuccess(nearbyCommunities);
            } else {
                Log.e("CommunityRepo", "Error fetching communities", task.getException());
                callback.onFailure(task.getException());
            }
        });
    }


    // Haversine formula (distance in meters)
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public interface FirestoreCommunitiesListCallback {
        void onSuccess(List<Community> communities);
        void onFailure(Exception e);
    }


    // Haversine formula to calculate distance between two coordinates
    private double distanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        int R = 6371000; // radius of Earth in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
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

    // 🔹 שליפת פוסטים מה־feed
    public void getFeedPosts(String communityId, FirestoreReportsListCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("feed")
//                .orderBy("timestamp", Query.Direction.DESCENDING) // ✅ מיון ישירות ב־Firestore
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

    // 🔹 בדיקת האם פתוח להגשת מועמדויות
    public void getManagerApplicationsOpen(String communityId, FirestoreBooleanCallback callback) {
        db.collection("communities")
                .document(communityId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean val = doc.getBoolean("applicationsOpen");
                        callback.onSuccess(val != null && val);
                    } else {
                        callback.onSuccess(false);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // 🔹 שינוי מצב קבלת מועמדויות
    public void setManagerApplicationsOpen(String communityId, boolean open, FirestoreCallback callback) {
        db.collection("communities")
                .document(communityId)
                .update("applicationsOpen", open)
                .addOnSuccessListener(v -> callback.onSuccess(communityId))
                .addOnFailureListener(callback::onFailure);
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
        db.collection("communities")
                .document(communityId)
                .collection("feed")
                .document(postId)
                .delete()
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Feed post deleted: " + postId);
                    callback.onSuccess(postId);
                })
                .addOnFailureListener(callback::onFailure);
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

    // ===================== מתודות שהיו חסרות =====================

    // ✅ יצירת דיווח חדש
    public void createReport(String communityId, Report report, FirestoreCallback callback) {
        db.collection("communities")
                .document(communityId)
                .collection("reports")
                .add(report.toMap())
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    // ✅ עדכון תמונות של דיווח
    public void updateReportImages(String communityId, String reportId, String field,
                                   List<String> urls, FirestoreCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("imageUrls", urls);
        db.collection("communities")
                .document(communityId)
                .collection("reports")
                .document(reportId)
                .update(data)
                .addOnSuccessListener(v -> callback.onSuccess(reportId))
                .addOnFailureListener(callback::onFailure);
    }

    // ✅ העברת מנהל קהילה
    public void transferManager(String communityId, String oldManagerId, String newManagerId,
                                FirestoreCallback callback) {
        // עדכון מנהל הקהילה החדש ב־communities
        db.collection("communities")
                .document(communityId)
                .update("managerId", newManagerId)
                .addOnSuccessListener(v -> {
                    // עדכון המשתמש הישן - כבר לא מנהל
                    db.collection("users")
                            .document(oldManagerId)
                            .update("isManager", false)
                            .addOnSuccessListener(aVoid -> {
                                // עדכון המשתמש החדש - עכשיו מנהל
                                db.collection("users")
                                        .document(newManagerId)
                                        .update("isManager", true)
                                        .addOnSuccessListener(bVoid -> {
                                            callback.onSuccess(communityId);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Firestore", "Failed to update new manager flag", e);
                                            callback.onFailure(e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Failed to update old manager flag", e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ✅ שליפת מרכז ורדיוס של קהילה (lat/lng + radius)
    public void getCommunityCenterAndRadiusByName(String communityName, CommunityGeoCallback callback) {
        db.collection("communities")
                .whereEqualTo("name", communityName)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        DocumentSnapshot doc = query.getDocuments().get(0);
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        Long radius = doc.getLong("radiusMeters");
                        if (lat != null && lng != null) {
                            callback.onSuccess(lat, lng, radius != null ? radius.intValue() : 1000);
                        } else {
                            callback.onFailure(new Exception("No geo data for community"));
                        }
                    } else {
                        callback.onFailure(new Exception("Community not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ===================== ממשקי Callback =====================
    public interface FirestoreCallback {
        void onSuccess(String documentId);
        void onFailure(Exception e);
    }

    public interface FirestoreCommunityCallback {
        void onSuccess(String description, String imageUrl);
        void onFailure(Exception e);
    }

    public interface FirestoreMessagesChangeCallback {
        void onChanges(List<com.google.firebase.firestore.DocumentChange> changes);
        void onError(Exception e);
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

    public interface CommunityGeoCallback {
        void onSuccess(double lat, double lng, int radiusMeters);
        void onFailure(Exception e);
    }
}