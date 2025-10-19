package com.example.maproject.service;

import android.util.Log;

import com.example.maproject.model.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

public class StatisticsManagerService {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String STATS_COLLECTION = "statistics";
    private static final String USERS_COLLECTION = "users";
    private static final String TAG = "StatisticsManager";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public void initializeStatisticsForUser(String userId) {
        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);
        statsRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Map<String, Object> initialStats = new HashMap<>();
                initialStats.put("activeDays", 0L);
                initialStats.put("longestStreak", 0L);
                initialStats.put("currentStreak", 0L);
                initialStats.put("totalTasksCreated", 0L);
                initialStats.put("totalTasksCompleted", 0L);
                initialStats.put("totalTasksNotDone", 0L);
                initialStats.put("totalTasksCancelled", 0L);
                initialStats.put("totalCompletedTaskDifficultySum", 0L);
                initialStats.put("specialMissionsStarted", 0L);
                initialStats.put("specialMissionsCompleted", 0L);
                initialStats.put("lastActiveDate", System.currentTimeMillis());
                Calendar calYesterday = Calendar.getInstance();
                calYesterday.add(Calendar.DAY_OF_YEAR, -1);
                initialStats.put("lastStreakCheckDate", dateFormat.format(calYesterday.getTime()));
                Map<String, Long> placeholderMap = new HashMap<>();
                placeholderMap.put("_placeholder", 0L);
                initialStats.put("tasksPerCategory", new HashMap<>(placeholderMap));
                initialStats.put("dailyXP", new HashMap<>(placeholderMap));
                initialStats.put("dailyDifficultySum", new HashMap<>(placeholderMap));
                initialStats.put("dailyCompletedCount", new HashMap<>(placeholderMap));
                statsRef.set(initialStats, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Statistics initialized for user: " + userId);
                            removePlaceholders(userId);
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to initialize statistics", e));
            } else {
                Log.d(TAG, "Statistics already exist for user: " + userId);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error checking statistics existence", e));
    }

    private void removePlaceholders(String userId) {
        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("tasksPerCategory._placeholder", FieldValue.delete());
        updates.put("dailyXP._placeholder", FieldValue.delete());
        updates.put("dailyDifficultySum._placeholder", FieldValue.delete());
        updates.put("dailyCompletedCount._placeholder", FieldValue.delete());
        statsRef.update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Placeholders removed"))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to remove placeholders (non-critical)", e));
    }

    public void fixMissingFields(String userId) {
        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);
        statsRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                initializeStatisticsForUser(userId);
                return;
            }
            Map<String, Object> updates = new HashMap<>();
            if (!doc.contains("activeDays")) updates.put("activeDays", 0L);
            if (!doc.contains("longestStreak")) updates.put("longestStreak", 0L);
            if (!doc.contains("currentStreak")) updates.put("currentStreak", 0L);
            if (!doc.contains("totalTasksCreated")) updates.put("totalTasksCreated", 0L);
            if (!doc.contains("totalTasksCompleted")) updates.put("totalTasksCompleted", 0L);
            if (!doc.contains("totalTasksNotDone")) updates.put("totalTasksNotDone", 0L);
            if (!doc.contains("totalTasksCancelled")) updates.put("totalTasksCancelled", 0L);
            if (!doc.contains("totalCompletedTaskDifficultySum")) updates.put("totalCompletedTaskDifficultySum", 0L);
            if (!doc.contains("specialMissionsStarted")) updates.put("specialMissionsStarted", 0L);
            if (!doc.contains("specialMissionsCompleted")) updates.put("specialMissionsCompleted", 0L);
            if (!doc.contains("lastActiveDate")) updates.put("lastActiveDate", System.currentTimeMillis());
            if (!doc.contains("lastStreakCheckDate")) {
                Calendar calYesterday = Calendar.getInstance();
                calYesterday.add(Calendar.DAY_OF_YEAR, -1);
                updates.put("lastStreakCheckDate", dateFormat.format(calYesterday.getTime()));
            }
            if (!doc.contains("tasksPerCategory")) {
                Map<String, Long> placeholder = new HashMap<>();
                placeholder.put("_placeholder", 0L);
                updates.put("tasksPerCategory", placeholder);
            }
            if (!doc.contains("dailyXP")) {
                Map<String, Long> placeholder = new HashMap<>();
                placeholder.put("_placeholder", 0L);
                updates.put("dailyXP", placeholder);
            }
            if (!doc.contains("dailyDifficultySum")) {
                Map<String, Long> placeholder = new HashMap<>();
                placeholder.put("_placeholder", 0L);
                updates.put("dailyDifficultySum", placeholder);
            }
            if (!doc.contains("dailyCompletedCount")) {
                Map<String, Long> placeholder = new HashMap<>();
                placeholder.put("_placeholder", 0L);
                updates.put("dailyCompletedCount", placeholder);
            }
            if (!updates.isEmpty()) {
                statsRef.update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Missing fields added for user: " + userId);
                            removePlaceholders(userId);
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to fix missing fields", e));
            } else {
                Log.d(TAG, "All fields already present for user: " + userId);
            }
        });
    }

    public void updateStatisticsOnTaskStatusChange(String userId, Task task, Task.Status oldStatus) {
        if (task.getStatus() == null || oldStatus == null) return;
        ensureStatisticsExist(userId, () -> performStatusUpdate(userId, task, oldStatus));
    }

    private void performStatusUpdate(String userId, Task task, Task.Status oldStatus) {
        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(userId);
        Task.Status newStatus = task.getStatus();
        String categoryName = task.getCategoryName() != null ? task.getCategoryName() : "Default";
        String todayDate = dateFormat.format(new Date());
        Map<String, Object> updates = new HashMap<>();
        if (oldStatus == Task.Status.COMPLETED) {
            updates.put("totalTasksCompleted", FieldValue.increment(-1));
            updates.put("tasksPerCategory." + categoryName, FieldValue.increment(-1));
            int taskXP = computeTaskXP(task);
            long difficultyValue = task.getDifficulty() != null ? task.getDifficulty().getXp() : 0;
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
            int taskXP = computeTaskXP(task);
            long difficultyValue = task.getDifficulty() != null ? task.getDifficulty().getXp() : 0;
            updates.put("totalCompletedTaskDifficultySum", FieldValue.increment(difficultyValue));
            updates.put("dailyXP." + todayDate, FieldValue.increment(taskXP));
            updates.put("dailyDifficultySum." + todayDate, FieldValue.increment(difficultyValue));
            updates.put("dailyCompletedCount." + todayDate, FieldValue.increment(1));
            userRef.update("totalExperiencePoints", FieldValue.increment(taskXP));
        } else if (newStatus == Task.Status.NOT_DONE) {
            updates.put("totalTasksNotDone", FieldValue.increment(1));
        } else if (newStatus == Task.Status.CANCELLED) {
            updates.put("totalTasksCancelled", FieldValue.increment(1));
        }

        if (!updates.isEmpty()) {
            statsRef.update(updates)
                    .addOnSuccessListener(aVoid -> updateStreak(userId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update statistics", e));
        } else {

            updateStreak(userId);
        }
    }

    public void updateCreatedTaskCount(String userId, long increment) {
        ensureStatisticsExist(userId, () -> {
            DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("totalTasksCreated", FieldValue.increment(increment));
            statsRef.set(updates, SetOptions.merge());
        });
    }

    private void ensureStatisticsExist(String userId, Runnable onComplete) {
        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);
        statsRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                initializeStatisticsForUser(userId);
                new android.os.Handler().postDelayed(onComplete, 500);
            } else {
                onComplete.run();
            }
        });
    }

    public void updateStreak(String userId) {
        DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);

        statsRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            String todayDate = dateFormat.format(new Date());
            String lastCheckDate = doc.getString("lastStreakCheckDate");

            // read dailyCompletedCount map
            Map<String, Object> dailyCompleted;
            dailyCompleted = doc.get("dailyCompletedCount", Map.class);
            int completedToday = 0;
            if (dailyCompleted != null) {
                Object o = dailyCompleted.get(todayDate);
                if (o instanceof Number) {
                    completedToday = ((Number) o).intValue();
                } else if (o instanceof String) {
                    try {
                        completedToday = Integer.parseInt((String) o);
                    } catch (Exception ignored) {
                    }
                }
            }


           if (completedToday > 1) {
               // return;
            }

            long currentStreak = doc.getLong("currentStreak") != null ? doc.getLong("currentStreak") : 0L;
            long longestStreak = doc.getLong("longestStreak") != null ? doc.getLong("longestStreak") : 0L;
            long activeDays = doc.getLong("activeDays") != null ? doc.getLong("activeDays") : 0L;


            boolean continuedFromYesterday = false;
            try {
                if (lastCheckDate != null) {
                    Date lastCheck = dateFormat.parse(lastCheckDate);
                    Calendar calYesterday = Calendar.getInstance();
                    calYesterday.add(Calendar.DAY_OF_YEAR, -1);
                    continuedFromYesterday = lastCheck != null && isSameDay(lastCheck.getTime(), calYesterday.getTimeInMillis());
                }
            } catch (Exception ignored) {
            }

            currentStreak = continuedFromYesterday ? currentStreak + 1 : 1L;
            if (currentStreak > longestStreak) longestStreak = currentStreak;
            activeDays = activeDays + 1L;

            Map<String, Object> updates = new HashMap<>();
            updates.put("currentStreak", currentStreak);
            updates.put("longestStreak", longestStreak);
            updates.put("activeDays", activeDays);
            updates.put("lastStreakCheckDate", todayDate);
            updates.put("lastActiveDate", System.currentTimeMillis());

            statsRef.update(updates)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update streak", e));
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to read stats for streak", e));
    }

    public void updateSpecialMissionStats(String userId, boolean isStarted, boolean isCompleted) {
        ensureStatisticsExist(userId, () -> {
            DocumentReference statsRef = db.collection(STATS_COLLECTION).document(userId);
            Map<String, Object> updates = new HashMap<>();
            if (isStarted) updates.put("specialMissionsStarted", FieldValue.increment(1));
            if (isCompleted) updates.put("specialMissionsCompleted", FieldValue.increment(1));
            if (!updates.isEmpty()) statsRef.set(updates, SetOptions.merge());
        });
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
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private long getStartOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private int computeTaskXP(Task task) {
        int difficultyXp = 0;
        int importanceXp = 0;
        if (task != null) {
            if (task.getDifficulty() != null) difficultyXp = task.getDifficulty().getXp();
            if (task.getImportance() != null) importanceXp = task.getImportance().getXp();
        }
        return difficultyXp + importanceXp;
    }
}
