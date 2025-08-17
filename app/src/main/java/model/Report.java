package model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Report implements Parcelable {

    // Firestore field keys (שומרים על הסכימה הקיימת שלך)
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_SENDER_NAME = "sender name"; // עם רווח, כמו בקוד שלך
    public static final String FIELD_SUBJECT = "subject";
    public static final String FIELD_TEXT = "text";

    // Not stored in Firestore doc; set manually from DocumentSnapshot#getId()
    private String id;

    // Data fields
    private String type;
    private String senderName;
    private String subject;
    private String text;

    // Required empty constructor for Firestore
    public Report() {}

    public Report(String type, String senderName, String subject, String text) {
        this.type = type;
        this.senderName = senderName;
        this.subject = subject;
        this.text = text;
    }

    // Parcelable
    protected Report(Parcel in) {
        id = in.readString();
        type = in.readString();
        senderName = in.readString();
        subject = in.readString();
        text = in.readString();
    }

    public static final Creator<Report> CREATOR = new Creator<Report>() {
        @Override public Report createFromParcel(Parcel in) { return new Report(in); }
        @Override public Report[] newArray(int size) { return new Report[size]; }
    };

    // Getters / Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    // Firestore serialization (id לא נכנס)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (type != null)       map.put(FIELD_TYPE, type);
        if (senderName != null) map.put(FIELD_SENDER_NAME, senderName);
        if (subject != null)    map.put(FIELD_SUBJECT, subject);
        if (text != null)       map.put(FIELD_TEXT, text);
        return map;
    }

    // Parcelable
    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(type);
        dest.writeString(senderName);
        dest.writeString(subject);
        dest.writeString(text);
    }
    @Override public int describeContents() { return 0; }

    // Helpful for list operations/removals
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Report)) return false;
        Report r = (Report) o;
        return id != null && id.equals(r.id);
    }
    @Override public int hashCode() {
        return id != null ? id.hashCode() : Objects.hash(type, senderName, subject, text);
    }

    @Override public String toString() {
        return "Report{id='" + id + "', type='" + type + "', senderName='" + senderName + "', subject='" + subject + "'}";
    }
}