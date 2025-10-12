package com.example.maproject.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Alliance {
    private String allianceId;
    private String name;
    private String leaderId;
    private String leaderUsername;
    private long createdAt;
    private Map<String, String> members; // userId -> username
    private boolean missionActive;
    // Opcionalno, za čat:
    // private String chatRef;

    public Alliance() {
        this.members = new ConcurrentHashMap<>();
    }

    public Alliance(String name, String leaderId, String leaderUsername) {
        this.name = name;
        this.leaderId = leaderId;
        this.leaderUsername = leaderUsername;
        this.createdAt = System.currentTimeMillis();
        this.members = new ConcurrentHashMap<>();
        // Vođa je automatski prvi član
        this.members.put(leaderId, leaderUsername);
        this.missionActive = false;
    }

    // Getteri
    public String getAllianceId() { return allianceId; }
    public String getName() { return name; }
    public String getLeaderId() { return leaderId; }
    public String getLeaderUsername() { return leaderUsername; }
    public long getCreatedAt() { return createdAt; }
    public Map<String, String> getMembers() { return members; }
    public boolean isMissionActive() { return missionActive; }
    public List<String> getMemberIds() { return members.keySet().stream().collect(Collectors.toList()); }

    // Setteri
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }
    public void setName(String name) { this.name = name; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }
    public void setLeaderUsername(String leaderUsername) { this.leaderUsername = leaderUsername; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setMembers(Map<String, String> members) { this.members = members; }
    public void setMissionActive(boolean missionActive) { this.missionActive = missionActive; }

    // Pomoćne metode
    public void addMember(String userId, String username) {
        this.members.put(userId, username);
    }

    public void removeMember(String userId) {
        this.members.remove(userId);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("allianceId", allianceId);
        map.put("name", name);
        map.put("leaderId", leaderId);
        map.put("leaderUsername", leaderUsername);
        map.put("createdAt", createdAt);
        map.put("members", members);
        map.put("missionActive", missionActive);
        return map;
    }
}