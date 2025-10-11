package com.example.maproject.service;

import com.example.maproject.model.Task;
import com.example.maproject.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class LevelUpProcessor {

    private final LevelingService levelingService = new LevelingService();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void awardXPAndCheckLevel(User user, Task task) {
        long xpGained = calculateXPGained(user.getLevel(), task);

        user.setCurrentLevelXP(user.getCurrentLevelXP() + xpGained);
        user.setTotalExperiencePoints(user.getTotalExperiencePoints() + xpGained);

        while (user.getCurrentLevelXP() >= levelingService.getRequiredXPForLevelUp(user.getLevel() + 1)) {
            levelUpUser(user);
        }

        updateUserInDatabase(user);
    }

    private long calculateXPGained(int userLevel, Task task) {
        long difficultyOrdinal = task.getDifficulty().ordinal();
        long importanceOrdinal = task.getImportance().ordinal();

        long difficultyBaseXP = difficultyOrdinal * 10;
        long importanceBaseXP = importanceOrdinal * 5;

        long scaledDifficultyXP = levelingService.calculateXPForDifficulty(userLevel, difficultyBaseXP);
        long scaledImportanceXP = levelingService.calculateXPForImportance(userLevel, importanceBaseXP);

        return levelingService.getBaseTaskXP() +
                scaledDifficultyXP +
                scaledImportanceXP;
    }

    private void levelUpUser(User user) {
        int newLevel = user.getLevel() + 1;
        long requiredXP = levelingService.getRequiredXPForLevelUp(newLevel);

        user.setCurrentLevelXP(user.getCurrentLevelXP() - requiredXP);
        user.setLevel(newLevel);

        int ppReward = levelingService.getPPRewardForLevel(newLevel);
        user.setPowerPoints(user.getPowerPoints() + ppReward);
        user.setTitle(levelingService.getTitleForLevel(newLevel));
    }

    private void updateUserInDatabase(User user) {
        db.collection("users").document(user.getUserId()).set(user.toMap(), SetOptions.merge());
    }
}