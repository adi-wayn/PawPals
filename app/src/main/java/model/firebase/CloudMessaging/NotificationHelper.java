package model.firebase.CloudMessaging;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.pawpals.R;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class NotificationHelper {

    public static final String CHANNEL_ID   = "default_channel";
    private static final String CHANNEL_NAME = "General Notifications";

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
}