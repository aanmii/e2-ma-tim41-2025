package com.example.maproject.service;

import com.example.maproject.model.Task;
import com.example.maproject.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class LevelUpProcessor {

    private final LevelingService levelingService = new LevelingService();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Dodeljuje XP zadatkom i proverava prelazak nivoa */
    public void awardXPAndCheckLevel(User user, Task task) {
        long xpGained = calculateXPGained(user.getLevel(), task);
        user.setTotalExperiencePoints(user.getTotalExperiencePoints() + xpGained);
        user.setCurrentLevelXP(levelingService.getCurrentLevelXP(user.getTotalExperiencePoints(), user.getLevel()));

        while (user.getCurrentLevelXP() >= levelingService.getXPForNextLevel(user.getLevel())) {
            levelUpUser(user);
        }

        updateUserInDatabase(user);
    }

    private long calculateXPGained(int userLevel, Task task) {
        long baseDifficultyXP = task.getDifficulty().ordinal() * 10L;
        long baseImportanceXP = task.getImportance().ordinal() * 5L;
        return levelingService.getBaseTaskXP() + baseDifficultyXP + baseImportanceXP;
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
