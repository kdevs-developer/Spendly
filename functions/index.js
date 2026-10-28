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
