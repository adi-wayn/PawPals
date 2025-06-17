package model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Community implements Parcelable {
    private String name;
    private CommunityManager manager;
    private ArrayList<User> members;
    private ArrayList<Report> reports;

    public Community(String name) {
        this.name = name;
        this.members = new ArrayList<>();
        this.reports = new ArrayList<>();
    }

    public Community(String name, CommunityManager manager) {
        this.name = name;
        this.manager = manager;
        this.members = new ArrayList<>();
        this.reports = new ArrayList<>();
    }

    protected Community(Parcel in) {
        name = in.readString();
        manager = in.readParcelable(CommunityManager.class.getClassLoader());
        members = in.createTypedArrayList(User.CREATOR);
        reports = in.createTypedArrayList(Report.CREATOR);
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
        return name;
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

    public void setReports(ArrayList<Report> reports) {
        this.reports = reports;
    }

    public ArrayList<Report> getReportByType(String type) {
        ArrayList<Report> result = new ArrayList<>();
        for (Report r : reports) {
            if (r.type.equals(type)) {
                result.add(r);
            }
        }
        return result;
    }

    public void addReport(Report r) {
        this.reports.add(r);
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
    }
}
