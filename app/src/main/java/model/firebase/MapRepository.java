package model.firebase;

import android.util.Log;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.*;

import model.MapReport;

public class MapRepository {
    private static final String TAG = "MapRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    // מאזין למיקומי משתמשים
    private ListenerRegistration liveLocationListener;
    // מאזין לדיווחי מפה
    private ListenerRegistration mapReportsListener;

    // שמירת מיקום משתמש
    public void updateUserLocation(String userId, double lat, double lng) {
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("latitude", lat);
        locationMap.put("longitude", lng);
        locationMap.put("lastSeen", FieldValue.serverTimestamp()); // זמן נוכחי מהשרת

        db.collection("locations")
                .document(userId)
                .set(locationMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location updated for " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update location", e));
    }

    // שליפת מיקומים של כל המשתמשים (ללא קשר לקהילה)
    public void getAllUserLocations(FirestoreLocationsCallback callback) {
        db.collection("locations")
                .get()
                .addOnSuccessListener(query -> {
                    Map<String, LatLng> locations = new HashMap<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        if (lat != null && lng != null) {
                            locations.put(doc.getId(), new LatLng(lat, lng));
                        }
                    }
                    callback.onSuccess(locations);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // שליפת מיקומים + שמות משתמשים לפי קהילה
    public void getUserLocationsWithNamesByCommunity(String communityName, FirestoreUserLocationsWithNamesCallback callback) {
        db.collection("users")
                .whereEqualTo("communityName", communityName)
                .get()
                .addOnSuccessListener(userQuery -> {
                    List<String> userIds = new ArrayList<>();
                    Map<String, String> userNamesMap = new HashMap<>();

                    for (DocumentSnapshot userDoc : userQuery.getDocuments()) {
                        String uid = userDoc.getId();
                        String name = userDoc.getString("userName");
                        userIds.add(uid);
                        userNamesMap.put(uid, name != null ? name : "אנונימי");
                    }

                    if (userIds.isEmpty()) {
                        callback.onSuccess(Collections.emptyMap());
                        return;
                    }

                    List<List<String>> batches = splitIntoBatches(userIds, 10);
                    Map<String, Pair<LatLng, String>> resultMap = new HashMap<>();
                    final int[] remaining = {batches.size()};
                    final boolean[] failed = {false};

                    for (List<String> batch : batches) {
                        db.collection("locations")
                                .whereIn(FieldPath.documentId(), batch)
                                .get()
                                .addOnSuccessListener(locationQuery -> {
                                    for (DocumentSnapshot doc : locationQuery.getDocuments()) {
                                        Double lat = doc.getDouble("latitude");
                                        Double lng = doc.getDouble("longitude");
                                        Timestamp lastSeen = doc.getTimestamp("lastSeen");

                                        // סינון לפי זמן: רק אם המשתמש נראה ב־5 דקות האחרונות
                                        if (lat != null && lng != null && lastSeen != null &&
                                                lastSeen.toDate().after(new Date(System.currentTimeMillis() - 5 * 60 * 1000))) {

                                            String uid = doc.getId();
                                            LatLng loc = new LatLng(lat, lng);
                                            String name = userNamesMap.get(uid);
                                            resultMap.put(uid, new Pair<>(loc, name));
                                        }
                                    }

                                    remaining[0]--;
                                    if (remaining[0] == 0 && !failed[0]) {
                                        callback.onSuccess(resultMap);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (!failed[0]) {
                                        failed[0] = true;
                                        callback.onFailure(e);
                                    }
                                });
                    }

                })
                .addOnFailureListener(callback::onFailure);
    }

    // פונקציית עזר לפיצול רשימות לקבוצות של 10
    private List<List<String>> splitIntoBatches(List<String> fullList, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < fullList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, fullList.size());
            batches.add(new ArrayList<>(fullList.subList(i, end)));
        }
        return batches;
    }

    public void listenToCommunityLocations(String communityName, String currentUserId, UserRepository userRepo, FirestoreLiveLocationCallback callback) {
        userRepo.getUserNamesByCommunity(communityName, new UserRepository.FirestoreUserNamesCallback() {
            @Override
            public void onSuccess(Map<String, String> userNames) {
                List<String> userIds = new ArrayList<>(userNames.keySet());

                // הסרה של מאזין קיים למיקומי משתמשים בלבד
                if (liveLocationListener != null) {
                    liveLocationListener.remove();
                    liveLocationListener = null;
                }

                liveLocationListener = db.collection("locations")
                        .addSnapshotListener((snapshots, e) -> {
                            if (e != null || snapshots == null) {
                                Log.e(TAG, "Live location listener error", e);
                                return;
                            }

                            long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);

                            for (DocumentChange change : snapshots.getDocumentChanges()) {
                                DocumentSnapshot doc = change.getDocument();
                                String uid = doc.getId();

                                if (!userIds.contains(uid) || uid.equals(currentUserId)) continue;

                                Double lat = doc.getDouble("latitude");
                                Double lng = doc.getDouble("longitude");
                                Timestamp lastSeen = doc.getTimestamp("lastSeen");

                                if (change.getType() == DocumentChange.Type.REMOVED || lastSeen == null ||
                                        lastSeen.toDate().before(new Date(fiveMinutesAgo))) {
                                    callback.onUserRemoved(uid);
                                    continue;
                                }

                                if (lat != null && lng != null) {
                                    LatLng position = new LatLng(lat, lng);
                                    String name = userNames.getOrDefault(uid, "anonymous");
                                    callback.onUserLocationUpdated(uid, name, position);
                                }
                            }
                        });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to get user names by community", e);
            }
        });
    }

    public void removeLiveLocationListener() {
        if (liveLocationListener != null) {
            liveLocationListener.remove();
            liveLocationListener = null;
        }
    }

    // שליפת קהילות בטווח גיאוגרפי מסוים (לדוגמה: רדיוס של 5 ק"מ ממיקום נוכחי)
    public void getNearbyCommunities(double userLat, double userLng, double radiusInMeters, FirestoreNearbyCommunitiesCallback callback) {
        db.collection("communities")
                .get()
                .addOnSuccessListener(query -> {
                    List<String> nearbyCommunities = new ArrayList<>();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");

                        if (lat != null && lng != null) {
                            float[] results = new float[1];
                            android.location.Location.distanceBetween(userLat, userLng, lat, lng, results);
                            float distance = results[0];
                            if (distance <= radiusInMeters) {
                                nearbyCommunities.add(doc.getId());
                            }
                        }
                    }

                    callback.onSuccess(nearbyCommunities);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void createMapReport(String communityName, MapReport report, FirestoreCallback callback) {
        db.collection("communities")
                .document(communityName)
                .collection("mapReports")
                .add(report.toMap())
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Map report created under community " + communityName);
                    callback.onSuccess(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to create map report", e);
                    callback.onFailure(e);
                });
    }

    public void listenToMapReports(String communityName, MapReportsListener listener) {
        // הסר מאזין קיים לדיווחי מפה בלבד
        if (mapReportsListener != null) {
            mapReportsListener.remove();
            mapReportsListener = null;
        }
        CollectionReference ref = db.collection("communities")
                .document(communityName)
                .collection("mapReports");
        mapReportsListener = ref.addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) {
                Log.e(TAG, "listenToMapReports error", e);
                return;
            }
            long now = System.currentTimeMillis();
            for (DocumentChange change : snapshots.getDocumentChanges()) {
                DocumentSnapshot doc = change.getDocument();
                String id = doc.getId();
                Map<String, Object> data = doc.getData();
                String type = (String) data.get("type");
                String senderName = (String) data.get("senderName");
                Double lat = (Double) data.get("latitude");
                Double lng = (Double) data.get("longitude");

                // שליפת timestamp בצורה בטוחה
                Object tsObj = data.get("timestamp");
                long tsMillis = 0L;
                if (tsObj instanceof Timestamp) {
                    tsMillis = ((Timestamp) tsObj).toDate().getTime();
                } else if (tsObj instanceof Long) {
                    tsMillis = (Long) tsObj;
                } else if (tsObj instanceof Double) {
                    tsMillis = ((Double) tsObj).longValue();
                }

                MapReport report = new MapReport(
                        type,
                        senderName,
                        lat != null ? lat : 0.0,
                        lng != null ? lng : 0.0,
                        tsMillis
                );
                // Expire reports older than 15 minutes
                boolean expired = tsMillis > 0 && (now - tsMillis) > (15 * 60 * 1000);
                if (expired) {
                    doc.getReference().delete();
                    listener.onReportRemoved(id);
                    continue;
                }
                if (change.getType() == DocumentChange.Type.REMOVED) {
                    listener.onReportRemoved(id);
                } else {
                    listener.onReportAdded(id, report);
                }
            }
        });
    }

    public void removeMapReportsListener() {
        if (mapReportsListener != null) {
            mapReportsListener.remove();
            mapReportsListener = null;
        }
    }

    // === ממשקי callback ===
    public interface MapReportsListener {
        void onReportAdded(String reportId, MapReport report);

        void onReportRemoved(String reportId);
    }

    public interface FirestoreCallback {
        void onSuccess(String documentId);

        void onFailure(Exception e);
    }

    public interface FirestoreNearbyCommunitiesCallback {
        void onSuccess(List<String> nearbyCommunityIds);
        void onFailure(Exception e);
    }

    public interface FirestoreLiveLocationCallback {
        void onUserLocationUpdated(String userId, String userName, LatLng position);
        void onUserRemoved(String userId);
    }

    public interface FirestoreLocationsCallback {
        void onSuccess(Map<String, LatLng> userLocations);
        void onFailure(Exception e);
    }

    public interface FirestoreUserLocationsWithNamesCallback {
        void onSuccess(Map<String, Pair<LatLng, String>> userLocationsWithNames);
        void onFailure(Exception e);
    }
}
