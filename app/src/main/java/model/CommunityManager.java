package model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.Map;

@IgnoreExtraProperties
public class CommunityManager extends User implements Parcelable {

    // נדרש לפיירבייס
    public CommunityManager() {
        super();
        this.isManager = true;
    }

    public CommunityManager(String name, String community) {
        super(name, community);
        this.isManager = true;
    }

    // קונסטרקטור של Parcelable
    protected CommunityManager(Parcel in) {
        super(in);
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
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        // אם בעתיד תוסיף שדות – תעדכן גם פה
        return map;
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
