package model.firebase;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Community;

public class CommunityRepository {
    private static final String TAG = "CommunityRepository";
    private final FirebaseFirestore db;

    public CommunityRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // יצירת קהילה חדשה
    public void createCommunity(String communityName, String managerUserId, List<Report> reports, FirestoreCallback callback) {
        Map<String, Object> communityData = new HashMap<>();
        communityData.put("name", communityName);
        communityData.put("managerId", managerUserId);
        communityData.put("reports", reports);

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

    // === ממשקי callback ===

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

    public interface FirestoreReportsListCallback {
        void onSuccess(List<Report> reports);
        void onFailure(Exception e);
    }
}
