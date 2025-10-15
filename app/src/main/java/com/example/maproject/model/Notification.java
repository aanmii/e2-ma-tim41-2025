package com.example.maproject.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FieldValue; // Dodato za kreiranje vremena na serveru

import java.util.HashMap;
import java.util.Map;

public class Notification {
    // Polja koja se čitaju/pišu iz Firestore-a moraju biti obavezno tu
    private String receiverId;
    private String type; // Npr. "ALLIANCE_INVITE", "ALLIANCE_ACCEPTED", "CHAT_MESSAGE"
    private String content;
    private String referenceId; // ID saveza, pozivnice, poruke, itd.
    private Timestamp timestamp;
    private boolean isRead;

    // Polje koje se ne sme slati u Firestore, ali se koristi u aplikaciji
    @Exclude
    private String notificationId;

    public Notification() {
        // Obavezan prazan konstruktor za Firebase Firestore
    }

    // Konstruktor za slanje notifikacije
    public Notification(String receiverId, String type, String content, String referenceId) {
        this.receiverId = receiverId;
        this.type = type;
        this.content = content;
        this.referenceId = referenceId;
        // Ostavljamo Timestamp prazan; popuniće se preko toMap() ili repozitorijuma
        this.isRead = false;
    }

    // Getteri (Obavezni za Firestore)
    @Exclude
    public String getNotificationId() { return notificationId; }
    public String getReceiverId() { return receiverId; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public String getReferenceId() { return referenceId; }
    public Timestamp getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }

    // Setteri (Obavezni za Firestore)
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public void setType(String type) { this.type = type; }
    public void setContent(String content) { this.content = content; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { isRead = read; }

    // Metoda za mapiranje (koristi se za slanje)
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("receiverId", receiverId);
        map.put("type", type);
        map.put("content", content);
        map.put("referenceId", referenceId);
        // Koristimo FieldValue.serverTimestamp() da bi se tačno vreme postavilo na serveru
        map.put("timestamp", FieldValue.serverTimestamp());
        map.put("isRead", isRead);
        return map;
    }
}
