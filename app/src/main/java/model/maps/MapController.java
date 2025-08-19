package model.maps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.pawpals.OtherUserProfileActivity;
import com.example.pawpals.ProfileActivity;
import com.example.pawpals.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.HashMap;
import java.util.Map;

import model.MapReport;
import model.User;
import model.firebase.firestore.MapRepository;
import model.firebase.firestore.UserRepository;

public class MapController {
    private LatLng pendingFocusCenter = null;
    private Integer pendingFocusRadius = null;
    private Circle focusCircle = null;
    private final Context context;
    private final MapView mapView;
    private final FusedLocationProviderClient locationClient;
    private final MapRepository mapRepo = new MapRepository();
    private final UserRepository userRepo = new UserRepository();
    private GoogleMap googleMap;
    private boolean isMapReady = false;
    private String currentUserId;
    private User currentUser;
    private final Map<String, Marker> userMarkers = new HashMap<>();
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private boolean isVisibleToOthers = true;
    private boolean reportMode = false;
    private String reportType;
    private Map<String, Marker> mapReportMarkers = new HashMap<>();

    // cache של משתמשים להצגת הפריוויו מהר
    private final Map<String, User> userCache = new HashMap<>();

    // תג לסימון סוג מרקר (משתמש/דיווח)
    private enum MarkerKind { USER, REPORT }

    private static class MarkerTag {
        final MarkerKind kind;
        final String id; // userId או reportId
        MarkerTag(MarkerKind kind, String id) { this.kind = kind; this.id = id; }
    }


    public MapController(MapView mapView, Context context, String currentUserId) {
        this.mapView = mapView;
        this.context = context;
        this.currentUserId = currentUserId;
        this.locationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void initializeMap(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(map -> {
            googleMap = map;
            isMapReady = true;

            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

            startRealTimeLocationUpdates(); // התחלת עדכון מיקום רציף

            Log.d("MapController", "key " + context.getString(R.string.maps_api_key));

            if (!hasLocationPermission()) {
                Log.w("MapController", "No location permission granted. Showing Tel Aviv.");
                moveToTelAviv();
                return;
            }

            googleMap.setMyLocationEnabled(true);

            googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override public View getInfoWindow(Marker marker) { return null; } // נשתמש ב־getInfoContents

                @Override
                public View getInfoContents(Marker marker) {
                    Object t = marker.getTag();
                    if (!(t instanceof MarkerTag)) return null;
                    MarkerTag tag = (MarkerTag) t;
                    if (tag.kind != MarkerKind.USER) return null; // לדיווחים – החלון הסטנדרטי

                    // ננפח layout מותאם
                    View v = View.inflate(context, R.layout.view_user_preview, null);
                    TextView tvName = v.findViewById(R.id.tvName);
                    TextView tvCommunity = v.findViewById(R.id.tvCommunity);
                    TextView tvBio = v.findViewById(R.id.tvBio);
                    TextView tvManager = v.findViewById(R.id.tvManager);
                    TextView tvDogs     = v.findViewById(R.id.tvDogs);
                    TextView tvContact  = v.findViewById(R.id.tvContact);
                    TextView tvAvatar = v.findViewById(R.id.tvAvatar);

                    // ברירת מחדל מהירה
                    tvName.setText(marker.getTitle());
                    tvCommunity.setText("");
                    tvBio.setText("");
                    tvManager.setVisibility(View.GONE);
                    tvDogs.setText("");
                    tvContact.setText("");

                    // אם יש בקאש – נציג; אחרת נמשוך ואז נרענן
                    User u = userCache.get(tag.id);
                    if (u != null) {
                        tvName.setText(u.getUserName());
                        tvCommunity.setText(u.getCommunityName());
                        if (u.getFieldsOfInterest() != null) tvBio.setText(u.getFieldsOfInterest());
                        if (u.isManager()) tvManager.setVisibility(View.VISIBLE);

                        int dogs = (u.getDogs() == null) ? 0 : u.getDogs().size();
                        tvDogs.setText("Dogs: " + dogs);

                        String contact = (u.getContactDetails() == null || u.getContactDetails().isEmpty())
                                ? "—" : u.getContactDetails();
                        tvContact.setText("Contact: " + contact);

                        String n = u.getUserName();
                        String initial = !TextUtils.isEmpty(n) ? n.substring(0,1).toUpperCase() : "?";
                        tvAvatar.setText(initial);
                    } else {
                        String n = marker.getTitle();
                        String initial = !TextUtils.isEmpty(n) ? n.substring(0,1).toUpperCase() : "?";
                        tvAvatar.setText(initial);

                        userRepo.getUserById(tag.id, new UserRepository.FirestoreUserCallback() {
                            @Override public void onSuccess(User user) {
                                userCache.put(tag.id, user);
                                if (marker.isInfoWindowShown()) marker.showInfoWindow(); // רענון התצוגה
                            }
                            @Override public void onFailure(Exception e) { /* no-op */ }
                        });
                    }
                    return v;
                }
            });

            // לחיצה על החלון – מעבר לפרופיל של אותו משתמש
            googleMap.setOnInfoWindowClickListener(marker -> {
                Object t = marker.getTag();
                if (t instanceof MarkerTag && ((MarkerTag) t).kind == MarkerKind.USER) {
                    String userId = ((MarkerTag) t).id;
                    Intent i = new Intent(context, OtherUserProfileActivity.class);
                    i.putExtra(OtherUserProfileActivity.EXTRA_OTHER_USER_ID, userId);          // תומך בפרופיל של משתמש אחר
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                }
            });


            // אם המתנו למיקוד – נבצע עכשיו
            if (pendingFocusCenter != null && pendingFocusRadius != null) {
                focusOnArea(pendingFocusCenter.latitude, pendingFocusCenter.longitude, pendingFocusRadius);
                pendingFocusCenter = null;
                pendingFocusRadius = null;
            }

            // שלב 1: שליפת פרטי המשתמש הנוכחי
            userRepo.getUserById(currentUserId, new UserRepository.FirestoreUserCallback() {
                @Override
                public void onSuccess(User user) {
                    currentUser = user;

                    // שלב 2: קבלת מיקום נוכחי
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        locationClient.getLastLocation()
                                .addOnSuccessListener(location -> {
                                    if (location != null) {
                                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f));
                                        Log.d("MapController", "User location: " + userLatLng);

                                        // שמירת מיקום
                                        mapRepo.updateUserLocation(currentUserId, userLatLng.latitude, userLatLng.longitude);

                                        // שליפת מיקומי המשתמשים בקהילה
                                        loadCommunityMembersLocations(user.getCommunityName());

                                        // טעינת דיווחים קיימים במפה
                                        loadMapReports(user.getCommunityName());

                                        googleMap.setOnMarkerClickListener(marker -> {
                                            // בדיקה האם זה מרקר של דיווח מפה
                                            String reportId = null;
                                            for (Map.Entry<String, Marker> entry : mapReportMarkers.entrySet()) {
                                                if (entry.getValue().equals(marker)) {
                                                    reportId = entry.getKey();
                                                    break;
                                                }
                                            }
                                            // אם נמצא דיווח ואם המשתמש הוא מנהל – הצג דיאלוג מחיקה
                                            if (reportId != null && currentUser != null && currentUser.isManager()) {
                                                final String reportIdFinal = reportId;
                                                new AlertDialog.Builder(context)
                                                        .setTitle("Delete Report")
                                                        .setMessage("Do you want to delete this map report?")
                                                        .setPositiveButton("Delete", (dialog, which) -> {
                                                            mapRepo.deleteMapReport(currentUser.getCommunityName(), reportIdFinal,
                                                                    new MapRepository.FirestoreCallback() {
                                                                        @Override public void onSuccess(String id) {
                                                                            Toast.makeText(context, "Report deleted", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                        @Override public void onFailure(Exception e) {
                                                                            Toast.makeText(context, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                                        }
                                                                    });
                                                        })
                                                        .setNegativeButton("Cancel", null)
                                                        .show();
                                            }
                                            // החזר false אם תרצה שהקליק יציג גם את חלון המידע (snippet). החזר true כדי לבלוע את האירוע.
                                            return false;
                                        });

                                    } else {
                                        Log.w("MapController", "Location is null. Falling back to Tel Aviv.");
                                        moveToTelAviv();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("MapController", "Failed to get location: " + e.getMessage());
                                    moveToTelAviv();
                                });
                    }, 500);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("MapController", "Failed to get user data: " + e.getMessage());
                    moveToTelAviv();
                }
            });
        });
    }

    // שליפת מיקומי משתמשים מהקהילה עם שמות והצגתם במפה
    private void loadCommunityMembersLocations(String communityName) {
        mapRepo.listenToCommunityLocations(communityName, currentUserId, userRepo,
                new MapRepository.FirestoreLiveLocationCallback() {
                    @Override
                    public void onUserLocationUpdated(String userId, String userName, LatLng position) {
                        if (userMarkers.containsKey(userId)) {
                            userMarkers.get(userId).setPosition(position);
                        } else {
                            Marker marker = googleMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(userName)
                                    .snippet("Tap to view profile"));

                            if (marker != null) {
                                marker.setTag(new MarkerTag(MarkerKind.USER, userId));
                                userMarkers.put(userId, marker);
                            }

                            // משוך פעם אחת את כל פרטי המשתמש לקאש (ל־InfoWindow)
                            if (!userCache.containsKey(userId)) {
                                userRepo.getUserById(userId, new UserRepository.FirestoreUserCallback() {
                                    @Override public void onSuccess(User user) { userCache.put(userId, user); }
                                    @Override public void onFailure(Exception e) { /* no-op */ }
                                });
                            }
                        }
                    }

                    @Override
                    public void onUserRemoved(String userId) {
                        if (userMarkers.containsKey(userId)) {
                            userMarkers.get(userId).remove();
                            userMarkers.remove(userId);
                            userCache.remove(userId);
                        }
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void startRealTimeLocationUpdates() {
        // הגדרת הבקשה לעדכוני מיקום (מינימום כל 5 שניות)
        this.locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, // דיוק גבוה
                5000 // מרווח בקשות: 5 שניות
        )
                .setMinUpdateIntervalMillis(3000) // זמן מינימלי בין עדכונים: 3 שניות
                .build();

        // הגדרת callback לעדכוני מיקום
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    Log.d("MapController", "Realtime user location: " + lat + ", " + lng);

                    // שמירת מיקום ב־Firestore
                    if (isVisibleToOthers) {
                        mapRepo.updateUserLocation(currentUserId, lat, lng);
                    }
                }
            }
        };

        // בדיקה שיש הרשאות, ואז התחלת בקשת העדכונים
        if (hasLocationPermission()) {
            locationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        }
    }

    private void stopRealTimeLocationUpdates() {
        if (locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    private void moveToTelAviv() {
        if (isMapReady && googleMap != null) {
            LatLng telAviv = new LatLng(32.0853, 34.7818);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(telAviv, 13f));
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void setVisibleToOthers(boolean visible) {
        this.isVisibleToOthers = visible;
    }

    // ב־initializeMap נטען גם את הדיווחים הקיימים
    private void loadMapReports(String communityName) {
        mapRepo.listenToMapReports(communityName, new MapRepository.MapReportsListener() {
            @Override
            public void onReportAdded(String id, MapReport report) {
                LatLng pos = new LatLng(report.getLatitude(), report.getLongitude());
                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(report.getType())
                        .snippet("Reported by: " + report.getSenderName())
                        .icon(getMarkerIcon(report.getType())));
                if (marker != null) marker.setTag(new MarkerTag(MarkerKind.REPORT, id));
                mapReportMarkers.put(id, marker);
            }
            @Override public void onReportRemoved(String id) {
                Marker m = mapReportMarkers.remove(id);
                if (m != null) m.remove();
            }
        });
    }

    // להיכנס למצב בחירת נקודה על המפה
    public void enterReportMode(String type) {
        this.reportMode = true;
        this.reportType = type;
        Toast.makeText(context, "Tap on the map to place a \"" + type + "\" report", Toast.LENGTH_LONG).show();
        googleMap.setOnMapClickListener(latLng -> {
            if (!reportMode) return;
            // יצירת דיווח
            MapReport newReport = new MapReport(reportType, currentUser.getUserName(),
                    latLng.latitude, latLng.longitude, System.currentTimeMillis());
            mapRepo.createMapReport(currentUser.getCommunityName(), newReport, new MapRepository.FirestoreCallback() {
                @Override public void onSuccess(String docId) {
                    // אפשר להראות Toast או להוסיף את הסמן כאן
                    reportMode = false;
                    googleMap.setOnMapClickListener(null);
                }
                @Override public void onFailure(Exception e) {
                    reportMode = false;
                    googleMap.setOnMapClickListener(null);
                    Toast.makeText(context, "Failed to save report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private BitmapDescriptor getMarkerIcon(String type) {
        switch (type) {
            case "Dog Patrol":      // פקח כלבים
                return bitmapDescriptorFromVector(context, R.drawable.ic_inspector);
            case "Trash Bin":       // פח אשפה
                return bitmapDescriptorFromVector(context, R.drawable.trash_bin);
            case "Danger":          // סכנה
                return bitmapDescriptorFromVector(context, R.drawable.ic_danger);
            case "Help":            // בקשת סיוע
                return bitmapDescriptorFromVector(context, R.drawable.ic_help);
            default:
                // צבע סגול כברירת מחדל
                return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);
        }
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context ctx, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(ctx, vectorResId);
        if (vectorDrawable == null) {
            // fallback במקרה שהאייקון לא נמצא
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);
        }
        // ציור הוקטור לביטמפ
        int width = vectorDrawable.getIntrinsicWidth();
        int height = vectorDrawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);

        // שינוי קנה המידה
        float scale = 0.05f;
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                Math.round(width * scale),
                Math.round(height * scale),
                true);

        return BitmapDescriptorFactory.fromBitmap(scaledBitmap);
    }

    public void focusOnArea(double lat, double lng, int radiusMeters) {
        LatLng center = new LatLng(lat, lng);
        if (!isMapReady || googleMap == null) {
            // נשמור לפעולה כשמפה תהיה מוכנה
            pendingFocusCenter = center;
            pendingFocusRadius = radiusMeters;
            return;
        }
        // הזזת מצלמה
        float zoom = zoomForRadius(radiusMeters);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, zoom));

        // ציור/רענון עיגול
        if (focusCircle != null) focusCircle.remove();
        focusCircle = googleMap.addCircle(new CircleOptions()
                .center(center)
                .radius(radiusMeters)
                .strokeWidth(2f)
                .strokeColor(0xFF6C63FF)   // קו סגול
                .fillColor(0x336C63FF));   // מילוי שקוף (כ~20%)
    }

    private float zoomForRadius(int radiusMeters) {
        double scale = radiusMeters / 500.0; // בערך 500 מ' למסך בזום 15
        float zoom = (float)(15 - (Math.log(scale) / Math.log(2)));
        if (zoom < 2f) zoom = 2f;
        if (zoom > 21f) zoom = 21f;
        return zoom;
    }

    // Lifecycle methods
    public void onResume() { mapView.onResume(); }

    public void onPause() {
        if (mapView != null) mapView.onPause();
        stopRealTimeLocationUpdates(); // הוספה
    }

    public void onDestroy() {
        if (mapView != null) mapView.onDestroy();
        mapRepo.removeLiveLocationListener();
        mapRepo.removeMapReportsListener();

        if (locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
    }

    public void onLowMemory() { mapView.onLowMemory(); }
    public void onSaveInstanceState(Bundle outState) {
        mapView.onSaveInstanceState(outState);
    }
}