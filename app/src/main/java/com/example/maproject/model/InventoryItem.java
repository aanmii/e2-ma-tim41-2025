package com.example.maproject.model;

public class InventoryItem {
    private String itemId;
    private String name;
    private String type; // "potion", "clothing", "weapon"
    private int quantity;
    private int remainingBattles; // Za odeću

    // Bonusi
    private int ppBonus; // Procenat povećanja PP
    private int attackSuccessBonus; // Procenat povećanja šanse za pogodak
    private int extraAttackChance; // Šansa za dodatni napad (%)
    private int coinBonus; // Procenat povećanja novčića

    private boolean isPermanent; // Za trajne napitke i oružje
    private boolean isActive; // Da li je trenutno aktivirana
    private int upgradeLevel; // Za oružje (počinje od 1)

    // Konstruktori
    public InventoryItem() {
        this.quantity = 1;
        this.upgradeLevel = 1;
        this.isActive = false;
        this.isPermanent = false;
    }

    public InventoryItem(String itemId, String name, String type, int quantity, int remainingBattles) {
        this();
        this.itemId = itemId;
        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.remainingBattles = remainingBattles;
    }

    // Getteri i setteri
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getRemainingBattles() { return remainingBattles; }
    public void setRemainingBattles(int remainingBattles) { this.remainingBattles = remainingBattles; }

    public int getPpBonus() { return ppBonus; }
    public void setPpBonus(int ppBonus) { this.ppBonus = ppBonus; }

    public int getAttackSuccessBonus() { return attackSuccessBonus; }
    public void setAttackSuccessBonus(int attackSuccessBonus) { this.attackSuccessBonus = attackSuccessBonus; }

    public int getExtraAttackChance() { return extraAttackChance; }
    public void setExtraAttackChance(int extraAttackChance) { this.extraAttackChance = extraAttackChance; }

    public int getCoinBonus() { return coinBonus; }
    public void setCoinBonus(int coinBonus) { this.coinBonus = coinBonus; }

    public boolean isPermanent() { return isPermanent; }
    public void setPermanent(boolean permanent) { isPermanent = permanent; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(int upgradeLevel) { this.upgradeLevel = upgradeLevel; }

    // Helper metode
    public String getDescription() {
        StringBuilder desc = new StringBuilder();

        if (ppBonus > 0) {
            desc.append("+").append(ppBonus).append("% PP");
        }
        if (attackSuccessBonus > 0) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("+").append(attackSuccessBonus).append("% Hit Chance");
        }
        if (extraAttackChance > 0) {
            if (desc.length() > 0) desc.append(", ");
            desc.append(extraAttackChance).append("% chance for +1 attack");
        }
        if (coinBonus > 0) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("+").append(coinBonus).append("% Coins");
        }

        if (type.equals("clothing")) {
            desc.append(" (").append(remainingBattles).append(" battles)");
        } else if (isPermanent) {
            desc.append(" (Permanent)");
        }

        return desc.toString();
    }
}