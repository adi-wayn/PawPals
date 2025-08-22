package model.firebase.CloudMessaging;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashMap;


public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage rm) {
        super.onMessageReceived(rm);

        if (rm.getData() != null && "chat_message".equals(rm.getData().get("type"))) {
            // פריסה לבטיחות
            String chatId     = rm.getData().get("chatId");
            String messageId  = rm.getData().get("messageId");
            String senderId   = rm.getData().get("senderId");
            String senderName = rm.getData().get("senderName");
            String text       = rm.getData().get("text");
            String avatarUrl  = rm.getData().get("avatarUrl"); // אופציונלי

            NotificationHelper.showChatMessage(
                    getApplicationContext(),
                    chatId, messageId, senderId, senderName, text, avatarUrl
            );
            return;
        }

        // אחרת – ההתנהגות הישנה שלך
        NotificationHelper.showNotification(getApplicationContext(), rm);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) {
            FirebaseFirestore.getInstance()
                    .collection("users").document(u.getUid())
                    .collection("fcmTokens").document(token)
                    .set(new HashMap<String, Object>() {{ put("active", true); put("platform","android"); put("ts", FieldValue.serverTimestamp()); }});
        }
    }
}