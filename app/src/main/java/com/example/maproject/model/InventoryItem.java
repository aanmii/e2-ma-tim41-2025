package com.example.maproject.model;

public class InventoryItem {
    private String itemId;
    private String name;
    private String type; // potion, clothing, weapon
    private int quantity;
    private int remainingBattles; // za odeću / jednokratne napitke

    public InventoryItem() {}

    public InventoryItem(String itemId, String name, String type, int quantity, int remainingBattles) {
        this.itemId = itemId;
        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.remainingBattles = remainingBattles;
    }

    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getQuantity() { return quantity; }
    public int getRemainingBattles() { return remainingBattles; }

    public void setItemId(String itemId) { this.itemId = itemId; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setRemainingBattles(int remainingBattles) { this.remainingBattles = remainingBattles; }
}
