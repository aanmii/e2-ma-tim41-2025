package com.example.maproject.data;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.maproject.model.Alliance;
import com.example.maproject.model.AllianceInvitation;
import com.example.maproject.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AllianceRepository {

    private static final String TAG = "AllianceRepository";
    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;

    public AllianceRepository() {
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
    }

    // --- CALLBACK INTERFEJSI ---
    public interface AllianceActionCallback {
        void onComplete(boolean success);
    }

    public interface UsernameCallback {
        void onUsernameRetrieved(String username);
    }

    // --- CREATE ALLIANCE ---
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

    // --- SEND INVITATION ---
    public void sendInvitation(AllianceInvitation invitation, AllianceActionCallback callback) {
        String invitationId = db.collection("invitations").document().getId();
        invitation.setInvitationId(invitationId);

        db.collection("invitations").document(invitationId)
                .set(new HashMap<>(invitation.toMap()))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Invitation sent");
                    callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending invitation", e);
                    callback.onComplete(false);
                });
    }

    // --- LOAD INVITATIONS ---
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

    // --- RESPOND TO INVITATION ---
    public void respondToInvitation(AllianceInvitation invitation, String userId, String username, AllianceActionCallback callback) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    String currentAllianceId = userDoc.getString("currentAllianceId");
                    if (currentAllianceId != null) {
                        db.collection("alliances").document(currentAllianceId).get()
                                .addOnSuccessListener(allianceDoc -> {
                                    Boolean missionActive = allianceDoc.getBoolean("missionActive");
                                    if (missionActive != null && missionActive) {
                                        callback.onComplete(false); // ne može da se pridruži
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
        db.collection("alliances").document(allianceId).get()
                .addOnSuccessListener(doc -> {
                    Alliance alliance = doc.toObject(Alliance.class);
                    if (alliance != null) {
                        alliance.addMember(userId, username);
                        db.collection("alliances").document(allianceId).set(new HashMap<>(alliance.toMap()))
                                .addOnSuccessListener(aVoid -> db.collection("users").document(userId)
                                        .update("currentAllianceId", allianceId)
                                        .addOnSuccessListener(aVoid1 -> {
                                            db.collection("invitations").document(invitation.getInvitationId())
                                                    .update("status", "ACCEPTED");
                                            Notification notification = new Notification(
                                                    alliance.getLeaderId(),
                                                    "ALLIANCE_ACCEPTED",
                                                    username + " je prihvatio/la poziv i pridružio/la se savezu " + alliance.getName() + ".",
                                                    allianceId
                                            );
                                            notificationRepository.sendNotification(notification);
                                            callback.onComplete(true);
                                        }).addOnFailureListener(e -> callback.onComplete(false)))
                                .addOnFailureListener(e -> callback.onComplete(false));
                    } else callback.onComplete(false);
                }).addOnFailureListener(e -> callback.onComplete(false));
    }

    // --- DELETE ALLIANCE ---
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

    // --- LEAVE ALLIANCE ---
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

    // --- LOAD USER ALLIANCE ---
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

    // --- GET USERNAME ---
    public void getUsername(String userId, UsernameCallback callback) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> callback.onUsernameRetrieved(doc.getString("username")))
                .addOnFailureListener(e -> callback.onUsernameRetrieved("Unknown"));
    }
}