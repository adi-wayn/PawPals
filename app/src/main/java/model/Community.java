package model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Community implements Parcelable {
    private String name;
    private CommunityManager Manager;
    public ArrayList<User> Members;
    public ArrayList<Report> Reports;

    public Community(String name) {
        this.name = name;
        this.Members = new ArrayList<>();
        this.Reports = new ArrayList<>();
    }

    public Community(String name, CommunityManager manager) {
        this.name = name;
        this.Manager = manager;
        this.Members = new ArrayList<>();
        this.Reports = new ArrayList<>();
    }

    protected Community(Parcel in) {
        name = in.readString();
        Manager = in.readParcelable(CommunityManager.class.getClassLoader());
        Members = in.createTypedArrayList(User.CREATOR);
        Reports = in.createTypedArrayList(Report.CREATOR);
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
        return Manager;
    }

    public void setManager(CommunityManager manager) {
        this.Manager = manager;
    }

    public ArrayList<Report> getReports() {
        return Reports;
    }

    public ArrayList<Report> getReportByType(String s) {
        ArrayList<Report> reportByType = new ArrayList<>();
        for (Report temp : Reports) {
            if (temp.type.equals(s)) {
                reportByType.add(temp);
            }
        }
        return reportByType;
    }

    @Override
    public String toString() {
        return name;
    }

    public void addReport(Report r) {
        this.Reports.add(r);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeParcelable(Manager, flags);
        dest.writeTypedList(Members);
        dest.writeTypedList(Reports);
    }
}
