package com.example.maproject.data;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.maproject.model.Alliance;
import com.example.maproject.model.AllianceInvitation;
import com.example.maproject.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AllianceRepository {
    private static final String TAG = "AllianceRepository";
    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;

    public AllianceRepository() {
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
    }

    public void createAlliance(Alliance alliance, MutableLiveData<String> resultLiveData) {
        String allianceId = db.collection("alliances").document().getId();
        alliance.setAllianceId(allianceId);

        db.collection("alliances").document(allianceId).set(alliance.toMap())
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(alliance.getLeaderId())
                            .update("currentAllianceId", allianceId)
                            .addOnSuccessListener(aVoid1 -> {
                                resultLiveData.postValue(allianceId);
                                Log.d(TAG, "Alliance created: " + allianceId);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating alliance", e);
                    resultLiveData.postValue(null);
                });
    }

    public void sendInvitation(AllianceInvitation invitation, MutableLiveData<Boolean> resultLiveData) {
        String invitationId = db.collection("invitations").document().getId();
        invitation.setInvitationId(invitationId);

        db.collection("invitations").document(invitationId).set(invitation.toMap())
                .addOnSuccessListener(aVoid -> {
                    resultLiveData.postValue(true);
                    Log.d(TAG, "Invitation sent");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending invitation", e);
                    resultLiveData.postValue(false);
                });
    }

    public void loadInvitations(String userId, MutableLiveData<List<AllianceInvitation>> invitationsLiveData) {
        db.collection("invitations")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading invitations", error);
                        invitationsLiveData.postValue(new ArrayList<>());
                        return;
                    }

                    if (querySnapshot != null) {
                        List<AllianceInvitation> invitations = new ArrayList<>();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            AllianceInvitation invitation = document.toObject(AllianceInvitation.class);
                            invitations.add(invitation);
                        }
                        invitationsLiveData.postValue(invitations);
                    }
                });
    }

    public void acceptInvitation(AllianceInvitation invitation, String userId,
                                 String username, MutableLiveData<Boolean> resultLiveData) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    String currentAllianceId = userDoc.getString("currentAllianceId");

                    if (currentAllianceId != null) {
                        db.collection("alliances").document(currentAllianceId).get()
                                .addOnSuccessListener(allianceDoc -> {
                                    Boolean missionActive = allianceDoc.getBoolean("missionActive");
                                    if (missionActive != null && missionActive) {
                                        resultLiveData.postValue(false);
                                        return;
                                    }
                                    leaveAndJoinAlliance(invitation, userId, username, currentAllianceId, resultLiveData);
                                });
                    } else {
                        joinAlliance(invitation, userId, username, resultLiveData);
                    }
                });
    }

    private void leaveAndJoinAlliance(AllianceInvitation invitation, String userId,
                                      String username, String oldAllianceId, MutableLiveData<Boolean> resultLiveData) {
        db.collection("alliances").document(oldAllianceId).get()
                .addOnSuccessListener(doc -> {
                    Alliance oldAlliance = doc.toObject(Alliance.class);
                    if (oldAlliance != null) {
                        oldAlliance.removeMember(userId);
                        db.collection("alliances").document(oldAllianceId).set(oldAlliance.toMap());
                    }
                    joinAlliance(invitation, userId, username, resultLiveData);
                });
    }

    private void joinAlliance(AllianceInvitation invitation, String userId,
                              String username, MutableLiveData<Boolean> resultLiveData) {
        String allianceId = invitation.getAllianceId();

        db.collection("alliances").document(allianceId).get()
                .addOnSuccessListener(doc -> {
                    Alliance alliance = doc.toObject(Alliance.class);
                    if (alliance != null) {
                        alliance.addMember(userId, username);

                        db.collection("alliances").document(allianceId).set(alliance.toMap())
                                .addOnSuccessListener(aVoid -> {
                                    db.collection("users").document(userId)
                                            .update("currentAllianceId", allianceId);

                                    db.collection("invitations").document(invitation.getInvitationId())
                                            .update("status", "ACCEPTED");

                                    // SLANJE SISTEMSKE NOTIFIKACIJE VOĐI
                                    Notification notification = new Notification(
                                            alliance.getLeaderId(),
                                            "ALLIANCE_ACCEPTED",
                                            username + " je prihvatio/la poziv i pridružio/la se savezu " + alliance.getName() + ".",
                                            allianceId
                                    );
                                    notificationRepository.sendNotification(notification);

                                    resultLiveData.postValue(true);
                                    Log.d(TAG, "Joined alliance");
                                });
                    }
                });
    }

    public void rejectInvitation(String invitationId, MutableLiveData<Boolean> resultLiveData) {
        db.collection("invitations").document(invitationId)
                .update("status", "REJECTED")
                .addOnSuccessListener(aVoid -> {
                    resultLiveData.postValue(true);
                    Log.d(TAG, "Invitation rejected");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error rejecting invitation", e);
                    resultLiveData.postValue(false);
                });
    }

    public void loadUserAlliance(String userId, MutableLiveData<Alliance> allianceLiveData) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    String allianceId = userDoc.getString("currentAllianceId");
                    if (allianceId != null) {
                        db.collection("alliances").document(allianceId)
                                .addSnapshotListener((doc, error) -> {
                                    if (doc != null && doc.exists()) {
                                        Alliance alliance = doc.toObject(Alliance.class);
                                        allianceLiveData.postValue(alliance);
                                    } else {
                                        allianceLiveData.postValue(null);
                                    }
                                });
                    } else {
                        allianceLiveData.postValue(null);
                    }
                });
    }

    public void deleteAlliance(String allianceId, MutableLiveData<Boolean> resultLiveData) {
        db.collection("alliances").document(allianceId).get()
                .addOnSuccessListener(doc -> {
                    Alliance alliance = doc.toObject(Alliance.class);
                    if (alliance != null && !alliance.isMissionActive()) {
                        for (String memberId : alliance.getMemberIds()) {
                            db.collection("users").document(memberId)
                                    .update("currentAllianceId", null);
                        }

                        db.collection("alliances").document(allianceId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    resultLiveData.postValue(true);
                                    Log.d(TAG, "Alliance deleted");
                                })
                                .addOnFailureListener(e -> resultLiveData.postValue(false));
                    } else {
                        resultLiveData.postValue(false);
                    }
                });
    }

    public void leaveAlliance(String allianceId, String userId, MutableLiveData<Boolean> resultLiveData) {
        db.collection("alliances").document(allianceId).get()
                .addOnSuccessListener(doc -> {
                    Alliance alliance = doc.toObject(Alliance.class);
                    if (alliance != null && !alliance.isMissionActive()) {
                        alliance.removeMember(userId);

                        db.collection("alliances").document(allianceId).set(alliance.toMap())
                                .addOnSuccessListener(aVoid -> {
                                    db.collection("users").document(userId)
                                            .update("currentAllianceId", null)
                                            .addOnSuccessListener(aVoid1 -> {
                                                resultLiveData.postValue(true);
                                                Log.d(TAG, "User left alliance " + allianceId);
                                            });
                                })
                                .addOnFailureListener(e -> resultLiveData.postValue(false));
                    } else {
                        resultLiveData.postValue(false);
                    }
                })
                .addOnFailureListener(e -> resultLiveData.postValue(false));
    }
}