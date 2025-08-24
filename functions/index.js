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

      // נביא את שם הקהילה מתוך המסמך של הקהילה (או נשתמש ב-id אם אין שדה name)
      let communityName = data.communityName; // אופציונלי אם תתחילו לשמור גם בהודעה
      if (!communityName) {
        const commDoc = await admin.firestore().collection("communities").doc(chatId).get();
        communityName = (commDoc.exists && commDoc.get("name")) || chatId;
      }

      // כל המשתמשים בקהילה הזו
      const usersSnap = await admin
          .firestore()
          .collection("users")
          .where("communityName", "==", communityName)
          .get();

      const recipientUids = usersSnap.docs
          .map((d) => d.id)
          .filter((uid) => uid && uid !== senderId);

      if (recipientUids.length === 0) return;

      // שליפת כל ה־tokens של הנמענים
      const tokenSnaps = await Promise.all(
          recipientUids.map((uid) =>
            admin.firestore().collection("users").doc(uid).collection("fcmTokens").get(),
          ),
      );

      const tokens = [];
      tokenSnaps.forEach((qs) => qs.forEach((doc) => tokens.push(doc.id)));
      if (tokens.length === 0) return;

      // הודעת דאטה (Heads-up יוצג אצלך באפליקציה)
      const baseMessage = {
        data: {type: "chat_message", chatId, messageId, senderId, senderName, text},
        android: {priority: "high"},
      };

      // שליחה במנות + ניקוי טוקנים מתים
      const chunk = 500;
      for (let i = 0; i < tokens.length; i += chunk) {
        const slice = tokens.slice(i, i + chunk);
        const res = await admin.messaging().sendEachForMulticast({tokens: slice, ...baseMessage});
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
                  .collection("users").doc(uid)
                  .collection("fcmTokens").doc(dead)
                  .delete()
                  .catch(() => {});
            });
          }
        });
      }
    },
);
