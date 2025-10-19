package com.example.maproject.service;

import com.example.maproject.model.Task;
import com.example.maproject.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class LevelUpProcessor {

    private final LevelingService levelingService = new LevelingService();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final TaskService taskService = new TaskService();

    public void awardXPAndCheckLevel(User user, Task task) {
        int xpGained = taskService.calculateTotalXP(task, user);
        user.setTotalExperiencePoints(user.getTotalExperiencePoints() + xpGained);
        user.setCurrentLevelXP(levelingService.getCurrentLevelXP(user.getTotalExperiencePoints(), user.getLevel()));

        while (user.getCurrentLevelXP() >= levelingService.getXPForNextLevel(user.getLevel())) {
            levelUpUser(user);
        }

        updateUserInDatabase(user);
    }

    private void levelUpUser(User user) {
        int currentLevel = user.getLevel();
        int newLevel = currentLevel + 1;

        long requiredXP = levelingService.getXPForNextLevel(currentLevel);
        user.setCurrentLevelXP(user.getCurrentLevelXP() - requiredXP);

        user.setLevel(newLevel);

        int ppReward = levelingService.calculatePPFromLevel(newLevel) - levelingService.calculatePPFromLevel(currentLevel);
        user.setPowerPoints(user.getPowerPoints() + ppReward);

        user.setTitle(levelingService.getTitleForLevel(newLevel));
    }

    private void updateUserInDatabase(User user) {
        db.collection("users")
                .document(user.getUserId())
                .set(user.toMap(), SetOptions.merge());
    }
}
