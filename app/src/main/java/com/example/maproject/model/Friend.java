package com.example.maproject.model;

import java.util.HashMap;
import java.util.Map;

public class Friend {
    private String friendshipId;
    private String userId;
    private String friendId;
    private String friendUsername;
    private String friendAvatar;
    private long createdAt;
    private FriendshipStatus status;

    public enum FriendshipStatus {
        PENDING,    // Čeka se odobrenje
        ACCEPTED,   // Prihvaćeno
        REJECTED    // Odbijeno
    }

    public Friend() {
    }

    public Friend(String userId, String friendId, String friendUsername, String friendAvatar) {
        this.userId = userId;
        this.friendId = friendId;
        this.friendUsername = friendUsername;
        this.friendAvatar = friendAvatar;
        this.status = FriendshipStatus.PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    // Getteri
    public String getFriendshipId() { return friendshipId; }
    public String getUserId() { return userId; }
    public String getFriendId() { return friendId; }
    public String getFriendUsername() { return friendUsername; }
    public String getFriendAvatar() { return friendAvatar; }
    public long getCreatedAt() { return createdAt; }
    public FriendshipStatus getStatus() { return status; }

    // Setteri
    public void setFriendshipId(String friendshipId) { this.friendshipId = friendshipId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setFriendId(String friendId) { this.friendId = friendId; }
    public void setFriendUsername(String friendUsername) { this.friendUsername = friendUsername; }
    public void setFriendAvatar(String friendAvatar) { this.friendAvatar = friendAvatar; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setStatus(FriendshipStatus status) { this.status = status; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("friendshipId", friendshipId);
        map.put("userId", userId);
        map.put("friendId", friendId);
        map.put("friendUsername", friendUsername);
        map.put("friendAvatar", friendAvatar);
        map.put("createdAt", createdAt);
        map.put("status", status.name());
        return map;
    }
}