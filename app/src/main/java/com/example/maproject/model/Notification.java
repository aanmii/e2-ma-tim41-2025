package com.example.maproject.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Notification {
    private String notificationId; // NOVO POLJE
    private String receiverId;
    private String type;
    private String content;
    private String referenceId;
    private Date timestamp;
    private boolean isRead;

    public Notification() {

    }

    public Notification(String receiverId, String type, String content, String referenceId) {
        this.receiverId = receiverId;
        this.type = type;
        this.content = content;
        this.referenceId = referenceId;
        this.timestamp = new Date();
        this.isRead = false;
    }

    // --- GETTERI ---
    public String getNotificationId() { return notificationId; }
    public String getReceiverId() { return receiverId; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public String getReferenceId() { return referenceId; }
    public Date getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }

    // --- SETTERI ---
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; } // NOVI SETTER
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public void setType(String type) { this.type = type; }
    public void setContent(String content) { this.content = content; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { isRead = read; }

    // --- toMap METODA ---
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("notificationId", notificationId);
        map.put("receiverId", receiverId);
        map.put("type", type);
        map.put("content", content);
        map.put("referenceId", referenceId);
        map.put("timestamp", timestamp);
        map.put("isRead", isRead);
        return map;
    }


}