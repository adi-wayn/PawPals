/* eslint-env node */
"use strict";

const admin = require("firebase-admin");
const {setGlobalOptions} = require("firebase-functions/v2");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");

admin.initializeApp();
setGlobalOptions({region: "europe-west1"});

exports.onNewMessage = onDocumentCreated(
    "communities/{chatId}/messages/{messageId}",
    async (event) => {
      const snap = event.data;
      if (!snap) return;

      const data = snap.data() || {};
      const {chatId, messageId} = event.params;

      const senderId = data.senderId || "";
      const senderName = data.senderName || "Someone";
      const text = data.text || "";

      // שם הקהילה (אם לא נשמר בהודעה)
      let communityName = data.communityName;
      if (!communityName) {
        const commDoc = await admin
            .firestore()
            .collection("communities")
            .doc(chatId)
            .get();
        communityName = (commDoc.exists && commDoc.get("name")) || chatId;
      }

      // כל המשתמשים בקהילה, חוץ מהשולח
      const usersSnap = await admin
          .firestore()
          .collection("users")
          .where("communityName", "==", communityName)
          .get();

      const recipientUids = usersSnap.docs
          .map((d) => d.id)
          .filter((uid) => uid && uid !== senderId);

      if (!recipientUids.length) return;

      // שליפת טוקנים של נמענים
      const tokenSnaps = await Promise.all(
          recipientUids.map((uid) =>
            admin
                .firestore()
                .collection("users")
                .doc(uid)
                .collection("fcmTokens")
                .get(),
          ),
      );
      let tokens = [];
      tokenSnaps.forEach((qs) => qs.forEach((doc) => tokens.push(doc.id)));

      // שליפת טוקנים של השולח (למקרה שטוקן שלו מקושר גם לאחרים)
      const senderTokens = [];
      if (senderId) {
        const sSnap = await admin
            .firestore()
            .collection("users")
            .doc(senderId)
            .collection("fcmTokens")
            .get();
        sSnap.forEach((d) => senderTokens.push(d.id));
      }

      // סינון סופי: לא שולחים אף טוקן של השולח
      tokens = tokens.filter((t) => !senderTokens.includes(t));
      if (!tokens.length) return;

      const baseMessage = {
        data: {type: "chat_message", chatId, messageId, senderId, senderName, text},
        android: {priority: "high"},
      };

      const chunk = 500;
      for (let i = 0; i < tokens.length; i += chunk) {
        const slice = tokens.slice(i, i + chunk);
        const res = await admin
            .messaging()
            .sendEachForMulticast({tokens: slice, ...baseMessage});

        res.responses.forEach((r, idx) => {
          if (
            !r.success &&
          r.error &&
          r.error.code === "messaging/registration-token-not-registered"
          ) {
            const dead = slice[idx];
            recipientUids.forEach((uid) => {
              admin
                  .firestore()
                  .collection("users")
                  .doc(uid)
                  .collection("fcmTokens")
                  .doc(dead)
                  .delete()
                  .catch(() => {});
            });
          }
        });
      }
    },
);

// === פוש על פוסט חדש ב-feed — בלי senderUid ===
exports.onNewFeedPost = onDocumentCreated(
    "communities/{communityId}/feed/{postId}",
    async (event) => {
      const snap = event.data;
      if (!snap) return;

      const data = snap.data() || {};
      const {communityId, postId} = event.params;

      // אצלך ב-Report השדה הוא "sender name" (עם רווח); משאירים fallback ל-senderName אם קיים
      const senderName = data["sender name"] || data.senderName || "Someone";
      const subject = data.subject || "New post";
      const text = data.text || "";

      // 1) כל המשתמשים בקהילה
      const usersSnap = await admin
          .firestore()
          .collection("users")
          .where("communityName", "==", communityId) // אצלך ה-id של הקהילה הוא השם שלה
          .get();

      const allUids = usersSnap.docs.map((d) => d.id);

      // 2) מציאת ה-Uids של השולח לפי שם (ייתכן יותר מאחד אם יש כפל שמות)
      let excludeUids = [];
      if (senderName) {
        const authorSnap = await admin
            .firestore()
            .collection("users")
            .where("communityName", "==", communityId)
            .where("userName", "==", senderName) // התאמה לפי שם המשתמש
            .get();
        excludeUids = authorSnap.docs.map((d) => d.id);
      }

      // נמענים = כל חברי הקהילה למעט מי ששמם תואם לשולח
      const recipientUids = allUids.filter((uid) => !excludeUids.includes(uid));
      if (!recipientUids.length) return;

      // 3) אוספים טוקנים מכל הנמענים
      const tokenSnaps = await Promise.all(
          recipientUids.map((uid) =>
            admin
                .firestore()
                .collection("users")
                .doc(uid)
                .collection("fcmTokens")
                .get(),
          ),
      );
      const tokens = [];
      tokenSnaps.forEach((qs) => qs.forEach((doc) => tokens.push(doc.id)));
      if (!tokens.length) return;

      // 4) הודעת דאטה – תוצג בצד האנדרואיד ב-MyFirebaseMessagingService
      const baseMessage = {
        data: {
          type: "feed_post",
          communityId,
          postId,
          senderName,
          subject,
          text,
        },
        android: {priority: "high"},
      };

      // 5) שליחה במנות + ניקוי טוקנים מתים
      const chunk = 500;
      for (let i = 0; i < tokens.length; i += chunk) {
        const slice = tokens.slice(i, i + chunk);
        const res = await admin
            .messaging()
            .sendEachForMulticast({tokens: slice, ...baseMessage});

        res.responses.forEach((r, idx) => {
          if (
            !r.success &&
          r.error &&
          r.error.code === "messaging/registration-token-not-registered"
          ) {
            const dead = slice[idx];
            recipientUids.forEach((uid) => {
              admin
                  .firestore()
                  .collection("users")
                  .doc(uid)
                  .collection("fcmTokens")
                  .doc(dead)
                  .delete()
                  .catch(() => {});
            });
          }
        });
      }
    },
);
