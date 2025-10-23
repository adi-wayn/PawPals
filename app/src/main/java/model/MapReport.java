package model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a temporary report that is attached to a geographic location.
 * This class is used for map-based reports such as hazards, inspectors or assistance requests
 * that appear temporarily on the map. It is intentionally separate from the feed Report model.
 */
public class MapReport implements Parcelable {
    private String type;
    private String senderName;
    private double latitude;
    private double longitude;
    private Timestamp timestamp;

    /**
     * Default constructor required for calls to DataSnapshot.getValue(MapReport.class)
     */
    public MapReport() {
        // no‑arg constructor for Firebase
    }

    /**
     * Create a new map report.
     *
     * @param type       The type of report (e.g. "פקח", "סכנה", "בקשת סיוע").
     * @param senderName Name of the reporting user. Can be empty for anonymous reports.
     * @param latitude   Latitude coordinate of the report location.
     * @param longitude  Longitude coordinate of the report location.
     * @param timestamp  Time of report creation in milliseconds since epoch.
     */
    public MapReport(String type, String senderName, double latitude, double longitude, Timestamp timestamp) {
        this.type = type;
        this.senderName = senderName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    // קונסטרקטור נוח לקבלת מילישניות והמרתן ל-Timestamp
    public MapReport(String type, String senderName, double latitude, double longitude, long timestampMillis) {
        this(type, senderName, latitude, longitude, new Timestamp(new Date(timestampMillis)));
    }
    protected MapReport(Parcel in) {
        type = in.readString();
        senderName = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        long tsMillis = in.readLong();
        this.timestamp = new Timestamp(new Date(tsMillis));
    }

    public static final Creator<MapReport> CREATOR = new Creator<MapReport>() {
        @Override
        public MapReport createFromParcel(Parcel in) {
            return new MapReport(in);
        }

        @Override
        public MapReport[] newArray(int size) {
            return new MapReport[size];
        }
    };

    public String getType() {
        return type;
    }

    public String getSenderName() {
        return senderName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Timestamp getTimestamp() { return timestamp; }

    /**
     * Serializes this report into a map structure for Firestore.
     *
     * @return Map with primitive values describing the report.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("senderName", senderName);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("timestamp", timestamp);
        return map;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(type);
        parcel.writeString(senderName);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
        parcel.writeLong(timestamp != null ? timestamp.toDate().getTime() : 0L);
    }
}