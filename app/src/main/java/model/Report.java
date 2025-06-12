package model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class Report implements Parcelable {
    public String type;
    public String senderName;
    public String subject;
    public String text;

    // Constructors
    public Report(String type, String senderName, String subject, String text) {
        this.type = type;
        this.senderName = senderName;
        this.subject = subject;
        this.text = text;
    }

    public Report() {
    }

    // Parcelable constructor
    protected Report(Parcel in) {
        type = in.readString();
        senderName = in.readString();
        subject = in.readString();
        text = in.readString();
    }

    public static final Creator<Report> CREATOR = new Creator<Report>() {
        @Override
        public Report createFromParcel(Parcel in) {
            return new Report(in);
        }

        @Override
        public Report[] newArray(int size) {
            return new Report[size];
        }
    };

    // Getters
    public String getType() {
        return type;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }

    // Firestore serialization
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", this.type);
        map.put("sender name", this.senderName);
        map.put("subject", this.subject);
        map.put("text", this.text);
        return map;
    }

    // Parcelable implementation
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(senderName);
        dest.writeString(subject);
        dest.writeString(text);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
