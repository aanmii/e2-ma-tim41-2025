"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.sendChatNotification = exports.sendAllianceInviteNotification = void 0;
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-functions/v2/firestore");
// Inicijalizacija Firebase Admin SDK-a
admin.initializeApp();
/**
 * Aktivira se kada se novi dokument kreira u 'invitations' kolekciji.
 * Šalje Push notifikaciju primaocu pozivnice.
 */
exports.sendAllianceInviteNotification = (0, firestore_1.onDocumentCreated)("invitations/{invitationId}", async (event) => {
    var _a, _b;
    const invitationData = (_a = event.data) === null || _a === void 0 ? void 0 : _a.data();
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
    const fcmToken = (_b = userDoc.data()) === null || _b === void 0 ? void 0 : _b.fcmToken;
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
    }
    catch (error) {
        console.error("Error sending alliance invitation notification:", error);
    }
});
exports.sendChatNotification = (0, firestore_1.onDocumentCreated)("alliances/{allianceId}/messages/{messageId}", async (event) => {
    var _a, _b, _c;
    const chatMessage = (_a = event.data) === null || _a === void 0 ? void 0 : _a.data();
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
    const membersMap = ((_b = allianceDoc.data()) === null || _b === void 0 ? void 0 : _b.members) || {};
    const allianceName = ((_c = allianceDoc.data()) === null || _c === void 0 ? void 0 : _c.name) || "Savez";
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
    const tokens = [];
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
    const body = `${senderUsername}: ${messageContent.substring(0, 50)}${messageContent.length > 50 ? "..." : ""}`;
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
        await admin.messaging().sendEachForMulticast(Object.assign({ tokens }, message));
        console.log(`Notif sent to ${tokens.length} members of alliance: ${allianceId}`);
    }
    catch (error) {
        console.error("Error sending chat notification:", error);
    }
});
//# sourceMappingURL=index.js.map