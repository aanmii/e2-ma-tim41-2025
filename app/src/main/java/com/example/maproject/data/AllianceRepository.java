package com.example.maproject.data;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.maproject.model.Alliance;
import com.example.maproject.model.AllianceInvitation;
import com.example.maproject.model.Notification;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllianceRepository {

    private static final String TAG = "AllianceRepository";
    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;

    public AllianceRepository() {
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
    }

    public interface AllianceActionCallback {
        void onComplete(boolean success);
    }

    public interface UsernameCallback {
        void onUsernameRetrieved(String username);
    }

    public void createAlliance(Alliance alliance, AllianceActionCallback callback) {
        String allianceId = db.collection("alliances").document().getId();
        alliance.setAllianceId(allianceId);

        db.collection("alliances").document(allianceId)
                .set(new HashMap<>(alliance.toMap()))
                .addOnSuccessListener(aVoid -> db.collection("users").document(alliance.getLeaderId())
                        .update("currentAllianceId", allianceId)
                        .addOnSuccessListener(aVoid1 -> {
                            Log.d(TAG, "Alliance created: " + allianceId);
                            callback.onComplete(true);
                        })
                        .addOnFailureListener(e -> callback.onComplete(false)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating alliance", e);
                    callback.onComplete(false);
                });
    }

    public void sendInvitation(AllianceInvitation invitation, InvitationCallback callback) {
        Map<String, Object> invitationData = new HashMap<>();
        invitationData.put("allianceId", invitation.getAllianceId());
        invitationData.put("allianceName", invitation.getAllianceName());
        invitationData.put("senderId", invitation.getSenderId());
        invitationData.put("senderUsername", invitation.getSenderUsername());
        invitationData.put("recipientId", invitation.getRecipientId());
        invitationData.put("status", "PENDING");
        invitationData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("invitations")
                .add(invitationData)
                .addOnSuccessListener(documentReference -> {
                    invitation.setInvitationId(documentReference.getId());
                    callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("AllianceRepository", "Error sending invitation", e);
                    callback.onComplete(false);
                });
    }

    public interface InvitationCallback {
        void onComplete(boolean success);
    }

    public void loadInvitations(String userId, MutableLiveData<List<AllianceInvitation>> invitationsLiveData) {
        db.collection("invitations")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((querySnapshot, error) -> {
                    List<AllianceInvitation> invitations = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            invitations.add(doc.toObject(AllianceInvitation.class));
                        }
                    }
                    invitationsLiveData.postValue(invitations);
                });
    }

    public void respondToInvitation(AllianceInvitation invitation, String userId, String username, AllianceActionCallback callback) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    String currentAllianceId = userDoc.getString("currentAllianceId");
                    if (currentAllianceId != null) {
                        db.collection("alliances").document(currentAllianceId).get()
                                .addOnSuccessListener(allianceDoc -> {
                                    Boolean missionActive = allianceDoc.getBoolean("missionActive");
                                    if (missionActive != null && missionActive) {
                                        callback.onComplete(false);
                                    } else {
                                        leaveAndJoinAlliance(invitation, userId, username, currentAllianceId, callback);
                                    }
                                }).addOnFailureListener(e -> callback.onComplete(false));
                    } else {
                        joinAlliance(invitation, userId, username, callback);
                    }
                }).addOnFailureListener(e -> callback.onComplete(false));
    }

    private void leaveAndJoinAlliance(AllianceInvitation invitation, String userId, String username, String oldAllianceId, AllianceActionCallback callback) {
        db.collection("alliances").document(oldAllianceId).get()
                .addOnSuccessListener(doc -> {
                    Alliance oldAlliance = doc.toObject(Alliance.class);
                    if (oldAlliance != null) {
                        oldAlliance.removeMember(userId);
                        db.collection("alliances").document(oldAllianceId).set(new HashMap<>(oldAlliance.toMap()));
                    }
                    joinAlliance(invitation, userId, username, callback);
                }).addOnFailureListener(e -> callback.onComplete(false));
    }

    private void joinAlliance(AllianceInvitation invitation, String userId, String username, AllianceActionCallback callback) {
        String allianceId = invitation.getAllianceId();
        String invitationId = invitation.getInvitationId();

        if (allianceId == null || invitationId == null) {
            Log.e(TAG, "Missing Alliance ID or Invitation ID during join process.");
            callback.onComplete(false);
            return;
        }

        final String finalUsername = username != null ? username : "Unknown";

        db.collection("alliances").document(allianceId).get()
                .addOnSuccessListener(doc -> {
                    Alliance alliance = doc.toObject(Alliance.class);
                    if (alliance != null) {
                        alliance.addMember(userId, finalUsername);

                        db.collection("alliances").document(allianceId).set(new HashMap<>(alliance.toMap()))
                                .addOnSuccessListener(aVoid -> db.collection("users").document(userId)
                                        .update("currentAllianceId", allianceId)
                                        .addOnSuccessListener(aVoid1 -> {
                                            db.collection("invitations").document(invitationId)
                                                    .update("status", "ACCEPTED");
                                            Notification notification = new Notification(
                                                    alliance.getLeaderId(),
                                                    "ALLIANCE_ACCEPTED",
                                                    finalUsername + " accepted your invite and joined " + alliance.getName() + ".",
                                                    allianceId
                                            );
                                            notificationRepository.sendNotification(notification);
                                            callback.onComplete(true);
                                        }).addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to update user's currentAllianceId.", e);
                                            callback.onComplete(false);
                                        }))
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update alliance member list.", e);
                                    callback.onComplete(false);
                                });
                    } else {
                        Log.w(TAG, "Alliance document not found: " + allianceId);
                        callback.onComplete(false);
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching alliance for joining.", e);
                    callback.onComplete(false);
                });
    }

    public void deleteAlliance(String allianceId, AllianceActionCallback callback) {
        db.collection("alliances").document(allianceId).get()
                .addOnSuccessListener(doc -> {
                    Alliance alliance = doc.toObject(Alliance.class);
                    if (alliance != null && !alliance.isMissionActive()) {
                        List<String> members = alliance.getMemberIds();
                        if (members != null) {
                            for (String memberId : members) {
                                db.collection("users").document(memberId).update("currentAllianceId", null);
                            }
                        }
                        db.collection("alliances").document(allianceId).delete()
                                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                                .addOnFailureListener(e -> callback.onComplete(false));
                    } else callback.onComplete(false);
                }).addOnFailureListener(e -> callback.onComplete(false));
    }

    public void leaveAlliance(String allianceId, String userId, AllianceActionCallback callback) {
        db.collection("alliances").document(allianceId).get()
                .addOnSuccessListener(doc -> {
                    Alliance alliance = doc.toObject(Alliance.class);
                    if (alliance != null && !alliance.isMissionActive()) {
                        alliance.removeMember(userId);
                        db.collection("alliances").document(allianceId).set(new HashMap<>(alliance.toMap()))
                                .addOnSuccessListener(aVoid -> db.collection("users").document(userId)
                                        .update("currentAllianceId", null)
                                        .addOnSuccessListener(aVoid1 -> callback.onComplete(true))
                                        .addOnFailureListener(e -> callback.onComplete(false)))
                                .addOnFailureListener(e -> callback.onComplete(false));
                    } else callback.onComplete(false);
                }).addOnFailureListener(e -> callback.onComplete(false));
    }

    public void loadUserAlliance(String userId, MutableLiveData<Alliance> allianceLiveData) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    String allianceId = userDoc.getString("currentAllianceId");
                    if (allianceId != null) {
                        db.collection("alliances").document(allianceId)
                                .addSnapshotListener((doc, error) -> {
                                    if (doc != null && doc.exists()) {
                                        allianceLiveData.postValue(doc.toObject(Alliance.class));
                                    } else allianceLiveData.postValue(null);
                                });
                    } else allianceLiveData.postValue(null);
                })
                .addOnFailureListener(e -> allianceLiveData.postValue(null));
    }

    public void getUsername(String userId, UsernameCallback callback) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> callback.onUsernameRetrieved(doc.getString("username")))
                .addOnFailureListener(e -> callback.onUsernameRetrieved("Unknown"));
    }

    public void cleanupOldInvitations(String userId) {
        db.collection("invitations")
                .whereEqualTo("recipientId", userId)
                .whereIn("status", Arrays.asList("REJECTED", "AUTO_REJECTED", "ACCEPTED"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                        Log.d("AllianceRepository", "Deleted old invitation: " + doc.getId());
                    }
                });
    }
}