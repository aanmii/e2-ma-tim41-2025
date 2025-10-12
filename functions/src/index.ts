import * as admin from "firebase-admin";
import {onDocumentCreated} from "firebase-functions/v2/firestore";

// Inicijalizacija Firebase Admin SDK-a
admin.initializeApp();

/**
 * Aktivira se kada se novi dokument kreira u 'invitations' kolekciji.
 * Šalje Push notifikaciju primaocu pozivnice.
 */
export const sendAllianceInviteNotification = onDocumentCreated(
  "invitations/{invitationId}",
  async (event) => {
    const invitationData = event.data?.data();
    if (!invitationData) {
      console.log("No invitation data found.");
      return;
    }

    const receiverId = invitationData.receiverId;
    const allianceName = invitationData.allianceName;
    const senderUsername = invitationData.senderUsername;
    const invitationId = event.params.invitationId;

    if (!receiverId) {
      console.log("Receiver ID not specified.");
      return;
    }

    // 1. Dohvatanje FCM tokena primaoca
    const userDoc = await admin.firestore()
      .collection("users")
      .doc(receiverId)
      .get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (!fcmToken) {
      console.log(`FCM token not found for user: ${receiverId}`);
      return;
    }

    // 2. Kreiranje poruke za FCM
    const message = {
      token: fcmToken,
      notification: {
        title: "⚔️ Poziv u Savez",
        body: `${senderUsername} te poziva u savez "${allianceName}".`,
      },
      data: {
        type: "ALLIANCE_INVITE",
        referenceId: invitationId, // ID pozivnice
      },
    };

    // 3. Slanje notifikacije
    try {
      await admin.messaging().send(message);
      console.log(`Alliance invitation notification sent to: ${receiverId}`);
    } catch (error) {
      console.error("Error sending alliance invitation notification:", error);
    }
  }
);
