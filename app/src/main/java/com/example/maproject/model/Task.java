package com.example.maproject.model;

import java.util.HashMap;
import java.util.Map;

public class Task {
    private String taskId;
    private String userId;
    private String title;
    private String description;
    private String categoryId;
    private String categoryName;

    public enum Difficulty {
        VERY_EASY(1),    // 1 XP
        EASY(3),         // 3 XP
        HARD(7),         // 7 XP
        EXTREMELY_HARD(20); // 20 XP

        private final int xp;
        Difficulty(int xp) { this.xp = xp; }
        public int getXp() { return xp; }
    }

    public enum Importance {
        NORMAL(1),           // 1 XP
        IMPORTANT(3),        // 3 XP
        EXTREMELY_IMPORTANT(10), // 10 XP
        SPECIAL(100);        // 100 XP

        private final int xp;
        Importance(int xp) { this.xp = xp; }
        public int getXp() { return xp; }
    }

    public enum Status {
        ACTIVE,
        COMPLETED,
        NOT_DONE,
        PAUSED,
        CANCELLED
    }

    private Difficulty difficulty;
    private Importance importance;
    private Status status;
    private boolean isRecurring;
    private long executionTime;
    private long completedTime;
    private long createdTime;


    public Task() {
    }

    public Task(String userId, String title, String categoryId, String categoryName,
                Difficulty difficulty, Importance importance, long executionTime) {
        this.userId = userId;
        this.title = title;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.difficulty = difficulty;
        this.importance = importance;
        this.status = Status.ACTIVE;
        this.executionTime = executionTime;
        this.createdTime = System.currentTimeMillis();
    }

    public String getTaskId() { return taskId; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public Difficulty getDifficulty() { return difficulty; }
    public Importance getImportance() { return importance; }
    public Status getStatus() { return status; }
    public boolean isRecurring() { return isRecurring; }
    public long getExecutionTime() { return executionTime; }
    public long getCompletedTime() { return completedTime; }
    public long getCreatedTime() { return createdTime; }

    public void setTaskId(String taskId) { this.taskId = taskId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public void setImportance(Importance importance) { this.importance = importance; }
    public void setStatus(Status status) { this.status = status; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    public void setCompletedTime(long completedTime) { this.completedTime = completedTime; }
    public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }


    public int getTotalXP() {
        return difficulty.getXp() + importance.getXp();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", taskId);
        map.put("userId", userId);
        map.put("title", title);
        map.put("description", description);
        map.put("categoryId", categoryId);
        map.put("categoryName", categoryName);
        map.put("difficulty", difficulty.name());
        map.put("importance", importance.name());
        map.put("status", status.name());
        map.put("isRecurring", isRecurring);
        map.put("executionTime", executionTime);
        map.put("completedTime", completedTime);
        map.put("createdTime", createdTime);
        return map;
    }
}