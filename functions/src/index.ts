import * as admin from "firebase-admin";
import {onDocumentCreated} from "firebase-functions/v2/firestore";

admin.initializeApp();

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


    const userDoc = await admin.firestore()
      .collection("users")
      .doc(recepientId)
      .get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (!fcmToken) {
      console.log(`FCM token not found for user: ${recepientId}`);
      return;
    }

    const message = {
      token: fcmToken,
      notification: {
        title: "⚔️Alliance invite",
        body: `${senderUsername} invites you into "${allianceName}".`,
      },
      data: {
        type: "ALLIANCE_INVITE",
        referenceId: invitationId,
      },
    };


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

    const allianceDoc = await admin.firestore()
      .collection("alliances")
      .doc(allianceId)
      .get();

    const membersMap = allianceDoc.data()?.members || {};
    const allianceName = allianceDoc.data()?.name || "Savez";

    const recipientIds = Object.keys(membersMap)
      .filter((id) => id !== senderId);

    if (recipientIds.length === 0) {
      console.log("No other recipients in the alliance.");
      return;
    }

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
        referenceId: allianceId,
      },
    };

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

