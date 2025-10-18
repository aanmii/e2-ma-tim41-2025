package com.example.maproject.service;

public class LevelingService {

    private static final long BASE_XP_LEVEL_1 = 200;
    private static final int BASE_PP_LEVEL_1 = 40;

    private static final long[] CUMULATIVE_XP_TABLE = new long[100];
    private static final int[] PP_TABLE = new int[100];

    private static final String[] TITLES = {
            "Sparkle",
            "Rising Star",
            "Brilliant Mind",
            "Master Creator",
            "Ultimate Visionary"
    };

    static {
        initializeTables();
    }

    private static void initializeTables() {
        CUMULATIVE_XP_TABLE[0] = 0;
        PP_TABLE[0] = 0;

        long prevXP = BASE_XP_LEVEL_1;
        int prevPP = BASE_PP_LEVEL_1;

        // Level 1
        CUMULATIVE_XP_TABLE[1] = BASE_XP_LEVEL_1;
        PP_TABLE[1] = BASE_PP_LEVEL_1;

        for (int level = 2; level < 100; level++) {
            prevXP = roundUpToNextHundred(prevXP * 2 + prevXP / 2);
            CUMULATIVE_XP_TABLE[level] = CUMULATIVE_XP_TABLE[level - 1] + prevXP;

            prevPP = (int) Math.round(prevPP + (0.75 * prevPP));
            PP_TABLE[level] = prevPP;
        }
    }

    private static long roundUpToNextHundred(double value) {
        return (long) Math.ceil(value / 100.0) * 100L;
    }

    public int calculateLevelFromXP(long totalXP) {
        for (int level = 1; level < CUMULATIVE_XP_TABLE.length; level++) {
            if (totalXP < CUMULATIVE_XP_TABLE[level]) return level;
        }
        return CUMULATIVE_XP_TABLE.length - 1;
    }

    public int calculatePPFromLevel(int level) {
        if (level <= 0) return 0;
        if (level >= PP_TABLE.length) return PP_TABLE[PP_TABLE.length - 1];
        return PP_TABLE[level];
    }

    public long getCurrentLevelXP(long totalXP, int currentLevel) {
        if (currentLevel <= 0) return totalXP;
        return totalXP - getCumulativeXPForLevel(currentLevel - 1);
    }

    public long getXPForNextLevel(int currentLevel) {
        if (currentLevel >= CUMULATIVE_XP_TABLE.length) return Long.MAX_VALUE;
        return CUMULATIVE_XP_TABLE[currentLevel] - getCumulativeXPForLevel(currentLevel - 1);
    }

    public long getCumulativeXPForLevel(int level) {
        if (level < 0) return 0;
        if (level >= CUMULATIVE_XP_TABLE.length) return CUMULATIVE_XP_TABLE[CUMULATIVE_XP_TABLE.length - 1];
        return CUMULATIVE_XP_TABLE[level];
    }

    public long getTotalXPForNextLevel(int currentLevel) {
        if (currentLevel <= 0) return CUMULATIVE_XP_TABLE[1];
        if (currentLevel >= CUMULATIVE_XP_TABLE.length - 1) return CUMULATIVE_XP_TABLE[CUMULATIVE_XP_TABLE.length - 1];
        return CUMULATIVE_XP_TABLE[currentLevel + 1];
    }

    public String getTitleForLevel(int level) {
        if (level <= 0) return TITLES[0];
        if (level >= TITLES.length) return TITLES[TITLES.length - 1];
        return TITLES[level - 1];
    }

    public long getBaseTaskXP() {
        return 50;
    }
}
