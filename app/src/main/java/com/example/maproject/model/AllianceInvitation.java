package com.example.maproject.model;

public class AllianceInvitation {
    private String invitationId;
    private String allianceId;
    private String allianceName;
    private String senderId;
    private String senderUsername;
    private String recipientId;
    private String status; // PENDING, ACCEPTED, REJECTED
    private com.google.firebase.Timestamp timestamp;

    // Prazan konstruktor za Firebase
    public AllianceInvitation() {
    }

    public AllianceInvitation(String allianceId, String allianceName, String senderId,
                              String senderUsername, String recipientId) {
        this.allianceId = allianceId;
        this.allianceName = allianceName;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.recipientId = recipientId;
        this.status = "PENDING";
    }

    // Getters
    public String getInvitationId() {
        return invitationId;
    }

    public String getAllianceId() {
        return allianceId;
    }

    public String getAllianceName() {
        return allianceName;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public String getStatus() {
        return status;
    }

    public com.google.firebase.Timestamp getTimestamp() {
        return timestamp;
    }

    // Setters - VAŽNO: Dodaj setter za invitationId
    public void setInvitationId(String invitationId) {
        this.invitationId = invitationId;
    }

    public void setAllianceId(String allianceId) {
        this.allianceId = allianceId;
    }

    public void setAllianceName(String allianceName) {
        this.allianceName = allianceName;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(com.google.firebase.Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}