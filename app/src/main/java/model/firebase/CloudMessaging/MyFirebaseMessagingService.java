package model.firebase.CloudMessaging;

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            String type = remoteMessage.getData().get("type");

            //הודעת צ'אט מדאטה
            if ("chat_message".equals(type)) {
                String chatId     = remoteMessage.getData().get("chatId");
                String messageId  = remoteMessage.getData().get("messageId");
                String senderId   = remoteMessage.getData().get("senderId");
                String senderName = remoteMessage.getData().get("senderName");
                String text       = remoteMessage.getData().get("text");

                NotificationHelper.showChatMessage(
                        getApplicationContext(),
                        chatId, messageId, senderId, senderName, text, /* avatarUrl= */ null
                );
                return;
            }

            //הודעת פוסט מדאטה
            if ("feed_post".equals(type)) {
                String communityId = remoteMessage.getData().get("communityId");
                String postId      = remoteMessage.getData().get("postId");
                String senderName  = remoteMessage.getData().get("senderName");
                String subject     = remoteMessage.getData().get("subject");
                String text        = remoteMessage.getData().get("text");

                NotificationHelper.showFeedPost(
                        getApplicationContext(),
                        communityId, postId, senderName, subject, text
                );
                return;
            }
        }

        // fallback: התראה רגילה
        NotificationHelper.showNotification(getApplicationContext(), remoteMessage);
    }


    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        model.firebase.CloudMessaging.FcmTokenManager.registerCurrentToken();
    }
}