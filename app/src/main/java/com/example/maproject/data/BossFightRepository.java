package com.example.maproject.data;

import com.example.maproject.model.BossFight;
import com.example.maproject.model.InventoryItem;
import com.example.maproject.model.User;
import com.example.maproject.service.LevelingService;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

                        if (random.nextInt(100) < 80){
                            InventoryItem droppedItem = generateEquipmentDrop();
                            if (droppedItem != null) {
                                fight.setItemDropped(droppedItem.getItemId());
                                fight.setItemDroppedName(droppedItem.getName());
                                addItemToInventory(user, droppedItem);
                            }
                        }

                    } else if (fight.getBossCurrentHp() <= fight.getBossMaxHp() / 2) {
                        int coins = calculateBossReward(fight.getBossLevel()) / 2;
                        coins = (int) Math.round(coins * (1 + fight.getCoinBonusPercent() / 100.0));
                        fight.setCoinsEarned(coins);
                        user.setCoins(user.getCoins() + coins);

                        if (random.nextInt(100) < 10) {
                            InventoryItem droppedItem = generateEquipmentDrop();
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

    private InventoryItem generateEquipmentDrop() {
        boolean isClothing = random.nextInt(100) < 70;

        if (isClothing) {
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

        } else {
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