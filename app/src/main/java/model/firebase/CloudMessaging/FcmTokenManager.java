package model.firebase.CloudMessaging;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;

public class FcmTokenManager {

    public static void registerCurrentToken() {
        final FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.isEmpty()) return;
                    FirebaseFirestore.getInstance()
                            .collection("users").document(u.getUid())
                            .collection("fcmTokens").document(token)
                            .set(Collections.singletonMap("ts", FieldValue.serverTimestamp()));
                });
    }

    // אופציונלי: לניקוי טוקן ביציאה מהחשבון
    public static void removeCurrentTokenOnLogout() {
        final FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.isEmpty()) return;
                    FirebaseFirestore.getInstance()
                            .collection("users").document(u.getUid())
                            .collection("fcmTokens").document(token)
                            .delete();
                });
    }
}