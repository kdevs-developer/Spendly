// Sends a push notification to every other member when a chat message is created.
// Region asia-south1 (Mumbai) to stay close to Indian users.
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
admin.initializeApp();

exports.onMessage = onDocumentCreated(
  { document: "chats/{chatId}/messages/{msgId}", region: "asia-south1" },
  async (event) => {
    const msg = event.data && event.data.data();
    if (!msg || msg.type === "system") return;
    const db = admin.firestore();
    const chat = (await db.doc(`chats/${event.params.chatId}`).get()).data();
    if (!chat) return;

    const title = chat.type === "direct" ? msg.senderName : `${chat.name} · ${msg.senderName}`;
    const body = msg.type === "image" ? "Photo" : (msg.text || "").slice(0, 100);

    const targets = chat.members.filter((u) => u !== msg.senderId);
    for (const uid of targets) {
      const me = (await db.doc(`users/${uid}`).get()).data() || {};
      if ((me.blocked || []).includes(msg.senderId) || !me.fcmToken) continue;
      await admin.messaging().send({
        token: me.fcmToken,
        notification: { title, body },
        data: { chatId: event.params.chatId },
        android: { priority: "high" },
      }).catch(() => {});
    }
  }
);
