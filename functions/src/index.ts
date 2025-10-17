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

    const recepientId = invitationData.recepientId;
    const allianceName = invitationData.allianceName;
    const senderUsername = invitationData.senderUsername;
    const invitationId = event.params.invitationId;

    if (!recepientId) {
      console.log("Receiver ID not specified.");
      return;
    }

    // 1. Dohvatanje FCM tokena primaoca
    const userDoc = await admin.firestore()
      .collection("users")
      .doc(recepientId)
      .get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (!fcmToken) {
      console.log(`FCM token not found for user: ${recepientId}`);
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
      console.log(`Alliance invitation notification sent to: ${recepientId}`);
    } catch (error) {
      console.error("Error sending alliance invitation notification:", error);
    }
  }
);


export const sendChatNotification = onDocumentCreated(
  "alliances/{allianceId}/messages/{messageId}",
  async (event) => {
    const chatMessage = event.data?.data();
    if (!chatMessage) {
      console.log("No chat message data found.");
      return;
    }

    const allianceId = event.params.allianceId;
    const senderId = chatMessage.senderId;
    const senderUsername = chatMessage.senderUsername;
    const messageContent = chatMessage.content;

    // 1. Dohvatanje ID-jeva svih članova saveza i imena saveza
    const allianceDoc = await admin.firestore()
      .collection("alliances")
      .doc(allianceId)
      .get();

    // Pretpostavljam da su članovi pohranjeni kao mapa {userId: username}
    const membersMap = allianceDoc.data()?.members || {};
    const allianceName = allianceDoc.data()?.name || "Savez";

    const recipientIds = Object.keys(membersMap)
      .filter((id) => id !== senderId);

    if (recipientIds.length === 0) {
      console.log("No other recipients in the alliance.");
      return;
    }

    // 2. Dohvatanje FCM tokena za SVE primaoce
    const usersSnapshot = await admin.firestore()
      .collection("users")
      .where(admin.firestore.FieldPath.documentId(), "in", recipientIds)
      .get();

    const tokens: string[] = [];
    usersSnapshot.forEach((doc) => {
      const token = doc.data().fcmToken;
      if (token) {
        tokens.push(token);
      }
    });

    if (tokens.length === 0) {
      console.log("No FCM tokens found for recipients.");
      return;
    }

    // Priprema poruke
    const title = allianceName;
    const body = `${senderUsername}: ${messageContent.substring(0, 50)}${
      messageContent.length > 50 ? "..." : ""
    }`;

    const message = {
      notification: {
        title: title,
        body: body,
      },
      data: {
        type: "CHAT_MESSAGE",
        referenceId: allianceId, // ID Saveza
      },
    };

    // 3. Slanje notifikacije svima (multicast)
    try {
      await admin.messaging().sendEachForMulticast({tokens, ...message});
      console.log(
        `Notif sent to ${tokens.length} members of alliance: ${allianceId}`
      );
    } catch (error) {
      console.error("Error sending chat notification:", error);
    }
  }
);

