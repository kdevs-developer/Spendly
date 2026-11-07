// Sends a push notification to every other member when a chat message is created.
// Region asia-south1 (Mumbai) to stay close to Indian users.
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const functionsV1 = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

exports.onMessage = onDocumentCreated(
  { document: "chats/{chatId}/messages/{msgId}", region: "asia-south1" },
  async (event) => {
    const msg = event.data && event.data.data();
    if (!msg || msg.type === "system") return;
    const db = admin.firestore();
    const chatId = event.params.chatId;
    const chat = (await db.doc(`chats/${chatId}`).get()).data();
    if (!chat) return;

    const title = chat.type === "direct" ? msg.senderName : `${chat.name} · ${msg.senderName}`;
    const body =
      msg.type === "image" ? "Photo" : msg.type === "video" ? "Video" : (msg.text || "").slice(0, 100);

    const targets = (chat.members || []).filter((u) => u !== msg.senderId);
    await Promise.all(
      targets.map(async (uid) => {
        const ref = db.doc(`users/${uid}`);
        const me = (await ref.get()).data() || {};
        if ((me.blocked || []).includes(msg.senderId)) return;
        if (!me.fcmToken) {
          console.warn(`No fcmToken for ${uid}; open the app once while signed in so it can save one.`);
          return;
        }
        try {
          // Data-only + high priority: the app's SpendlyMessagingService always runs, even when the app is closed,
          // so it can show the notification AND mark the message delivered. (A "notification" block would make
          // Android show it by itself and skip that code.)
          await admin.messaging().send({
            token: me.fcmToken,
            data: { chatId, title: String(title || ""), body: String(body || "") },
            android: { priority: "high", ttl: 24 * 60 * 60 * 1000 },
          });
        } catch (e) {
          console.error(`Push to ${uid} failed:`, e.code, e.message);
          // A dead token (app reinstalled / signed out elsewhere) is removed so it is not retried forever.
          if (e.code === "messaging/registration-token-not-registered" || e.code === "messaging/invalid-registration-token") {
            await ref.update({ fcmToken: admin.firestore.FieldValue.delete() }).catch(() => {});
          }
        }
      })
    );
  }
);

// Optional clean-up that only the server can do (needs the Blaze plan). The app already removes the person's own
// data before deleting the login; this sweeps what the rules stop a phone from deleting: chats left with no
// members (and their messages), and any photos or lookup rows that were missed.
exports.cleanupDeletedUser = functionsV1.region("asia-south1").auth.user().onDelete(async (user) => {
  const db = admin.firestore();
  const uid = user.uid;
  await db.recursiveDelete(db.doc(`users/${uid}`));
  const codes = await db.collection("userCodes").where("uid", "==", uid).get();
  await Promise.all(codes.docs.map((d) => d.ref.delete()));
  for (const side of ["from", "to"]) {
    const q = await db.collection("requests").where(side, "==", uid).get();
    await Promise.all(q.docs.map((d) => d.ref.delete()));
  }
  const still = await db.collection("chats").where("members", "array-contains", uid).get();
  for (const c of still.docs) {
    const rest = (c.data().members || []).filter((m) => m !== uid);
    if (rest.length === 0) await db.recursiveDelete(c.ref);
    else await c.ref.update({ members: rest, [`memberNames.${uid}`]: "Deleted user" });
  }
  const empty = await db.collection("chats").where("members", "==", []).get();
  for (const c of empty.docs) await db.recursiveDelete(c.ref);
  const [files] = await admin.storage().bucket().getFiles({ prefix: "chats/" });
  await Promise.all(files.filter((f) => f.name.endsWith(`_${uid}.jpg`)).map((f) => f.delete().catch(() => {})));
});
