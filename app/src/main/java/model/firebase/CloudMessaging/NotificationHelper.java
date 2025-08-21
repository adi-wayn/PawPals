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

    public static void showNotification(Context context, RemoteMessage remoteMessage, String communityId) {
        // Step 1: יצירת ערוץ התראות
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "default_channel",
                    "General Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Step 2: חילוץ title/body
        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            // במקרה רגיל (FCM Notification)
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            // במקרה של הודעת Data (כמו אצלנו)
            title = remoteMessage.getData().get("title");
            body  = remoteMessage.getData().get("body");
        }

        if (title == null) title = "New message";
        if (body == null) body = "";

        // Step 3: בניית התראה
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "default_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Step 4: הצגת התראה
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = new Random().nextInt();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, builder.build());
            }
        } else {
            notificationManager.notify(notificationId, builder.build());
        }
    }

}
