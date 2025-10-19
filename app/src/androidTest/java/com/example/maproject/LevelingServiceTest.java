package com.example.maproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.maproject.model.Task;
import com.example.maproject.model.User;
import com.example.maproject.service.LevelUpProcessor;

import org.junit.Before;
import org.junit.Test;

public class LevelingServiceTest {

    private LevelUpProcessor processor;
    private User user;

    @Before
    public void setup() {
        processor = new LevelUpProcessor();

        // Test user
        user = new User();
        user.setUserId("user123");
        user.setLevel(1);
        user.setTotalExperiencePoints(0);
        user.setCurrentLevelXP(0);
        user.setPowerPoints(0);
        user.setTitle("Sparkle");
    }

    @Test
    public void testSingleTaskXP() {
        Task task = new Task();
        task.setDifficulty(Task.Difficulty.HARD);        // 7 XP
        task.setImportance(Task.Importance.IMPORTANT);  // 3 XP

        processor.awardXPAndCheckLevel(user, task);

        // XP
        assertEquals(10, user.getCurrentLevelXP());
        assertEquals(10, user.getTotalExperiencePoints());

        // Level should stay 1
        assertEquals(1, user.getLevel());

        // PowerPoints should stay 0 because user hasn't leveled up
        assertEquals(0, user.getPowerPoints());

        // Title should stay Sparkle
        assertEquals("Sparkle", user.getTitle());
    }

    @Test
    public void testLevelUp() {
        Task task = new Task();
        task.setDifficulty(Task.Difficulty.EXTREMELY_HARD); // 20 XP
        task.setImportance(Task.Importance.SPECIAL);       // 100 XP

        // Simulate completions to exceed level 1 XP (200)
        while (user.getTotalExperiencePoints() < 200) {
            processor.awardXPAndCheckLevel(user, task);
        }

        // Total XP should now exceed 200 -> level up
        assertTrue(user.getTotalExperiencePoints() >= 200);

        // Level should now be at least 2
        assertTrue(user.getLevel() >= 2);

        // PP should now be > 0 after leveling up
        assertTrue(user.getPowerPoints() > 0);

        // Title should have changed
        assertNotEquals("Sparkle", user.getTitle());
    }
}
