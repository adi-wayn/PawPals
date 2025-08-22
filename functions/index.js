/* eslint-env node */
"use strict";

const admin = require("firebase-admin");
const {setGlobalOptions} = require("firebase-functions/v2");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");

admin.initializeApp();
setGlobalOptions({region: "europe-west1"}); // אפשר לשנות אזור

exports.onNewMessage = onDocumentCreated(
    "communities/{chatId}/messages/{messageId}",
    async (event) => {
    // בלי optional chaining כדי שלא ייפול בלינט
      const snap = event.data;
      const data = snap ? snap.data() : {};
      const chatId = event.params.chatId;
      const messageId = event.params.messageId;

      const senderId = data.senderId;
      const senderName = data.senderName || "Someone";
      const text = data.text || "";

      // 1) שליפת חברי הצ'אט (התאם למסד שלך)
      const membersSnap = await admin
          .firestore()
          .collection("communities")
          .doc(chatId)
          .collection("members")
          .get();

      const recipientUids = membersSnap.docs
          .map((d) => d.id)
          .filter((uid) => uid !== senderId);

      if (recipientUids.length === 0) return;

      // 2) איסוף טוקנים
      const tokens = [];
      for (const uid of recipientUids) {
        const tokSnap = await admin
            .firestore()
            .collection("users")
            .doc(uid)
            .collection("fcmTokens")
            .get();
        tokSnap.forEach((doc) => tokens.push(doc.id));
      }
      if (tokens.length === 0) return;

      // 3) הודעת דאטה לאנדרואיד (Heads-up באפליקציה שלך)
      const baseMessage = {
        data: {
          type: "chat_message",
          chatId,
          messageId,
          senderId,
          senderName,
          text,
        },
        android: {priority: "high"},
      };

      // 4) שליחה במנות וניקוי טוקנים מתים
      const chunk = 500;
      for (let i = 0; i < tokens.length; i += chunk) {
        const res = await admin.messaging().sendEachForMulticast({
          tokens: tokens.slice(i, i + chunk),
          ...baseMessage,
        });

        res.responses.forEach((r, idx) => {
          if (
            !r.success &&
          r.error &&
          r.error.code === "messaging/registration-token-not-registered"
          ) {
            const dead = tokens[i + idx];
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
