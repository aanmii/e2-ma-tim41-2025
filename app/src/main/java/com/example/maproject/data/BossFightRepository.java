package com.example.maproject.data;

import com.example.maproject.model.BossFight;
import com.example.maproject.model.InventoryItem;
import com.example.maproject.model.Task;
import com.example.maproject.model.User;
import com.example.maproject.service.LevelingService;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BossFightRepository {

    private FirebaseFirestore db;
    private LevelingService levelingService;
    private Random random;

    public BossFightRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.levelingService = new LevelingService();
        this.random = new Random();
    }
    public void prepareBossFight(String userId, OnBossFightPreparedListener listener) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null) {
                        listener.onError("User not found");
                        return;
                    }

                    // First check if there is an undefeated boss fight for this user
                    db.collection("boss_fights")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("victory", false)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(fightSnapshot -> {
                                if (!fightSnapshot.isEmpty()) {
                                    DocumentSnapshot fdoc = fightSnapshot.getDocuments().get(0);
                                    BossFight existing = new BossFight(userId, fdoc.getLong("bossLevel").intValue());

                                    // Restore HP if available in history, otherwise recalc
                                    if (fdoc.contains("bossMaxHp")) {
                                        existing.setBossMaxHp(fdoc.getLong("bossMaxHp").intValue());
                                    } else {
                                        existing.setBossMaxHp(calculateBossHp(existing.getBossLevel()));
                                    }

                                    if (fdoc.contains("bossCurrentHp")) {
                                        existing.setBossCurrentHp(fdoc.getLong("bossCurrentHp").intValue());
                                    } else {
                                        existing.setBossCurrentHp(existing.getBossMaxHp());
                                    }

                                    int userPp = user.getPowerPoints();
                                    if (userPp == 0) {
                                        userPp = levelingService.calculatePPFromLevel(levelingService.calculateLevelFromXP(user.getTotalExperiencePoints()));
                                        user.setPowerPoints(userPp);
                                        db.collection("users").document(userId)
                                                .update("powerPoints", userPp);
                                    }

                                    existing.setUserBasePp(userPp);
                                    applyEquipmentBonuses(existing, user.getActiveEquipment());

                                    // Compute player's task success rate for the current stage (last 7 days)
                                    long cut = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
                                    db.collection("tasks")
                                            .whereEqualTo("userId", userId)
                                            .whereGreaterThanOrEqualTo("createdTime", cut)
                                            .get()
                                            .addOnSuccessListener(snapshot -> {
                                                int total = 0;
                                                int completed = 0;
                                                for (DocumentSnapshot tdoc : snapshot.getDocuments()) {
                                                    Task task = tdoc.toObject(Task.class);
                                                    if (task == null) continue;
                                                    Task.Status status = task.getStatus();
                                                    if (status == Task.Status.PAUSED || status == Task.Status.CANCELLED) continue;
                                                    total++;
                                                    if (status == Task.Status.COMPLETED) completed++;
                                                }

                                                if (total > 0) {
                                                    int rate = (int) Math.round((completed * 100.0) / total);
                                                    existing.setSuccessRatePercent(rate);
                                                }

                                                listener.onPrepared(existing);
                                            })
                                            .addOnFailureListener(e -> listener.onPrepared(existing));

                                    return;
                                }

                                // No undefeated boss - create a new one for the user's level
                                int userLevel = levelingService.calculateLevelFromXP(user.getTotalExperiencePoints());
                                BossFight fight = new BossFight(userId, userLevel);

                                fight.setBossMaxHp(calculateBossHp(userLevel));
                                fight.setBossCurrentHp(fight.getBossMaxHp());

                                int userPp = user.getPowerPoints();

                                if (userPp == 0) {
                                    userPp = levelingService.calculatePPFromLevel(userLevel);
                                    user.setPowerPoints(userPp);

                                    final int finalPp = userPp;

                                    db.collection("users").document(userId)
                                            .update("powerPoints", finalPp);
                                }

                                fight.setUserBasePp(userPp);
                                applyEquipmentBonuses(fight, user.getActiveEquipment());

                                // Compute player's task success rate for the current stage (last 7 days)
                                long cut = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
                                db.collection("tasks")
                                        .whereEqualTo("userId", userId)
                                        .whereGreaterThanOrEqualTo("createdTime", cut)
                                        .get()
                                        .addOnSuccessListener(snapshot -> {
                                            int total = 0;
                                            int completed = 0;
                                            for (DocumentSnapshot tdoc : snapshot.getDocuments()) {
                                                Task task = tdoc.toObject(Task.class);
                                                if (task == null) continue;
                                                Task.Status status = task.getStatus();
                                                if (status == Task.Status.PAUSED || status == Task.Status.CANCELLED) continue;
                                                total++;
                                                if (status == Task.Status.COMPLETED) completed++;
                                            }

                                            if (total > 0) {
                                                int rate = (int) Math.round((completed * 100.0) / total);
                                                fight.setSuccessRatePercent(rate);
                                            }

                                            listener.onPrepared(fight);
                                        })
                                        .addOnFailureListener(e -> {
                                            // If tasks query fails, still return prepared fight with default rate
                                            listener.onPrepared(fight);
                                        });

                            })
                            .addOnFailureListener(e -> {
                                // If boss_fights query fails, fall back to spawning a new boss
                                int userLevel = levelingService.calculateLevelFromXP(user.getTotalExperiencePoints());
                                BossFight fight = new BossFight(userId, userLevel);

                                fight.setBossMaxHp(calculateBossHp(userLevel));
                                fight.setBossCurrentHp(fight.getBossMaxHp());

                                int userPp = user.getPowerPoints();

                                if (userPp == 0) {
                                    userPp = levelingService.calculatePPFromLevel(userLevel);
                                    user.setPowerPoints(userPp);

                                    final int finalPp = userPp;

                                    db.collection("users").document(userId)
                                            .update("powerPoints", finalPp);
                                }

                                fight.setUserBasePp(userPp);
                                applyEquipmentBonuses(fight, user.getActiveEquipment());

                                listener.onPrepared(fight);
                            });

                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    private void applyEquipmentBonuses(BossFight fight, List<InventoryItem> activeEquipment) {
        if (activeEquipment == null || activeEquipment.isEmpty()) {
            fight.setUserFinalPp(fight.getUserBasePp());
            return;
        }

        double totalPpBonusPercent = 0;
        double totalAttackBonus = 0;
        int totalExtraAttacks = 0;
        int totalCoinBonus = 0;

        for (InventoryItem item : activeEquipment) {
            switch (item.getType()) {
                case "potion":
                    totalPpBonusPercent += item.getPpBonus();
                    break;

                case "clothing":
                    if (item.getItemId().equals("gloves")) {
                        totalPpBonusPercent += 10;

                    } else if (item.getItemId().equals("shield")) {
                        totalAttackBonus += 10;

                    } else if (item.getItemId().equals("boots")) {
                        if (random.nextInt(100) < 40) {
                            totalExtraAttacks += 1;
                        }
                    }
                    break;

                case "weapon":
                    if (item.getItemId().equals("sword")) {
                        double baseBonus = 5.0;
                        double upgradeBonus = (item.getUpgradeLevel() - 1) * 0.01;
                        totalPpBonusPercent += baseBonus + upgradeBonus;

                    } else if (item.getItemId().equals("bow")) {
                        double baseBonus = 5.0;
                        double upgradeBonus = (item.getUpgradeLevel() - 1) * 0.01;
                        totalAttackBonus += baseBonus + upgradeBonus;
                    }
                    break;
            }
        }

        int finalPp = fight.getUserBasePp();
        if (totalPpBonusPercent > 0) {
            finalPp = (int) Math.round(finalPp * (1 + totalPpBonusPercent / 100.0));
        }

        fight.setUserFinalPp(finalPp);
        fight.setAttackSuccessBonus((int) Math.round(totalAttackBonus));
        fight.setExtraAttacks(totalExtraAttacks);
        fight.setCoinBonusPercent(totalCoinBonus);
    }

    public boolean performAttack(BossFight fight) {
        if (!fight.hasAttacksRemaining() || fight.isBossDefeated()) {
            return false;
        }

        int successChance = fight.getSuccessChance();
        boolean hit = random.nextInt(100) < successChance;

        if (hit) {
            int newHp = Math.max(0, fight.getBossCurrentHp() - fight.getUserFinalPp());
            fight.setBossCurrentHp(newHp);
        }

        fight.setRemainingAttacks(fight.getRemainingAttacks() - 1);
        return hit;
    }

    public void completeBossFight(String userId, BossFight fight, OnFightCompletedListener listener) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null) {
                        listener.onError("User not found");
                        return;
                    }

                    boolean victory = fight.isBossDefeated();
                    fight.setCompleted(true);
                    fight.setVictory(victory);

                    int permanentPpIncrease = applyPermanentPotions(user);
                    if (permanentPpIncrease > 0) {
                        user.setPowerPoints(user.getPowerPoints() + permanentPpIncrease);
                    }

                    processEquipmentAfterFight(user);

                    if (victory) {
                        int coins = calculateBossReward(fight.getBossLevel());
                        coins = (int) Math.round(coins * (1 + fight.getCoinBonusPercent() / 100.0));
                        fight.setCoinsEarned(coins);
                        user.setCoins(user.getCoins() + coins);

                        // Spec: 20% chance to receive an item. Within that: 95% clothing, 5% weapon.
                        int dropRoll = random.nextInt(100);
                        if (dropRoll < 20) { // 20% base chance
                            int kindRoll = random.nextInt(100);
                            InventoryItem droppedItem;
                            if (kindRoll < 95) {
                                droppedItem = generateClothingDrop();
                            } else {
                                droppedItem = generateWeaponDrop();
                            }

                            if (droppedItem != null) {
                                fight.setItemDropped(droppedItem.getItemId());
                                fight.setItemDroppedName(droppedItem.getName());
                                addItemToInventory(user, droppedItem);
                            }
                        }

                    } else if (fight.getBossCurrentHp() <= fight.getBossMaxHp() / 2) {
                        // Boss not defeated but lost at least 50% HP
                        int coins = calculateBossReward(fight.getBossLevel()) / 2;
                        coins = (int) Math.round(coins * (1 + fight.getCoinBonusPercent() / 100.0));
                        fight.setCoinsEarned(coins);
                        user.setCoins(user.getCoins() + coins);

                        int dropRoll = random.nextInt(100);
                        if (dropRoll < 10) { // half of 20% = 10%
                            int kindRoll = random.nextInt(100);
                            InventoryItem droppedItem;
                            if (kindRoll < 95) {
                                droppedItem = generateClothingDrop();
                            } else {
                                droppedItem = generateWeaponDrop();
                            }

                            if (droppedItem != null) {
                                fight.setItemDropped(droppedItem.getItemId());
                                fight.setItemDroppedName(droppedItem.getName());
                                addItemToInventory(user, droppedItem);
                            }
                        }
                    } else {
                        fight.setCoinsEarned(0);
                    }

                    saveFightHistory(fight);

                    db.collection("users").document(userId)
                            .set(user.toMap())
                            .addOnSuccessListener(aVoid -> {
                                listener.onCompleted(fight);
                            })
                            .addOnFailureListener(e -> {
                                listener.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    private int applyPermanentPotions(User user) {
        List<InventoryItem> activeEquipment = user.getActiveEquipment();
        if (activeEquipment == null) return 0;

        int totalPermanentIncrease = 0;
        List<InventoryItem> updatedActive = new ArrayList<>();

        for (InventoryItem item : activeEquipment) {
            if (item.getType().equals("potion") && item.isPermanent()) {
                int currentBasePp = user.getPowerPoints();
                int increase = (int) Math.round(currentBasePp * item.getPpBonus() / 100.0);
                totalPermanentIncrease += increase;
                continue;
            }

            updatedActive.add(item);
        }

        user.setActiveEquipment(updatedActive);
        return totalPermanentIncrease;
    }

    private void processEquipmentAfterFight(User user) {
        List<InventoryItem> activeEquipment = user.getActiveEquipment();
        if (activeEquipment == null) return;

        List<InventoryItem> updatedActive = new ArrayList<>();

        for (InventoryItem item : activeEquipment) {
            if (item.getType().equals("potion")) {
                if (!item.isPermanent()) {
                    continue;
                }
            } else if (item.getType().equals("clothing")) {
                item.setRemainingBattles(item.getRemainingBattles() - 1);
                if (item.getRemainingBattles() > 0) {
                    updatedActive.add(item);
                }

            } else if (item.getType().equals("weapon")) {
                updatedActive.add(item);
            }
        }

        user.setActiveEquipment(updatedActive);
    }

    private InventoryItem generateClothingDrop() {
        String[] clothingIds = {"gloves", "shield", "boots"};
        String id = clothingIds[random.nextInt(clothingIds.length)];

        InventoryItem item = new InventoryItem();
        item.setItemId(id);
        item.setType("clothing");
        item.setQuantity(1);
        item.setRemainingBattles(2);
        item.setUpgradeLevel(1);

        switch (id) {
            case "gloves":
                item.setName("Power Gloves");
                item.setPpBonus(10);
                break;
            case "shield":
                item.setName("Magic Shield");
                item.setAttackSuccessBonus(10);
                break;
            case "boots":
                item.setName("Speed Boots");
                item.setExtraAttackChance(40);
                break;
        }
        return item;
    }

    private InventoryItem generateWeaponDrop() {
        String[] weaponIds = {"sword", "bow"};
        String id = weaponIds[random.nextInt(weaponIds.length)];

        InventoryItem item = new InventoryItem();
        item.setItemId(id);
        item.setType("weapon");
        item.setQuantity(1);
        item.setUpgradeLevel(1);
        item.setPermanent(true);

        if (id.equals("sword")) {
            item.setName("Mighty Sword");
            item.setPpBonus(5);
        } else {
            item.setName("Bow & Arrow");
            item.setAttackSuccessBonus(5);
        }
        return item;
    }

    private void addItemToInventory(User user, InventoryItem newItem) {
        List<InventoryItem> equipment = user.getEquipment();
        if (equipment == null) equipment = new ArrayList<>();

        boolean found = false;
        for (InventoryItem existing : equipment) {
            if (existing.getItemId().equals(newItem.getItemId())) {
                if (newItem.getType().equals("weapon")) {
                    existing.setUpgradeLevel(existing.getUpgradeLevel() + 2);
                } else {
                    existing.setQuantity(existing.getQuantity() + 1);
                }
                found = true;
                break;
            }
        }

        if (!found) {
            equipment.add(newItem);
        }

        user.setEquipment(equipment);
    }

    private void saveFightHistory(BossFight fight) {
        Map<String, Object> fightData = new HashMap<>();
        fightData.put("userId", fight.getUserId());
        fightData.put("bossLevel", fight.getBossLevel());
        fightData.put("victory", fight.isVictory());
        fightData.put("coinsEarned", fight.getCoinsEarned());
        fightData.put("itemDropped", fight.getItemDropped());
        fightData.put("timestamp", fight.getTimestamp());
        fightData.put("bossMaxHp", fight.getBossMaxHp());
        fightData.put("bossCurrentHp", fight.getBossCurrentHp());

        db.collection("boss_fights")
                .document(fight.getFightId())
                .set(fightData);
    }

    private int calculateBossHp(int level) {
        if (level <= 1) return 200;
        int hp = 200;
        for (int i = 2; i <= level; i++) {
            hp = (int) (hp * 2.5);
        }
        return hp;
    }

    private int calculateBossReward(int level) {
        if (level <= 1) return 200;
        int reward = 200;
        for (int i = 2; i <= level; i++) {
            reward = (int) (reward * 1.2);
        }
        return reward;
    }

    public interface OnBossFightPreparedListener {
        void onPrepared(BossFight fight);
        void onError(String error);
    }

    public interface OnFightCompletedListener {
        void onCompleted(BossFight fight);
        void onError(String error);
    }
}

