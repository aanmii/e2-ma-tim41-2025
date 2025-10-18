package com.example.maproject.ui.model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.model.InventoryItem;
import com.example.maproject.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private List<InventoryItem> items;
    private String userId;
    private boolean isActiveSection;
    private InventoryAdapter otherAdapter;
    private List<InventoryItem> otherList;

    public InventoryAdapter(List<InventoryItem> items, String userId, boolean isActiveSection,
                            InventoryAdapter otherAdapter, List<InventoryItem> otherList) {
        this.items = items;
        this.userId = userId;
        this.isActiveSection = isActiveSection;
        this.otherAdapter = otherAdapter;
        this.otherList = otherList;
    }

    public void setOtherAdapter(InventoryAdapter adapter) {
        this.otherAdapter = adapter;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_card, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventoryItem item = items.get(position);

        holder.itemName.setText(item.getName());
        holder.itemType.setText(item.getType());
        holder.itemQuantity.setText("x" + item.getQuantity());
        holder.itemImage.setImageResource(getImageResource(item.getItemId()));

        if (isActiveSection || item.isActive()) {
            holder.activateButton.setText("Activated ✅");
            holder.activateButton.setEnabled(false);
        } else {
            holder.activateButton.setText("Activate");
            holder.activateButton.setEnabled(true);

            holder.activateButton.setOnClickListener(v -> {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (!snapshot.exists()) return;

                            User user = snapshot.toObject(User.class);
                            if (user == null) return;

                            // Smanji količinu ili ukloni iz inventara
                            if (item.getQuantity() > 1) {
                                item.setQuantity(item.getQuantity() - 1);
                            } else {
                                items.remove(position);
                            }

                            // Kreiraj aktivnu stavku SA SVIM BONUSIMA
                            InventoryItem activeItem = new InventoryItem(
                                    item.getItemId(),
                                    item.getName(),
                                    item.getType(),
                                    1,
                                    item.getRemainingBattles()
                            );

                            // **KLJUČNO - KOPIRAJ SVE BONUSE**
                            activeItem.setPpBonus(item.getPpBonus());
                            activeItem.setAttackSuccessBonus(item.getAttackSuccessBonus());
                            activeItem.setExtraAttackChance(item.getExtraAttackChance());
                            activeItem.setCoinBonus(item.getCoinBonus());
                            activeItem.setPermanent(item.isPermanent());
                            activeItem.setUpgradeLevel(item.getUpgradeLevel());
                            activeItem.setActive(true);

                            // Dodaj u aktivnu opremu
                            if (otherList != null) {
                                otherList.add(activeItem);
                            }

                            // Sačuvaj u bazu
                            user.setEquipment(items);
                            user.setActiveEquipment(otherList);

                            db.collection("users").document(userId)
                                    .set(user.toMap())
                                    .addOnSuccessListener(aVoid -> {
                                        notifyDataSetChanged();
                                        if (otherAdapter != null) {
                                            otherAdapter.notifyDataSetChanged();
                                        }

                                        // Log za debug
                                        android.util.Log.d("InventoryAdapter",
                                                "Item activated: " + item.getName() +
                                                        " | PP Bonus: " + activeItem.getPpBonus() +
                                                        " | Attack Bonus: " + activeItem.getAttackSuccessBonus() +
                                                        " | Extra Attack Chance: " + activeItem.getExtraAttackChance());
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e("InventoryAdapter", "Failed to save equipment", e);
                                    });
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("InventoryAdapter", "Failed to load user", e);
                        });
            });
        }
    }

    private int getImageResource(String itemId) {
        switch (itemId) {
            case "potion1": return R.drawable.ic_potion1;
            case "potion2": return R.drawable.ic_potion2;
            case "potion3": return R.drawable.ic_potion3;
            case "potion4": return R.drawable.ic_potion4;
            case "gloves": return R.drawable.ic_gloves;
            case "boots": return R.drawable.ic_boots;
            case "shield": return R.drawable.ic_shield;
            case "sword": return R.drawable.ic_sword;
            default: return R.drawable.ic_potion1;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemName, itemType, itemQuantity;
        Button activateButton;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.item_image);
            itemName = itemView.findViewById(R.id.item_title);
            itemType = itemView.findViewById(R.id.item_desc);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            activateButton = itemView.findViewById(R.id.activate_button);
        }
    }
}
