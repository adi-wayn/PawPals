package model.firebase.CloudMessaging;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // שליפת communityId אם קיים בשדות ה-data של ההודעה
        String communityId = null;
        if (remoteMessage.getData() != null && remoteMessage.getData().containsKey("communityId")) {
            communityId = remoteMessage.getData().get("communityId");
        }

        // קריאה לגרסה החדשה של NotificationHelper
        NotificationHelper.showNotification(getApplicationContext(), remoteMessage, communityId);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "Refreshed token: " + token);
        // כאן אפשר לשמור את הטוקן ב־Firestore תחת המשתמש הנוכחי
    }
}
