package model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;

public class CommunityManager extends User implements Parcelable {

    public CommunityManager(String name, Community community) {
        super(name, community);
        this.isManager = true;
    }

    // Constructor לקריאה מ־Parcel
    protected CommunityManager(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public static final Creator<CommunityManager> CREATOR = new Creator<CommunityManager>() {
        @Override
        public CommunityManager createFromParcel(Parcel in) {
            return new CommunityManager(in);
        }

        @Override
        public CommunityManager[] newArray(int size) {
            return new CommunityManager[size];
        }
    };

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        // אם תוסיף שדות נוספים, תעדכן גם פה
        return map;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
