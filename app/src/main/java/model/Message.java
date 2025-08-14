package model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Message implements Parcelable {
    // מזהה ההודעה (אופציונלי - אפשר לשמור בו את document id אם תרצי)
    public @Nullable String id;

    // מזהה צאטים לפי קהילה - כל צאט שייך לקהילה
    public String chatId;
//ה ID של המשתמש ששלח את ההודעה
    public String senderId;
    //שם של המשתמש ששלח את ההודעה
    public String senderName;
    //מתי נשלחה ההודעה
    public String text;

    // זמן שרת (ימולא אוטומטית ע"י Firestore אם לא תשלחי ערך)
    @ServerTimestamp
    public @Nullable Date timestamp;

    public Message() {} // דרוש לפיירבייס

    public Message(String chatId,String senderId, String senderName, String text) {
        this.chatId=chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
    }

    public Message(@Nullable String id,
                   String chatId,
                   String senderId,
                   String senderName,
                   String text,
                   @Nullable Date timestamp) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public @Nullable String getId() { return id; }
    public String getChatId() { return chatId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getText() { return text; }
    public @Nullable Date getTimestamp() { return timestamp; }

    public void setId(@Nullable String id) { this.id = id; }
    public void setChatId( String chatId) { this.chatId = chatId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setText(String text) { this.text = text; }
    public void setTimestamp(@Nullable Date timestamp) { this.timestamp = timestamp; }

    // Firestore serialization
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (id != null)      map.put("id", id);
        map.put("chatId", chatId);
        map.put("senderId", senderId);
        map.put("senderName", senderName);   // בלי רווח בשם המפתח
        map.put("text", text);
        map.put("timestamp", timestamp);     // אם null, Firestore ימלא @ServerTimestamp בזמן כתיבה
        return map;
    }

    // אופציונלי: בנייה ממפה (אם קוראים ידנית ולא דרך toObject)
    public static Message fromMap(Map<String, Object> map) {
        Message m = new Message();
        m.id = (String) map.get("id");
        m.chatId = (String) map.get("chatId");
        m.senderId = (String) map.get("senderId");
        m.senderName = (String) map.get("senderName");
        m.text = (String) map.get("text");
        Object ts = map.get("timestamp");
        if (ts instanceof Date) m.timestamp = (Date) ts;
        return m;
    }

    // === Parcelable ===
    protected Message(Parcel in) {
        id = in.readString();
        chatId = in.readString();
        senderId = in.readString();
        senderName = in.readString();
        text = in.readString();
        long t = in.readLong();
        timestamp = (t == -1L) ? null : new Date(t);
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override public Message createFromParcel(Parcel in) { return new Message(in); }
        @Override public Message[] newArray(int size) { return new Message[size]; }
    };

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(chatId);
        dest.writeString(senderId);
        dest.writeString(senderName);
        dest.writeString(text);
        dest.writeLong(timestamp == null ? -1L : timestamp.getTime());
    }

    @Override public int describeContents() { return 0; }
}
