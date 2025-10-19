package com.example.maproject.model;

import java.util.Date;

public class BossFight {
    private String fightId;
    private String userId;
    private int bossLevel;
    private int bossMaxHp;
    private int bossCurrentHp;
    private int userBasePp;
    private int userFinalPp; // Nakon opreme
    private int attackSuccessBonus; // Od štita (%)
    private int extraAttacks; // Od čizama
    private int coinBonusPercent; // Od luka (%)
    private int totalAttacks; // 5 + extra
    private int remainingAttacks;
    private boolean isCompleted;
    private boolean isVictory;
    private int coinsEarned;
    private String itemDropped; // ID opreme ako je dropovana
    private long timestamp;

    private String itemDroppedName;
    private int itemDroppedImage;

    // Player success rate determined from tasks in the stage (0-100)
    private int successRatePercent = 67; // default fallback

    public BossFight() {
        this.timestamp = System.currentTimeMillis();
        this.isCompleted = false;
        this.isVictory = false;
        this.totalAttacks = 5;
        this.remainingAttacks = 5;
    }

    public BossFight(String userId, int bossLevel) {
        this();
        this.userId = userId;
        this.bossLevel = bossLevel;
        this.fightId = userId + "_" + bossLevel + "_" + timestamp;
    }

    public String getFightId() { return fightId; }
    public void setFightId(String fightId) { this.fightId = fightId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getBossLevel() { return bossLevel; }
    public void setBossLevel(int bossLevel) { this.bossLevel = bossLevel; }

    public int getBossMaxHp() { return bossMaxHp; }
    public void setBossMaxHp(int bossMaxHp) { this.bossMaxHp = bossMaxHp; }

    public int getBossCurrentHp() { return bossCurrentHp; }
    public void setBossCurrentHp(int bossCurrentHp) { this.bossCurrentHp = bossCurrentHp; }

    public int getUserBasePp() { return userBasePp; }
    public void setUserBasePp(int userBasePp) { this.userBasePp = userBasePp; }

    public int getUserFinalPp() { return userFinalPp; }
    public void setUserFinalPp(int userFinalPp) { this.userFinalPp = userFinalPp; }

    public int getAttackSuccessBonus() { return attackSuccessBonus; }
    public void setAttackSuccessBonus(int attackSuccessBonus) { this.attackSuccessBonus = attackSuccessBonus; }

    public int getExtraAttacks() { return extraAttacks; }
    public void setExtraAttacks(int extraAttacks) {
        this.extraAttacks = extraAttacks;
        this.totalAttacks = 5 + extraAttacks;
        this.remainingAttacks = this.totalAttacks;
    }

    public int getCoinBonusPercent() { return coinBonusPercent; }
    public void setCoinBonusPercent(int coinBonusPercent) { this.coinBonusPercent = coinBonusPercent; }

    public int getTotalAttacks() { return totalAttacks; }
    public void setTotalAttacks(int totalAttacks) { this.totalAttacks = totalAttacks; }

    public int getRemainingAttacks() { return remainingAttacks; }
    public void setRemainingAttacks(int remainingAttacks) { this.remainingAttacks = remainingAttacks; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public boolean isVictory() { return isVictory; }
    public void setVictory(boolean victory) { isVictory = victory; }

    public int getCoinsEarned() { return coinsEarned; }
    public void setCoinsEarned(int coinsEarned) { this.coinsEarned = coinsEarned; }

    public String getItemDropped() { return itemDropped; }
    public void setItemDropped(String itemDropped) { this.itemDropped = itemDropped; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Helper metode
    public boolean isBossDefeated() {
        return bossCurrentHp <= 0;
    }

    public boolean hasAttacksRemaining() {
        return remainingAttacks > 0;
    }

    public int getSuccessRatePercent() { return successRatePercent; }
    public void setSuccessRatePercent(int successRatePercent) { this.successRatePercent = successRatePercent; }

    public int getSuccessChance() {
        int base = Math.min(100, successRatePercent + attackSuccessBonus);
        return base;
    }

    public String getItemDroppedName() { return itemDroppedName; }
    public void setItemDroppedName(String itemDroppedName) { this.itemDroppedName = itemDroppedName; }

    public int getItemDroppedImage() { return itemDroppedImage; }
    public void setItemDroppedImage(int itemDroppedImage) { this.itemDroppedImage = itemDroppedImage; }



}