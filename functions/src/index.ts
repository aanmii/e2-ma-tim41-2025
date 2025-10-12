import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

export const sendNotification = functions.firestore
  .document("notifications/{notificationId}")
  .onCreate(async (snapshot, context) => {
    const data = snapshot.data();
    if (!data) return;

    const receiverId = data.receiverId;

    // Dohvati FCM token korisnika
    const userDoc = await admin.firestore().collection("users").doc(receiverId).get();
    const fcmToken = userDoc.data()?.fcmToken;
    if (!fcmToken) return;

    const message = {
      token: fcmToken,
      notification: {
        title: "Novi poziv u savez",
        body: data.content
      },
      data: {
        type: data.type,
        referenceId: data.referenceId
      }
    };

    await admin.messaging().send(message);
  });
