package model.firebase;

import android.util.Log;

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

    // שליפת מיקומים של כל המשתמשים
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

    // שליפת מיקומים לפי קהילה
    public void getUserLocationsByCommunity(String communityName, FirestoreLocationsCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // שלב 1: שליפת כל המשתמשים בקהילה
        db.collection("users")
                .whereEqualTo("communityName", communityName)
                .get()
                .addOnSuccessListener(userQuery -> {
                    List<String> userIds = new ArrayList<>();
                    for (DocumentSnapshot userDoc : userQuery.getDocuments()) {
                        userIds.add(userDoc.getId());
                    }

                    if (userIds.isEmpty()) {
                        callback.onSuccess(Collections.emptyMap());
                        return;
                    }

                    // שלב 2: פיצול מזהים לקבוצות של 10
                    List<List<String>> batches = splitIntoBatches(userIds, 10);
                    Map<String, LatLng> allLocations = new HashMap<>();
                    int totalBatches = batches.size();

                    // מעקב אחרי כמות הקריאות שנותרו
                    final int[] remaining = {totalBatches};
                    final boolean[] failed = {false};

                    for (List<String> batch : batches) {
                        db.collection("locations")
                                .whereIn(FieldPath.documentId(), batch)
                                .get()
                                .addOnSuccessListener(locationQuery -> {
                                    for (DocumentSnapshot locDoc : locationQuery.getDocuments()) {
                                        Double lat = locDoc.getDouble("latitude");
                                        Double lng = locDoc.getDouble("longitude");
                                        if (lat != null && lng != null) {
                                            allLocations.put(locDoc.getId(), new LatLng(lat, lng));
                                        }
                                    }

                                    remaining[0]--;
                                    if (remaining[0] == 0 && !failed[0]) {
                                        callback.onSuccess(allLocations);
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

    private List<List<String>> splitIntoBatches(List<String> fullList, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < fullList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, fullList.size());
            batches.add(new ArrayList<>(fullList.subList(i, end)));
        }
        return batches;
    }


    // Callback interface
    public interface FirestoreLocationsCallback {
        void onSuccess(Map<String, LatLng> userLocations);
        void onFailure(Exception e);
    }
}
