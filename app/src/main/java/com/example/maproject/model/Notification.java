package com.example.maproject.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FieldValue; // Dodato za kreiranje vremena na serveru

import java.util.HashMap;
import java.util.Map;

public class Notification {

    private String receiverId;
    private String type;
    private String content;
    private String referenceId;
    private Timestamp timestamp;
    private boolean isRead;


    @Exclude
    private String notificationId;

    public Notification() {

    }


    public Notification(String receiverId, String type, String content, String referenceId) {
        this.receiverId = receiverId;
        this.type = type;
        this.content = content;
        this.referenceId = referenceId;
        this.isRead = false;
    }

    @Exclude
    public String getNotificationId() { return notificationId; }
    public String getReceiverId() { return receiverId; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public String getReferenceId() { return referenceId; }
    public Timestamp getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }


    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public void setType(String type) { this.type = type; }
    public void setContent(String content) { this.content = content; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { isRead = read; }


    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("receiverId", receiverId);
        map.put("type", type);
        map.put("content", content);
        map.put("referenceId", referenceId);
        map.put("timestamp", FieldValue.serverTimestamp());
        map.put("isRead", isRead);
        return map;
    }
}
