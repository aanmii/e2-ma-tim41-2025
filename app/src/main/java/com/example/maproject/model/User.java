package com.example.maproject.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private String userId;
    private String email;
    private String username;
    private String avatar;
    private int level;
    private String title;
    private int powerPoints; // PP
    private int experiencePoints; // XP
    private int coins;
    private int badges;
    private List<String> equipment; // Lista opreme
    private List<String> activeEquipment; // Trenutno aktivna oprema
    private boolean isEmailVerified;
    private long registrationTimestamp;

    // Prazan konstruktor za Firebase
    public User() {
        this.equipment = new ArrayList<>();
        this.activeEquipment = new ArrayList<>();
    }

    // Konstruktor za registraciju
    public User(String userId, String email, String username, String avatar) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.avatar = avatar;
        this.level = 0;
        this.title = "Početnik"; // Početna titula
        this.powerPoints = 0;
        this.experiencePoints = 0;
        this.coins = 0;
        this.badges = 0;
        this.equipment = new ArrayList<>();
        this.activeEquipment = new ArrayList<>();
        this.isEmailVerified = false;
        this.registrationTimestamp = System.currentTimeMillis();
    }

    // Getteri
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public int getPowerPoints() { return powerPoints; }
    public int getExperiencePoints() { return experiencePoints; }
    public int getCoins() { return coins; }
    public int getBadges() { return badges; }
    public List<String> getEquipment() { return equipment; }
    public List<String> getActiveEquipment() { return activeEquipment; }
    public boolean isEmailVerified() { return isEmailVerified; }
    public long getRegistrationTimestamp() { return registrationTimestamp; }

    // Setteri
    public void setUserId(String userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }
    public void setUsername(String username) { this.username = username; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setLevel(int level) { this.level = level; }
    public void setTitle(String title) { this.title = title; }
    public void setPowerPoints(int powerPoints) { this.powerPoints = powerPoints; }
    public void setExperiencePoints(int experiencePoints) { this.experiencePoints = experiencePoints; }
    public void setCoins(int coins) { this.coins = coins; }
    public void setBadges(int badges) { this.badges = badges; }
    public void setEquipment(List<String> equipment) { this.equipment = equipment; }
    public void setActiveEquipment(List<String> activeEquipment) { this.activeEquipment = activeEquipment; }
    public void setEmailVerified(boolean emailVerified) { isEmailVerified = emailVerified; }
    public void setRegistrationTimestamp(long registrationTimestamp) { this.registrationTimestamp = registrationTimestamp; }

    // Konverzija u Map za Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("email", email);
        map.put("username", username);
        map.put("avatar", avatar);
        map.put("level", level);
        map.put("title", title);
        map.put("powerPoints", powerPoints);
        map.put("experiencePoints", experiencePoints);
        map.put("coins", coins);
        map.put("badges", badges);
        map.put("equipment", equipment);
        map.put("activeEquipment", activeEquipment);
        map.put("isEmailVerified", isEmailVerified);
        map.put("registrationTimestamp", registrationTimestamp);
        return map;
    }
}