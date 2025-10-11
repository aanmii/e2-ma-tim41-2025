package com.example.maproject.service;

import com.example.maproject.model.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StatisticsManagerService {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String STATS_COLLECTION = "statistics";

    public void updateStatisticsOnTaskStatusChange(String userId, Task task, Task.Status oldStatus) {

        if (task.getStatus() == null || oldStatus == null) return;

        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);
        Map<String, Object> updates = new HashMap<>();

        Task.Status newStatus = task.getStatus();
        String categoryName = task.getCategoryName();

        long difficultyValue = task.getDifficulty().ordinal();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (oldStatus == Task.Status.COMPLETED) {
            updates.put("totalTasksCompleted", FieldValue.increment(-1));
            updates.put("tasksPerCategory." + categoryName, FieldValue.increment(-1));
            updates.put("totalCompletedTaskDifficultySum", FieldValue.increment(-difficultyValue));
        } else if (oldStatus == Task.Status.NOT_DONE) {
            updates.put("totalTasksNotDone", FieldValue.increment(-1));
        } else if (oldStatus == Task.Status.CANCELLED) {
            updates.put("totalTasksCancelled", FieldValue.increment(-1));
        }

        if (newStatus == Task.Status.COMPLETED) {
            updates.put("totalTasksCompleted", FieldValue.increment(1));
            updates.put("tasksPerCategory." + categoryName, FieldValue.increment(1));
            updates.put("totalCompletedTaskDifficultySum", FieldValue.increment(difficultyValue));
            updates.put("dailyDifficultySum." + todayDate, FieldValue.increment(difficultyValue));
            updates.put("dailyCompletedCount." + todayDate, FieldValue.increment(1));
        } else if (newStatus == Task.Status.NOT_DONE) {
            updates.put("totalTasksNotDone", FieldValue.increment(1));
        } else if (newStatus == Task.Status.CANCELLED) {
            updates.put("totalTasksCancelled", FieldValue.increment(1));
        }

        if (!updates.isEmpty()) {
            statsRef.set(updates, SetOptions.merge());
        }
    }

    public void updateCreatedTaskCount(String userId, long increment) {
        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("totalTasksCreated", FieldValue.increment(increment));

        updates.put("activeDays", 0L);
        updates.put("longestStreak", 0L);
        updates.put("totalTasksCompleted", 0L);
        updates.put("totalTasksNotDone", 0L);
        updates.put("totalTasksCancelled", 0L);
        updates.put("totalCompletedTaskDifficultySum", 0L);

        statsRef.set(updates, SetOptions.merge());
    }
}