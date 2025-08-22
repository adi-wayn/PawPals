package model.firebase.CloudMessaging;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.pawpals.R;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

import model.MarkReadReceiver;
import model.ReplyReceiver;

public class NotificationHelper {

    public static final String CHANNEL_ID   = "default_channel";
    private static final String CHANNEL_NAME = "General Notifications";

    // מפתחות ל-RemoteInput/Extras
    public static final String KEY_TEXT_REPLY = "key_text_reply";
    public static final String EXTRA_CHAT_ID = "extra_chat_id";
    public static final String EXTRA_MESSAGE_ID = "extra_message_id";

    // FCM payload
    public static void showNotification(Context context, RemoteMessage remoteMessage) {
        createChannelIfNeeded(context);

        String title = null, body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body  = remoteMessage.getNotification().getBody();
        } else if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            title = remoteMessage.getData().get("title");
            body  = remoteMessage.getData().get("body");
        }

        if (title == null) title = "New message";
        if (body  == null) body  = "";
        showInternalNotification(context, title, body);
    }

    // local use
    public static void showSimpleNotification(Context context, String title, String body) {
        createChannelIfNeeded(context);
        if (title == null) title = "New message";
        if (body  == null) body  = "";
        showInternalNotification(context, title, body);
    }

    private static void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // heads-up
            );
            // אופציונלי: קול/ויברציה (Heads-up נוטה להופיע יותר עקבי)
            ch.enableVibration(true);
            ch.setShowBadge(true);

            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private static void showInternalNotification(Context context, String title, String body) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)     // ל-pre-O
                .setDefaults(NotificationCompat.DEFAULT_ALL)       // קול/ויברציה/אור
                .setAutoCancel(true);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        int notificationId = new Random().nextInt(Integer.MAX_VALUE); // חיובי

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                nm.notify(notificationId, b.build());
            }
        } else {
            nm.notify(notificationId, b.build());
        }
    }

    // Group למיון התראות לפי צ'אט
    private static String groupKeyForChat(String chatId) {
        return "group_chat_" + chatId;
    }

    // ===== הודעת צ'אט “כמו וואטסאפ” =====
    public static void showChatMessage(Context ctx,
                                       String chatId,
                                       String messageId,
                                       String senderId,
                                       String senderName,
                                       String text,
                                       @Nullable String avatarUrl) {
        createChannelIfNeeded(ctx);

        // PendingIntent לפתיחת ה-ChatActivity הספציפי
        Intent openIntent = new Intent(ctx, com.example.pawpals.ChatActivity.class);
        openIntent.putExtra(EXTRA_CHAT_ID, chatId);
        // FLAG_IMMUTABLE מספיק כאן (אין RemoteInput לאינטנט הזה)
        PendingIntent contentPi = PendingIntent.getActivity(
                ctx, chatId.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Reply אינליין (BroadcastReceiver)
        Intent replyIntent = new Intent(ctx, ReplyReceiver.class)
                .setAction("com.example.pawpals.ACTION_REPLY")
                .putExtra(EXTRA_CHAT_ID, chatId)
                .putExtra(EXTRA_MESSAGE_ID, messageId);
        // MUST: MUTABLE כדי ש-RemoteInput יעבוד
        PendingIntent replyPi = PendingIntent.getBroadcast(
                ctx, ("reply_"+chatId).hashCode(),
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel("Reply")
                .build();
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_notification, "Reply", replyPi)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .build();

        // Mark as read
        Intent readIntent = new Intent(ctx, MarkReadReceiver.class)
                .setAction("com.example.pawpals.ACTION_MARK_READ")
                .putExtra(EXTRA_CHAT_ID, chatId)
                .putExtra(EXTRA_MESSAGE_ID, messageId);
        PendingIntent readPi = PendingIntent.getBroadcast(
                ctx, ("read_"+chatId).hashCode(),
                readIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Action readAction =
                new NotificationCompat.Action.Builder(R.drawable.ic_notification, "Mark as read", readPi).build();

        // Large icon (אופציונלי) – אפשר להשמיט אם אין צורך
        Bitmap large = null; // אפשר לטעון עם Glide ב-Worker, השמטתי לפשטות

        String title = (senderName != null && !senderName.isEmpty()) ? senderName : "New message";
        String body  = (text != null) ? text : "";

        // קיבוץ לפי צ'אט ו-ID יציב לכל צ'אט
        String groupKey = groupKeyForChat(chatId);
        int notificationId = (chatId.hashCode() & 0x7fffffff);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)      // ל-pre O
                .setDefaults(NotificationCompat.DEFAULT_ALL)        // קול/ויברציה/אור
                .setAutoCancel(true)
                .setContentIntent(contentPi)
                .addAction(replyAction)
                .addAction(readAction)
                .setGroup(groupKey)
                .setOnlyAlertOnce(false);

        if (large != null) b.setLargeIcon(large);

        NotificationManagerCompat nm = NotificationManagerCompat.from(ctx);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
            // התראת הודעה
            nm.notify(notificationId, b.build());

            // סיכום קבוצה (כדי לערום הודעות)
            NotificationCompat.Builder summary = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("New messages")
                    .setContentText("New messages in chat")
                    .setStyle(new NotificationCompat.InboxStyle().addLine(title + ": " + body))
                    .setGroup(groupKey)
                    .setGroupSummary(true)
                    .setAutoCancel(true);
            nm.notify(("summary_"+chatId).hashCode(), summary.build());
        }
    }
}