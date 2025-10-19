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
    private int powerPoints;
    private long currentLevelXP;
    private long totalExperiencePoints;
    private int coins;
    private int badges;
    private List<InventoryItem> equipment;
    private List<InventoryItem> activeEquipment;
    private boolean isEmailVerified;
    private long registrationTimestamp;

    public User() {
        this.equipment = new ArrayList<>();
        this.activeEquipment = new ArrayList<>();
        this.level = 1;
        this.powerPoints = 40;
    }

    public User(String userId, String email, String username, String avatar) {
        this();
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.avatar = avatar;
        this.title = "Beginner";
        this.currentLevelXP = 0;
        this.totalExperiencePoints = 0;
        this.coins = 0;
        this.badges = 0;
        this.isEmailVerified = false;
        this.registrationTimestamp = System.currentTimeMillis();
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getAvatar() { return avatar; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public int getPowerPoints() { return powerPoints; }
    public long getCurrentLevelXP() { return currentLevelXP; }
    public long getTotalExperiencePoints() { return totalExperiencePoints; }
    public int getCoins() { return coins; }
    public int getBadges() { return badges; }

    public List<InventoryItem> getEquipment() {
        return equipment != null ? equipment : new ArrayList<>();
    }

    public List<InventoryItem> getActiveEquipment() {
        return activeEquipment != null ? activeEquipment : new ArrayList<>();
    }

    public boolean isEmailVerified() { return isEmailVerified; }
    public long getRegistrationTimestamp() { return registrationTimestamp; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }
    public void setUsername(String username) { this.username = username; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setLevel(int level) { this.level = level; }
    public void setTitle(String title) { this.title = title; }
    public void setPowerPoints(int powerPoints) { this.powerPoints = powerPoints; }
    public void setCurrentLevelXP(long currentLevelXP) { this.currentLevelXP = currentLevelXP; }
    public void setTotalExperiencePoints(long totalExperiencePoints) {
        this.totalExperiencePoints = totalExperiencePoints;
    }
    public void setCoins(int coins) { this.coins = coins; }
    public void setBadges(int badges) { this.badges = badges; }
    public void setEquipment(List<InventoryItem> equipment) {
        this.equipment = equipment != null ? equipment : new ArrayList<>();
    }
    public void setActiveEquipment(List<InventoryItem> activeEquipment) {
        this.activeEquipment = activeEquipment != null ? activeEquipment : new ArrayList<>();
    }
    public void setEmailVerified(boolean emailVerified) {
        this.isEmailVerified = emailVerified;
    }
    public void setRegistrationTimestamp(long registrationTimestamp) {
        this.registrationTimestamp = registrationTimestamp;
    }

    public void addItemToEquipment(InventoryItem item) {
        if (equipment == null) equipment = new ArrayList<>();

        for (InventoryItem existing : equipment) {
            if (existing.getItemId().equals(item.getItemId())) {
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                return;
            }
        }
        equipment.add(item);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("email", email);
        map.put("username", username);
        map.put("avatar", avatar);
        map.put("level", level);
        map.put("title", title);
        map.put("powerPoints", powerPoints);
        map.put("currentLevelXP", currentLevelXP);
        map.put("totalExperiencePoints", totalExperiencePoints);
        map.put("coins", coins);
        map.put("badges", badges);
        map.put("equipment", equipment != null ? equipment : new ArrayList<>());
        map.put("activeEquipment", activeEquipment != null ? activeEquipment : new ArrayList<>());
        map.put("isEmailVerified", isEmailVerified);
        map.put("registrationTimestamp", registrationTimestamp);
        return map;
    }
}