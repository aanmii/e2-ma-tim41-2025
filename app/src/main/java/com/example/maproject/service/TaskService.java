package com.example.maproject.service;

import com.example.maproject.model.Task;
import com.example.maproject.model.User;

import java.util.concurrent.TimeUnit;

public class TaskService {

    private static final long THREE_DAYS_MILLIS = TimeUnit.DAYS.toMillis(3);

    public int calculateTotalXP(Task task, User user) {
        long baseDifficultyXP = task.getDifficulty().getXp();
        long baseImportanceXP = task.getImportance().getXp();

        LevelingService levelingService = new LevelingService();
        long bonusDifficultyXP = levelingService.getDifficultyBonusXPForLevel(user.getLevel());
        long bonusImportanceXP = levelingService.getImportanceBonusXPForLevel(user.getLevel());

        return (int) (baseDifficultyXP + baseImportanceXP + bonusDifficultyXP + bonusImportanceXP);
    }

    public boolean canBeMarkedDone(Task task, long nowMillis) {
        if (task.getStatus() != Task.Status.ACTIVE) return false;
        if (nowMillis < task.getExecutionTime()) return false;
        return nowMillis <= task.getExecutionTime() + THREE_DAYS_MILLIS;
    }

    public boolean markDone(Task task, long nowMillis, String userId, StatisticsManagerService statsManager) {
        if (!canBeMarkedDone(task, nowMillis)) return false;

        Task.Status oldStatus = task.getStatus();
        task.setStatus(Task.Status.COMPLETED);
        task.setCompletedTime(nowMillis);
        statsManager.updateStatisticsOnTaskStatusChange(userId, task, oldStatus);

        return true;
    }

    public boolean markCancelled(Task task, long nowMillis, String userId, StatisticsManagerService statsManager) {
        if (task.getStatus() != Task.Status.ACTIVE) return false;

        Task.Status oldStatus = task.getStatus();
        task.setStatus(Task.Status.CANCELLED);
        task.setCompletedTime(nowMillis);

        statsManager.updateStatisticsOnTaskStatusChange(userId, task, oldStatus);

        return true;
    }

    public boolean markPaused(Task task) {
        if (task.getStatus() != Task.Status.ACTIVE || !task.isRecurring()) return false;
        task.setStatus(Task.Status.PAUSED);
        return true;
    }

    public boolean reactivate(Task task) {
        if (task.getStatus() != Task.Status.PAUSED) return false;
        task.setStatus(Task.Status.ACTIVE);
        return true;
    }

    public boolean expireIfPastUpdateWindow(Task task, long nowMillis) {
        if (task.getStatus() != Task.Status.ACTIVE) return false;
        if (nowMillis > task.getExecutionTime() + THREE_DAYS_MILLIS) {
            task.setStatus(Task.Status.NOT_DONE);
            return true;
        }
        return false;
    }
}
