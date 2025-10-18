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
exports.cleanupUnverifiedUsers = exports.sendChatNotification = exports.sendAllianceInviteNotification = void 0;
const admin = __importStar(require("firebase-admin"));
const firestore_1 = require("firebase-functions/v2/firestore");
admin.initializeApp();
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
    const userDoc = await admin.firestore()
        .collection("users")
        .doc(recepientId)
        .get();
    const fcmToken = (_b = userDoc.data()) === null || _b === void 0 ? void 0 : _b.fcmToken;
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
    const allianceDoc = await admin.firestore()
        .collection("alliances")
        .doc(allianceId)
        .get();
    const membersMap = ((_b = allianceDoc.data()) === null || _b === void 0 ? void 0 : _b.members) || {};
    const allianceName = ((_c = allianceDoc.data()) === null || _c === void 0 ? void 0 : _c.name) || "Savez";
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
    const title = allianceName;
    const body = `${senderUsername}: ${messageContent.substring(0, 50)}${messageContent.length > 50 ? "..." : ""}`;
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
        await admin.messaging().sendEachForMulticast(Object.assign({ tokens }, message));
        console.log(`Notif sent to ${tokens.length} members of alliance: ${allianceId}`);
    }
    catch (error) {
        console.error("Error sending chat notification:", error);
    }
});
const scheduler_1 = require("firebase-functions/v2/scheduler");
exports.cleanupUnverifiedUsers = (0, scheduler_1.onSchedule)("every 24 hours", async (event) => {
    const db = admin.firestore();
    const cutoffTime = Date.now() - 5 * 60 * 1000;
    console.log("Starting cleanup of unverified users...");
    try {
        const snapshot = await db.collection("users")
            .where("isEmailVerified", "==", false)
            .where("registrationTimestamp", "<", cutoffTime)
            .get();
        if (snapshot.empty) {
            console.log("No unverified users to delete.");
            return;
        }
        for (const doc of snapshot.docs) {
            const uid = doc.id;
            try {
                await admin.auth().deleteUser(uid);
                console.log(`Deleted user from Auth: ${uid}`);
            }
            catch (authErr) {
                console.error(`Failed to delete user ${uid} from Auth:`, authErr);
            }
            try {
                await doc.ref.delete();
                console.log(`Deleted user document: ${uid}`);
            }
            catch (firestoreErr) {
                console.error(`Failed to delete doc for ${uid}:`, firestoreErr);
            }
        }
        console.log("Cleanup complete.");
    }
    catch (err) {
        console.error("Error running cleanupUnverifiedUsers:", err);
    }
});
//# sourceMappingURL=index.js.map