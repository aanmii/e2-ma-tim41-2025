package com.example.maproject.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatMessage {
    private String senderId;
    private String senderUsername;
    private String content;
    private Date timestamp;

    public ChatMessage() {

    }


    public ChatMessage(String senderId, String senderUsername, String content) {
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;

    }


    public String getSenderId() {
        return senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getContent() {
        return content;
    }

    public Date getTimestamp() {
        return timestamp;
    }


    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }


    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("senderId", senderId);
        map.put("senderUsername", senderUsername);
        map.put("content", content);
        map.put("timestamp", new Date());
        return map;
    }
}