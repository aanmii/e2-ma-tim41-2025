package com.example.maproject.model;

import java.util.HashMap;
import java.util.Map;

public class UserStatistics {
    private int activeDays;                    // Broj dana aktivnog korišćenja
    private int totalTasksCreated;             // Ukupno kreiranih zadataka
    private int totalTasksCompleted;           // Ukupno završenih zadataka
    private int totalTasksNotDone;             // Ukupno neurađenih zadataka
    private int totalTasksCancelled;           // Ukupno otkazanih zadataka
    private int longestStreak;                 // Najduži niz uspešnih zadataka
    private int currentStreak;                 // Trenutni niz
    private int specialMissionsStarted;        // Započete specijalne misije
    private int specialMissionsCompleted;      // Završene specijalne misije
    private Map<String, Integer> tasksPerCategory; // Zadaci po kategoriji
    private long lastActiveDate;               // Poslednji datum aktivnosti

    public UserStatistics() {
        this.tasksPerCategory = new HashMap<>();
        this.lastActiveDate = System.currentTimeMillis();
    }

    // Getteri
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

    // Setteri
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

    // Konverzija u Map za Firestore
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
        return map;
    }
}