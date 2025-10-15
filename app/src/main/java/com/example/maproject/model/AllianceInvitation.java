package com.example.maproject.model;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.Timestamp; // Dodat import za Firebase Timestamp

import java.util.HashMap;
import java.util.Map;

public class AllianceInvitation {
    private String invitationId;
    private String allianceId;
    private String allianceName;
    private String senderId;
    private String senderUsername;
    private String receiverId;
    private Timestamp sentAt; // Izmenjeno iz 'long' u Firebase 'Timestamp'
    private String status; // "PENDING", "ACCEPTED", "REJECTED"

    public AllianceInvitation() {
    }

    public AllianceInvitation(String allianceId, String allianceName, String senderId, String senderUsername, String receiverId) {
        this.allianceId = allianceId;
        this.allianceName = allianceName;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.receiverId = receiverId;
        // Uklonjeno je postavljanje 'sentAt' ovde.
        // Vreme će biti postavljeno od strane servera korišćenjem FieldValue.serverTimestamp() u toMap().
        this.status = "PENDING";
    }

    // Getteri
    public String getInvitationId() { return invitationId; }
    public String getAllianceId() { return allianceId; }
    public String getAllianceName() { return allianceName; }
    public String getSenderId() { return senderId; }
    public String getSenderUsername() { return senderUsername; }
    public String getReceiverId() { return receiverId; }
    // Vraća Firebase Timestamp
    public Timestamp getSentAt() { return sentAt; }
    public String getStatus() { return status; }

    // Setteri
    public void setInvitationId(String invitationId) { this.invitationId = invitationId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }
    public void setAllianceName(String allianceName) { this.allianceName = allianceName; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    // Prihvata Firebase Timestamp
    public void setSentAt(Timestamp sentAt) { this.sentAt = sentAt; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        // Iznenađujuće, Firestore ne zahteva 'invitationId' u map-i ako koristite setId() i set(data)
        // ali ostavljamo za potpunost:
        map.put("invitationId", invitationId);
        map.put("allianceId", allianceId);
        map.put("allianceName", allianceName);
        map.put("senderId", senderId);
        map.put("senderUsername", senderUsername);
        map.put("receiverId", receiverId);
        // KRITIČNA POPRAVKA: Koristite serverTimestamp za novo vreme slanja
        map.put("sentAt", FieldValue.serverTimestamp());
        map.put("status", status);
        return map;
    }
}
