// Item.java
package com.example.maproject.model;

public class Item {
    public enum Type { POTION, CLOTHING, WEAPON }

    private String id;
    private String name;
    private Type type;
    private String description;
    private int price;
    private int effectPercent;
    private boolean isPermanent;

    public Item(String id, String name, Type type, String description, int price, int effectPercent, boolean isPermanent) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.price = price;
        this.effectPercent = effectPercent;
        this.isPermanent = isPermanent;
    }


}
