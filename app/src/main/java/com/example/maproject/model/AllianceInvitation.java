package com.example.maproject.model;

import java.util.HashMap;
import java.util.Map;

public class AllianceInvitation {
    private String invitationId;
    private String allianceId;
    private String allianceName;
    private String senderId;
    private String senderUsername;
    private String receiverId;
    private long sentAt;
    private String status; // "PENDING", "ACCEPTED", "REJECTED"

    public AllianceInvitation() {
    }

    public AllianceInvitation(String allianceId, String allianceName, String senderId, String senderUsername, String receiverId) {
        this.allianceId = allianceId;
        this.allianceName = allianceName;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.receiverId = receiverId;
        this.sentAt = System.currentTimeMillis();
        this.status = "PENDING";
    }

    // Getteri
    public String getInvitationId() { return invitationId; }
    public String getAllianceId() { return allianceId; }
    public String getAllianceName() { return allianceName; }
    public String getSenderId() { return senderId; }
    public String getSenderUsername() { return senderUsername; }
    public String getReceiverId() { return receiverId; }
    public long getSentAt() { return sentAt; }
    public String getStatus() { return status; }

    // Setteri
    public void setInvitationId(String invitationId) { this.invitationId = invitationId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }
    public void setAllianceName(String allianceName) { this.allianceName = allianceName; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public void setSentAt(long sentAt) { this.sentAt = sentAt; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("invitationId", invitationId);
        map.put("allianceId", allianceId);
        map.put("allianceName", allianceName);
        map.put("senderId", senderId);
        map.put("senderUsername", senderUsername);
        map.put("receiverId", receiverId);
        map.put("sentAt", sentAt);
        map.put("status", status);
        return map;
    }
}