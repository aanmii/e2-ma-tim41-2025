package com.example.maproject.data;

import android.util.Log;

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

    /**
     * Priprema novu borbu sa bosom
     */
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

                    // Postavi osnovne vrednosti
                    fight.setBossMaxHp(calculateBossHp(userLevel));
                    fight.setBossCurrentHp(fight.getBossMaxHp());
                    fight.setUserBasePp(levelingService.calculatePPFromLevel(userLevel));

                    // Primeni bonuse od aktivne opreme
                    applyEquipmentBonuses(fight, user.getActiveEquipment());

                    listener.onPrepared(fight);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Primenjuje bonuse od aktivne opreme - sabira sve bonuse od iste vrste opreme
     */
    private void applyEquipmentBonuses(BossFight fight, List<InventoryItem> activeEquipment) {
        if (activeEquipment == null || activeEquipment.isEmpty()) {
            fight.setUserFinalPp(fight.getUserBasePp());
            return;
        }

        int totalPpBonusPercent = 0;
        int totalAttackBonus = 0;
        int totalExtraAttacks = 0;
        int totalCoinBonus = 0;

        // Brojači za svaki tip opreme (za logging/debug)
        int potionCount = 0;
        int gloveCount = 0;
        int shieldCount = 0;
        int bootCount = 0;
        int weaponCount = 0;

        for (InventoryItem item : activeEquipment) {
            switch (item.getType()) {
                case "potion":
                    // NAPICI - dodaj PP bonus (može biti 20%, 40%, 5%, 10%)
                    totalPpBonusPercent += item.getPpBonus();
                    potionCount++;
                    Log.d("BossFight", "Potion activated: +" + item.getPpBonus() + "% PP");
                    break;

                case "clothing":
                    if (item.getItemId().equals("gloves")) {
                        // RUKAVICE: +10% PP po paru
                        totalPpBonusPercent += 10;
                        gloveCount++;
                        Log.d("BossFight", "Gloves activated: +10% PP");

                    } else if (item.getItemId().equals("shield")) {
                        // ŠTIT: +10% šanse za uspešan napad po komadu
                        totalAttackBonus += 10;
                        shieldCount++;
                        Log.d("BossFight", "Shield activated: +10% hit chance");

                    } else if (item.getItemId().equals("boots")) {
                        // ČIZME: 40% šansa za +1 napad PO PARU čizama
                        if (random.nextInt(100) < 40) {
                            totalExtraAttacks += 1;
                            Log.d("BossFight", "Boots activated: +1 extra attack!");
                        } else {
                            Log.d("BossFight", "Boots activated but no extra attack (40% chance failed)");
                        }
                        bootCount++;
                    }
                    break;

                case "weapon":
                    if (item.getItemId().equals("sword")) {
                        // MAČ: +5% PP (base) + upgrade bonus
                        int swordBonus = 5;
                        double upgradeBonus = (item.getUpgradeLevel() - 1) * 0.02;
                        totalPpBonusPercent += swordBonus;
                        totalPpBonusPercent += upgradeBonus;
                        weaponCount++;
                        Log.d("BossFight", "Sword activated: +" + swordBonus + "% PP (+" + upgradeBonus + "% from upgrades)");

                    } else if (item.getItemId().equals("bow")) {
                        // LUK: +5% novčića (base) + upgrade bonus
                        int bowBonus = 5;
                        double upgradeBonus = (item.getUpgradeLevel() - 1) * 0.02;
                        totalCoinBonus += bowBonus;
                        totalCoinBonus += upgradeBonus;
                        weaponCount++;
                        Log.d("BossFight", "Bow activated: +" + bowBonus + "% coins (+" + upgradeBonus + "% from upgrades)");
                    }
                    break;
            }
        }

        // Primeni PP bonus
        int finalPp = fight.getUserBasePp();
        if (totalPpBonusPercent > 0) {
            finalPp = (int) Math.round(finalPp * (1 + totalPpBonusPercent / 100.0));
        }

        fight.setUserFinalPp(finalPp);
        fight.setAttackSuccessBonus(totalAttackBonus);
        fight.setExtraAttacks(totalExtraAttacks);
        fight.setCoinBonusPercent(totalCoinBonus);

        // Log ukupnih bonusa
        Log.d("BossFight", "=== TOTAL EQUIPMENT BONUSES ===");
        Log.d("BossFight", "Active items: " + activeEquipment.size());
        Log.d("BossFight", "Potions: " + potionCount + ", Gloves: " + gloveCount +
                ", Shields: " + shieldCount + ", Boots: " + bootCount + ", Weapons: " + weaponCount);
        Log.d("BossFight", "Base PP: " + fight.getUserBasePp() + " → Final PP: " + finalPp +
                " (+" + totalPpBonusPercent + "%)");
        Log.d("BossFight", "Attack Success Bonus: +" + totalAttackBonus + "%");
        Log.d("BossFight", "Extra Attacks: +" + totalExtraAttacks);
        Log.d("BossFight", "Coin Bonus: +" + totalCoinBonus + "%");
        Log.d("BossFight", "================================");
    }
    /**
     * Izvršava jedan napad na bosa
     * @return true ako je napad uspeo, false ako je promašaj
     */
    public boolean performAttack(BossFight fight) {
        if (!fight.hasAttacksRemaining() || fight.isBossDefeated()) {
            return false;
        }

        // Proveri da li napad pogađa (bazna šansa + bonus)
        int successChance = fight.getSuccessChance();
        boolean hit = random.nextInt(100) < successChance;

        if (hit) {
            // Napad pogađa - oduzmi PP od HP bosa
            int newHp = Math.max(0, fight.getBossCurrentHp() - fight.getUserFinalPp());
            fight.setBossCurrentHp(newHp);
        }

        // Smanji broj preostalih napada
        fight.setRemainingAttacks(fight.getRemainingAttacks() - 1);

        return hit;
    }

    /**
     * Završava borbu i primenjuje nagrade/kazne
     */
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

                    processEquipmentAfterFight(user);

                    if (victory) {

                        int coins = calculateBossReward(fight.getBossLevel());
                        coins = (int) Math.round(coins * (1 + fight.getCoinBonusPercent() / 100.0));
                        fight.setCoinsEarned(coins);
                        user.setCoins(user.getCoins() + coins);

                        if (random.nextInt(100) < 20) {
                            InventoryItem droppedItem = generateEquipmentDrop();
                            if (droppedItem != null) {
                                fight.setItemDropped(droppedItem.getItemId());
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
                                addItemToInventory(user, droppedItem);
                            }
                        }
                    } else {

                        fight.setCoinsEarned(0);
                    }


                    saveFightHistory(fight);


                    db.collection("users").document(userId)
                            .set(user)
                            .addOnSuccessListener(aVoid -> listener.onCompleted(fight))
                            .addOnFailureListener(e -> listener.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
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

                updatedActive.add(item);

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

        boolean isClothing = random.nextInt(100) < 95;

        if (isClothing) {
            String[] clothingIds = {"gloves", "shield", "boots"};
            String id = clothingIds[random.nextInt(clothingIds.length)];

            InventoryItem item = new InventoryItem();
            item.setItemId(id);
            item.setType("clothing");
            item.setQuantity(1);
            item.setRemainingBattles(2);

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
                item.setCoinBonus(5);
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
                existing.setQuantity(existing.getQuantity() + 1);
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
            hp = (int) (hp * 2 + hp / 2.0);
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