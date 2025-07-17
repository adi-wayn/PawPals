package model.firebase;

import android.util.Log;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.*;

import java.util.*;

public class LocationRepository {
    private static final String TAG = "LocationRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // שמירת מיקום משתמש
    public void updateUserLocation(String userId, double lat, double lng) {
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("latitude", lat);
        locationMap.put("longitude", lng);

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
                                        if (lat != null && lng != null) {
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

    // === ממשקי callback ===

    public interface FirestoreLocationsCallback {
        void onSuccess(Map<String, LatLng> userLocations);
        void onFailure(Exception e);
    }

    public interface FirestoreUserLocationsWithNamesCallback {
        void onSuccess(Map<String, Pair<LatLng, String>> userLocationsWithNames);
        void onFailure(Exception e);
    }
}
