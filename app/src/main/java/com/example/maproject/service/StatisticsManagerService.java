package com.example.maproject.service;

import android.util.Log;

import com.example.maproject.model.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StatisticsManagerService {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String STATS_COLLECTION = "statistics";
    private static final String USERS_COLLECTION = "users";
    private static final String TAG = "StatisticsManager";


    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());


    public void updateStatisticsOnTaskStatusChange(String userId, Task task, Task.Status oldStatus) {
        if (task.getStatus() == null || oldStatus == null) return;

        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(userId);

        Task.Status newStatus = task.getStatus();
        String categoryName = task.getCategoryName();
        String todayDate = dateFormat.format(new Date());

        updateActiveDays(userId);

        Map<String, Object> updates = new HashMap<>();

        if (oldStatus == Task.Status.COMPLETED) {
            updates.put("totalTasksCompleted", FieldValue.increment(-1));
            updates.put("tasksPerCategory." + categoryName, FieldValue.increment(-1));

            int taskXP = task.getTotalXP();
            long difficultyValue = task.getDifficulty().ordinal();

            updates.put("totalCompletedTaskDifficultySum", FieldValue.increment(-difficultyValue));
            updates.put("dailyXP." + todayDate, FieldValue.increment(-taskXP));
            updates.put("dailyDifficultySum." + todayDate, FieldValue.increment(-difficultyValue));
            updates.put("dailyCompletedCount." + todayDate, FieldValue.increment(-1));

            userRef.update("totalExperiencePoints", FieldValue.increment(-taskXP));

        } else if (oldStatus == Task.Status.NOT_DONE) {
            updates.put("totalTasksNotDone", FieldValue.increment(-1));
        } else if (oldStatus == Task.Status.CANCELLED) {
            updates.put("totalTasksCancelled", FieldValue.increment(-1));
        }


        if (newStatus == Task.Status.COMPLETED) {
            updates.put("totalTasksCompleted", FieldValue.increment(1));
            updates.put("tasksPerCategory." + categoryName, FieldValue.increment(1));


            int taskXP = task.getTotalXP();
            long difficultyValue = task.getDifficulty().ordinal();

            updates.put("totalCompletedTaskDifficultySum", FieldValue.increment(difficultyValue));
            updates.put("dailyXP." + todayDate, FieldValue.increment(taskXP));
            updates.put("dailyDifficultySum." + todayDate, FieldValue.increment(difficultyValue));
            updates.put("dailyCompletedCount." + todayDate, FieldValue.increment(1));


            userRef.update("totalExperiencePoints", FieldValue.increment(taskXP));


            updateStreak(userId);

        } else if (newStatus == Task.Status.NOT_DONE) {
            updates.put("totalTasksNotDone", FieldValue.increment(1));
            updateStreak(userId);
        } else if (newStatus == Task.Status.CANCELLED) {
            updates.put("totalTasksCancelled", FieldValue.increment(1));
        }

        if (!updates.isEmpty()) {
            statsRef.set(updates, SetOptions.merge())
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update statistics", e));
        }
    }


    public void updateCreatedTaskCount(String userId, long increment) {
        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("totalTasksCreated", FieldValue.increment(increment));

        statsRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Map<String, Object> initialStats = new HashMap<>();
                initialStats.put("activeDays", 0L);
                initialStats.put("longestStreak", 0L);
                initialStats.put("currentStreak", 0L);
                initialStats.put("totalTasksCreated", increment);
                initialStats.put("totalTasksCompleted", 0L);
                initialStats.put("totalTasksNotDone", 0L);
                initialStats.put("totalTasksCancelled", 0L);
                initialStats.put("totalCompletedTaskDifficultySum", 0L);
                initialStats.put("specialMissionsStarted", 0L);
                initialStats.put("specialMissionsCompleted", 0L);
                initialStats.put("lastActiveDate", System.currentTimeMillis());
                initialStats.put("lastStreakCheckDate", dateFormat.format(new Date()));

                statsRef.set(initialStats, SetOptions.merge());
            } else {
                statsRef.set(updates, SetOptions.merge());
            }
        });
    }


    public void updateActiveDays(String userId) {
        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);

        statsRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            Long lastActiveTimestamp = doc.getLong("lastActiveDate");
            if (lastActiveTimestamp == null) {
                lastActiveTimestamp = System.currentTimeMillis();
            }

            long currentTime = System.currentTimeMillis();
            long daysDifference = TimeUnit.MILLISECONDS.toDays(currentTime - lastActiveTimestamp);

            if (daysDifference >= 1) {
                Map<String, Object> updates = new HashMap<>();

                if (daysDifference == 1) {

                    updates.put("activeDays", FieldValue.increment(1));
                } else if (daysDifference > 1) {
                    updates.put("activeDays", 1L);
                }

                updates.put("lastActiveDate", currentTime);
                statsRef.update(updates);
            }
        });
    }


    public void updateStreak(String userId) {
        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);

        statsRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            String todayDate = dateFormat.format(new Date());
            String lastCheckDate = doc.getString("lastStreakCheckDate");

            if (todayDate.equals(lastCheckDate)) {

                return;
            }

            db.collection("tasks")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int completedToday = 0;
                        int notDoneToday = 0;

                        for (DocumentSnapshot taskDoc : querySnapshot.getDocuments()) {
                            Long executionTime = taskDoc.getLong("executionTime");
                            String status = taskDoc.getString("status");

                            if (executionTime != null && isSameDay(executionTime, System.currentTimeMillis())) {
                                if ("COMPLETED".equals(status)) {
                                    completedToday++;
                                } else if ("NOT_DONE".equals(status)) {
                                    notDoneToday++;
                                }
                            }
                        }

                        Long currentStreak = doc.getLong("currentStreak");
                        Long longestStreak = doc.getLong("longestStreak");

                        if (currentStreak == null) currentStreak = 0L;
                        if (longestStreak == null) longestStreak = 0L;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("lastStreakCheckDate", todayDate);

                        if (notDoneToday > 0) {

                            currentStreak = 0L;
                        } else if (completedToday > 0) {

                            currentStreak++;

                            if (currentStreak > longestStreak) {
                                longestStreak = currentStreak;
                                updates.put("longestStreak", longestStreak);
                            }
                        }


                        updates.put("currentStreak", currentStreak);
                        statsRef.update(updates);
                    });
        });
    }


    public void updateSpecialMissionStats(String userId, boolean isStarted, boolean isCompleted) {
        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);

        Map<String, Object> updates = new HashMap<>();

        if (isStarted) {
            updates.put("specialMissionsStarted", FieldValue.increment(1));
        }

        if (isCompleted) {
            updates.put("specialMissionsCompleted", FieldValue.increment(1));
        }

        if (!updates.isEmpty()) {
            statsRef.set(updates, SetOptions.merge());
        }
    }


    public List<String> getLastNDays(int n) {
        List<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        for (int i = n - 1; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            dates.add(dateFormat.format(calendar.getTime()));
        }

        return dates;
    }

    private boolean isSameDay(long timestamp1, long timestamp2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp1);
        cal2.setTimeInMillis(timestamp2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}