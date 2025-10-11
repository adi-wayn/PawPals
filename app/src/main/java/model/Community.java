package model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Community implements Parcelable {
    private String name;
    private CommunityManager manager;
    private ArrayList<User> members;
    private ArrayList<Report> reports;
    private ArrayList<Message> messages;
    private double latitude;
    private double longitude;


    public Community(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.messages = new ArrayList<>();
        this.members = new ArrayList<>();
        this.reports = new ArrayList<>();
    }

    public Community(String name, double latitude, double longitude, CommunityManager manager) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.manager = manager;
        this.messages = new ArrayList<>();
        this.members = new ArrayList<>();
        this.reports = new ArrayList<>();
    }

    protected Community(Parcel in) {
        name = in.readString();
        manager = in.readParcelable(CommunityManager.class.getClassLoader());
        members = in.createTypedArrayList(User.CREATOR);
        messages = in.createTypedArrayList(Message.CREATOR);
        reports = in.createTypedArrayList(Report.CREATOR);
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    public static final Creator<Community> CREATOR = new Creator<Community>() {
        @Override
        public Community createFromParcel(Parcel in) {
            return new Community(in);
        }

        @Override
        public Community[] newArray(int size) {
            return new Community[size];
        }
    };

    public String getName() {
        return name != null ? name : "";
    }

    public CommunityManager getManager() {
        return manager;
    }

    public void setManager(CommunityManager manager) {
        this.manager = manager;
    }

    public ArrayList<User> getMembers() {
        return members;
    }

    public void setMembers(ArrayList<User> members) {
        this.members = members;
    }

    public ArrayList<Report> getReports() {
        return reports;
    }
    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setReports(ArrayList<Report> reports) {
        this.reports = reports;
    }

    public ArrayList<Report> getReportByType(String type) {
        ArrayList<Report> result = new ArrayList<>();
        for (Report r : reports) {
            if (r.getType().equals(type)) {
                result.add(r);
            }
        }
        return result;
    }

    public void addReport(Report r) {
        this.reports.add(r);
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeParcelable(manager, flags);
        dest.writeTypedList(members);
        dest.writeTypedList(reports);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }

    public void setName(String id) {
    }

    public Community() {
        // Needed for Firestore
        this.messages = new ArrayList<>();
        this.members = new ArrayList<>();
        this.reports = new ArrayList<>();
    }

}
