package com.example.maproject.model;

import java.util.Locale;

public class Task {

    private String taskId;
    private String userId;
    private String title;
    private String description;
    private String categoryId;
    private String categoryName;

    public enum Difficulty {
        VERY_EASY(1), EASY(3), HARD(7), EXTREMELY_HARD(20);
        private final int xp;
        Difficulty(int xp) { this.xp = xp; }
        public int getXp() { return xp; }

        public static Difficulty fromString(String value) {
            try {
                return Difficulty.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                return EASY;
            }
        }
    }

    public enum Importance {
        NORMAL(1), IMPORTANT(3), EXTREMELY_IMPORTANT(10), SPECIAL(100);
        private final int xp;
        Importance(int xp) { this.xp = xp; }
        public int getXp() { return xp; }

        public static Importance fromString(String value) {
            try {
                return Importance.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                return NORMAL;
            }
        }
    }

    public enum Status { ACTIVE, COMPLETED, NOT_DONE, PAUSED, CANCELLED }

    public enum TimeUnit {
        HOUR, DAY, WEEK, MONTH;

        public static TimeUnit fromString(String value) {
            try {
                return TimeUnit.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                return DAY;
            }
        }
    }

    // === Core attributes ===
    private Difficulty difficulty;
    private Importance importance;
    private Status status;
    private boolean isRecurring;

    // === Timing fields ===
    private long startDate;               // when the task begins
    private int durationInterval;         // how long it lasts (e.g. 3)
    private TimeUnit durationUnit;        // unit for duration (HOUR, DAY, WEEK)

    // === Recurrence (only if recurring) ===
    private int recurrenceInterval;       // e.g. every 2 weeks
    private TimeUnit recurrenceUnit;      // HOUR, DAY, WEEK, MONTH
    private long recurrenceStart;         // first occurrence start date
    private long recurrenceEnd;           // last possible occurrence

    // === Status timestamps ===
    private long completedTime;
    private long createdTime;

    public Task() {}

    public Task(String userId, String title, String categoryId, String categoryName,
                Difficulty difficulty, Importance importance, long startDate,
                int durationInterval, TimeUnit durationUnit) {
        this.userId = userId;
        this.title = title;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.difficulty = difficulty;
        this.importance = importance;
        this.status = Status.ACTIVE;
        this.startDate = startDate;
        this.durationInterval = durationInterval;
        this.durationUnit = durationUnit;
        this.isRecurring = false;
        this.createdTime = System.currentTimeMillis();
    }

    // === Getters & Setters ===
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public Importance getImportance() { return importance; }
    public void setImportance(Importance importance) { this.importance = importance; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public int getDurationInterval() { return durationInterval; }
    public void setDurationInterval(int durationInterval) { this.durationInterval = durationInterval; }

    public TimeUnit getDurationUnit() { return durationUnit; }
    public void setDurationUnit(TimeUnit durationUnit) { this.durationUnit = durationUnit; }

    public int getRecurrenceInterval() { return recurrenceInterval; }
    public void setRecurrenceInterval(int recurrenceInterval) { this.recurrenceInterval = recurrenceInterval; }

    public TimeUnit getRecurrenceUnit() { return recurrenceUnit; }
    public void setRecurrenceUnit(TimeUnit recurrenceUnit) { this.recurrenceUnit = recurrenceUnit; }

    public long getRecurrenceStart() { return recurrenceStart; }
    public void setRecurrenceStart(long recurrenceStart) { this.recurrenceStart = recurrenceStart; }

    public long getRecurrenceEnd() { return recurrenceEnd; }
    public void setRecurrenceEnd(long recurrenceEnd) { this.recurrenceEnd = recurrenceEnd; }

    public long getCompletedTime() { return completedTime; }
    public void setCompletedTime(long completedTime) { this.completedTime = completedTime; }

    public long getCreatedTime() { return createdTime; }
    public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }
}
