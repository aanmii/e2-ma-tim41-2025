package com.example.maproject.model;

import java.util.HashMap;
import java.util.Map;

public class UserStatistics {
    private int activeDays;
    private int totalTasksCreated;
    private int totalTasksCompleted;
    private int totalTasksNotDone;
    private int totalTasksCancelled;
    private int longestStreak;
    private int currentStreak;
    private int specialMissionsStarted;
    private int specialMissionsCompleted;
    private Map<String, Integer> tasksPerCategory;
    private long lastActiveDate;
    private int totalCompletedTaskDifficultySum;
    private Map<String, Long> dailyXP;
    private Map<String, Long> dailyDifficultySum;
    private Map<String, Long> dailyCompletedCount;


    public UserStatistics() {
        this.tasksPerCategory = new HashMap<>();
        this.dailyXP = new HashMap<>();
        this.dailyDifficultySum = new HashMap<>();
        this.dailyCompletedCount = new HashMap<>();
        this.lastActiveDate = System.currentTimeMillis();
    }

    public int getActiveDays() { return activeDays; }
    public int getTotalTasksCreated() { return totalTasksCreated; }
    public int getTotalTasksCompleted() { return totalTasksCompleted; }
    public int getTotalTasksNotDone() { return totalTasksNotDone; }
    public int getTotalTasksCancelled() { return totalTasksCancelled; }
    public int getLongestStreak() { return longestStreak; }
    public int getCurrentStreak() { return currentStreak; }
    public int getSpecialMissionsStarted() { return specialMissionsStarted; }
    public int getSpecialMissionsCompleted() { return specialMissionsCompleted; }
    public Map<String, Integer> getTasksPerCategory() { return tasksPerCategory; }
    public long getLastActiveDate() { return lastActiveDate; }
    public int getTotalCompletedTaskDifficultySum() { return totalCompletedTaskDifficultySum; }
    public Map<String, Long> getDailyXP() { return dailyXP; }
    public Map<String, Long> getDailyDifficultySum() { return dailyDifficultySum; }
    public Map<String, Long> getDailyCompletedCount() { return dailyCompletedCount; }


    public void setActiveDays(int activeDays) { this.activeDays = activeDays; }
    public void setTotalTasksCreated(int totalTasksCreated) { this.totalTasksCreated = totalTasksCreated; }
    public void setTotalTasksCompleted(int totalTasksCompleted) { this.totalTasksCompleted = totalTasksCompleted; }
    public void setTotalTasksNotDone(int totalTasksNotDone) { this.totalTasksNotDone = totalTasksNotDone; }
    public void setTotalTasksCancelled(int totalTasksCancelled) { this.totalTasksCancelled = totalTasksCancelled; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public void setSpecialMissionsStarted(int specialMissionsStarted) { this.specialMissionsStarted = specialMissionsStarted; }
    public void setSpecialMissionsCompleted(int specialMissionsCompleted) { this.specialMissionsCompleted = specialMissionsCompleted; }
    public void setTasksPerCategory(Map<String, Integer> tasksPerCategory) { this.tasksPerCategory = tasksPerCategory; }
    public void setLastActiveDate(long lastActiveDate) { this.lastActiveDate = lastActiveDate; }
    public void setTotalCompletedTaskDifficultySum(int totalCompletedTaskDifficultySum) { this.totalCompletedTaskDifficultySum = totalCompletedTaskDifficultySum; }
    public void setDailyXP(Map<String, Long> dailyXP) { this.dailyXP = dailyXP; }
    public void setDailyDifficultySum(Map<String, Long> dailyDifficultySum) { this.dailyDifficultySum = dailyDifficultySum; }
    public void setDailyCompletedCount(Map<String, Long> dailyCompletedCount) { this.dailyCompletedCount = dailyCompletedCount; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("activeDays", activeDays);
        map.put("totalTasksCreated", totalTasksCreated);
        map.put("totalTasksCompleted", totalTasksCompleted);
        map.put("totalTasksNotDone", totalTasksNotDone);
        map.put("totalTasksCancelled", totalTasksCancelled);
        map.put("longestStreak", longestStreak);
        map.put("currentStreak", currentStreak);
        map.put("specialMissionsStarted", specialMissionsStarted);
        map.put("specialMissionsCompleted", specialMissionsCompleted);
        map.put("tasksPerCategory", tasksPerCategory);
        map.put("lastActiveDate", lastActiveDate);
        map.put("totalCompletedTaskDifficultySum", totalCompletedTaskDifficultySum);
        map.put("dailyXP", dailyXP);
        map.put("dailyDifficultySum", dailyDifficultySum);
        map.put("dailyCompletedCount", dailyCompletedCount);
        return map;
    }
}