package model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import model.firebase.CloudMessaging.NotificationHelper;

public class MarkReadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID);
        String messageId = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE_ID);

        // TODO: דווח לשרת/Firestore ש"הודעה נקראה" (chatId, messageId)

        int notifId = (chatId != null) ? (chatId.hashCode() & 0x7fffffff) : 0;
        NotificationManagerCompat.from(context).cancel(notifId);
    }
}