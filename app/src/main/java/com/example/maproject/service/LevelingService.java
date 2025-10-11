package com.example.maproject.service;

import java.util.HashMap;
import java.util.Map;

public class LevelingService {

    private static final int BASE_XP_LEVEL_1 = 200;
    private static final int BASE_PP_LEVEL_2 = 40;
    private static final long BASE_XP_PER_TASK = 20;

    private static final Map<Integer, String> TITLES = new HashMap<>();

    static {
        TITLES.put(1, "Početnik");
        TITLES.put(2, "Iskusni Avanturista");
        TITLES.put(3, "Majstor Navika");
        TITLES.put(4, "Šampion Volje");
        TITLES.put(5, "Apsolutni Heroj");
    }

    private long roundUpToNextHundred(double value) {
        if (value <= 0) return BASE_XP_LEVEL_1;
        return (long) Math.ceil(value / 100.0) * 100L;
    }

    private int roundToNearestWhole(double value) {
        return (int) Math.round(value);
    }

    public long getRequiredXPForLevelUp(int nextLevel) {
        if (nextLevel <= 1) return BASE_XP_LEVEL_1;

        long xpNeededForLevel = BASE_XP_LEVEL_1;

        for (int i = 2; i <= nextLevel; i++) {
            double nextXP = xpNeededForLevel * 2.0 + xpNeededForLevel / 2.0;
            xpNeededForLevel = roundUpToNextHundred(nextXP);
        }

        return xpNeededForLevel;
    }

    public int getPPRewardForLevel(int nextLevel) {
        if (nextLevel <= 1) return 0;
        if (nextLevel == 2) return BASE_PP_LEVEL_2;

        int previousPP = BASE_PP_LEVEL_2;

        for (int i = 3; i <= nextLevel; i++) {
            double nextPP = previousPP + 0.75 * previousPP;
            previousPP = roundToNearestWhole(nextPP);
        }
        return previousPP;
    }

    public String getTitleForLevel(int level) {
        return TITLES.getOrDefault(level, "Neustrašivi");
    }

    public long getBaseTaskXP() {
        return BASE_XP_PER_TASK;
    }

    public long calculateXPForImportance(int currentLevel, long baseXP) {
        if (currentLevel <= 1) return baseXP;

        long currentXP = baseXP;
        for (int i = 2; i <= currentLevel; i++) {
            double newXP = currentXP + (currentXP / 2.0);
            currentXP = roundToNearestWhole(newXP);
        }
        return currentXP;
    }

    public long calculateXPForDifficulty(int currentLevel, long baseXP) {
        return calculateXPForImportance(currentLevel, baseXP);
    }
}