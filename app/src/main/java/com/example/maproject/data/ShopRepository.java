// ShopRepository.java
package com.example.maproject.data;

import com.example.maproject.model.Item;
import com.example.maproject.model.Item.Type;
import java.util.ArrayList;
import java.util.List;

public class ShopRepository {

    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();

        // Napici
        items.add(new Item("potion1", "Napitak snage +20%", Type.POTION, "Povećava PP za 20%", 50, 20, false));
        items.add(new Item("potion2", "Napitak snage +40%", Type.POTION, "Povećava PP za 40%", 70, 40, false));
        items.add(new Item("potion3", "Trajni napitak +5%", Type.POTION, "Trajno povećava PP za 5%", 200, 5, true));
        items.add(new Item("potion4", "Trajni napitak +10%", Type.POTION, "Trajno povećava PP za 10%", 1000, 10, true));

        // Odeća
        items.add(new Item("gloves", "Rukavice", Type.CLOTHING, "+10% snage", 60, 10, false));
        items.add(new Item("shield", "Štit", Type.CLOTHING, "+10% šanse za uspeh", 60, 10, false));
        items.add(new Item("boots", "Čizme", Type.CLOTHING, "+40% dodatnih napada", 80, 40, false));

        // Oružje
        items.add(new Item("sword", "Mač hrabrosti", Type.WEAPON, "+5% snage", 0, 5, true));
        items.add(new Item("bow", "Luk i strela", Type.WEAPON, "+5% novčića", 0, 5, true));

        return items;
    }
}
