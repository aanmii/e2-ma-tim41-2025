package com.example.maproject.service;

import java.util.HashMap;
import java.util.Map;

public class LevelingService {

    private static final int BASE_XP_LEVEL_1 = 200;
    private static final int BASE_PP_LEVEL_2 = 40;
    private static final long BASE_XP_PER_TASK = 20;

    private static final Map<Integer, String> TITLES = new HashMap<>();

    static {
        TITLES.put(1, "Sparkle");
        TITLES.put(2, "Rising Star");
        TITLES.put(3, "Brilliant Mind");
        TITLES.put(4, "Master Creator");
        TITLES.put(5, "Ultimate Visionary");
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
        return TITLES.getOrDefault(level, "Sparkle");
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