// ShopRepository.java
package com.example.maproject.data;

import com.example.maproject.model.Item;
import com.example.maproject.model.Item.Type;
import java.util.ArrayList;
import java.util.List;

public class ShopRepository {

    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();

        items.add(new Item("potion1", "Strength potion +20%", Type.POTION, "+20% PP", 50, 20, false));
        items.add(new Item("potion2", "Strength potion +40%", Type.POTION, "+40% PP", 70, 40, false));
        items.add(new Item("potion3", "Permanent potion +5%", Type.POTION, "+5% PP forever", 200, 5, true));
        items.add(new Item("potion4", "Permanent potion +10%", Type.POTION, "+10% PP forever", 1000, 10, true));

        items.add(new Item("gloves", "Hearty gloves", Type.CLOTHING, "+10% strength", 60, 10, false));
        items.add(new Item("shield", "Magic shield", Type.CLOTHING, "+10% chance for success", 60, 10, false));
        items.add(new Item("boots", "Sparkle boots", Type.CLOTHING, "+40% extra attacks", 80, 40, false));

        items.add(new Item("sword", "Sword", Type.WEAPON, "+5% strength", 0, 5, true));
        items.add(new Item("bow", "Arrow and bow", Type.WEAPON, "+5% coins", 0, 5, true));

        return items;
    }
}
