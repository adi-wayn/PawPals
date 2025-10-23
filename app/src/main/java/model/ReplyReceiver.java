package model;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

import model.firebase.CloudMessaging.NotificationHelper;

public class ReplyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        CharSequence reply = RemoteInput.getResultsFromIntent(intent) != null
                ? RemoteInput.getResultsFromIntent(intent).getCharSequence(NotificationHelper.KEY_TEXT_REPLY)
                : null;

        String chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID);
        String messageId = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE_ID);

        // TODO: שלח את ה-reply לשרת/Firestore (chatId, reply.toString())
        // למשל: MessagesRepository.send(chatId, replyText);

        // נקה את ההתראה של אותו צ'אט
        int notifId = (chatId != null) ? (chatId.hashCode() & 0x7fffffff) : 0;
        NotificationManagerCompat.from(context).cancel(notifId);
    }
}
