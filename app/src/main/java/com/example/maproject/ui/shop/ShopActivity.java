package com.example.maproject.ui.shop;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.example.maproject.data.UserRepository;
import com.example.maproject.model.InventoryItem;
import com.example.maproject.model.User;
import com.example.maproject.service.LevelingService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ShopActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private LevelingService levelingService;
    private FirebaseUser firebaseUser;
    private User currentUser;
    private int userLevel = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        userRepository = new UserRepository();
        levelingService = new LevelingService();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCurrentUser();
    }

    private void loadCurrentUser() {
        userRepository.getUser(firebaseUser.getUid(), user -> {
            currentUser = user;
            userLevel = levelingService.calculateLevelFromXP(user.getTotalExperiencePoints());
            setupAllItems();
        });
    }

    private void setupAllItems() {
        setupItem(R.id.item_potion1, R.drawable.ic_potion1,
                "Strength Potion +20%", "+20% PP (one-time)",
                "potion1", "potion", 50, 20, false);

        setupItem(R.id.item_potion2, R.drawable.ic_potion2,
                "Strength Potion +40%", "+40% PP (one-time)",
                "potion2", "potion", 70, 40, false);

        setupItem(R.id.item_potion3, R.drawable.ic_potion3,
                "Permanent Potion +5%", "+5% PP (permanent)",
                "potion3", "potion", 200, 5, true);

        setupItem(R.id.item_potion4, R.drawable.ic_potion4,
                "Permanent Potion +10%", "+10% PP (permanent)",
                "potion4", "potion", 1000, 10, true);

        // ODEĆA - cene u procentima
        setupClothing(R.id.item_gloves, R.drawable.ic_gloves,
                "Power Gloves", "+10% PP (2 fights)",
                "gloves", 60, 10, 0, 0);

        setupClothing(R.id.item_shield, R.drawable.ic_shield,
                "Magic Shield", "+10% hit chance (2 fights)",
                "shield", 60, 0, 10, 0);

        setupClothing(R.id.item_boots, R.drawable.ic_boots,
                "Speed Boots", "40% chance +1 attack (2 fights)",
                "boots", 80, 0, 0, 40);

        // ORUŽJE - ne može se kupiti
        setupWeapon(R.id.item_sword, R.drawable.ic_sword,
                "Mighty Sword", "+5% PP (permanent)",
                "sword");

        setupWeapon(R.id.item_bow, R.drawable.ic_bow,
                "Bow & Arrow", "+5% coin reward (permanent)",
                "bow");
    }

    private void setupItem(int itemId, int imageRes, String titleText, String descText,
                           String itemKey, String type, int pricePercentage,
                           int bonus, boolean isPermanent) {
        View itemView = findViewById(itemId);
        if (itemView == null) return;

        ImageView image = itemView.findViewById(R.id.item_image);
        TextView title = itemView.findViewById(R.id.item_title);
        TextView desc = itemView.findViewById(R.id.item_desc);
        TextView priceText = itemView.findViewById(R.id.item_price);
        ImageView coinIcon = itemView.findViewById(R.id.coin_icon);
        Button buyButton = itemView.findViewById(R.id.buy_button);

        image.setImageResource(imageRes);
        title.setText(titleText);
        desc.setText(descText);

        int actualPrice = calculateItemPrice(pricePercentage, userLevel);
        priceText.setText(String.valueOf(actualPrice));
        coinIcon.setImageResource(R.drawable.coins);

        buyButton.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(this, "Loading user data...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser.getCoins() < actualPrice) {
                Toast.makeText(this, "Not enough coins! Need " + actualPrice,
                        Toast.LENGTH_LONG).show();
                return;
            }

            InventoryItem item = new InventoryItem(itemKey, titleText, type, 1, 0);
            item.setPpBonus(bonus);
            item.setPermanent(isPermanent);

            userRepository.buyItem(firebaseUser.getUid(), item, actualPrice, status -> {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
                if (status.contains("success")) {
                    loadCurrentUser();
                }
            });
        });
    }

    private void setupClothing(int itemId, int imageRes, String titleText, String descText,
                               String itemKey, int pricePercentage,
                               int ppBonus, int attackBonus, int extraAttackChance) {
        View itemView = findViewById(itemId);
        if (itemView == null) return;

        ImageView image = itemView.findViewById(R.id.item_image);
        TextView title = itemView.findViewById(R.id.item_title);
        TextView desc = itemView.findViewById(R.id.item_desc);
        TextView priceText = itemView.findViewById(R.id.item_price);
        ImageView coinIcon = itemView.findViewById(R.id.coin_icon);
        Button buyButton = itemView.findViewById(R.id.buy_button);

        image.setImageResource(imageRes);
        title.setText(titleText);
        desc.setText(descText);

        int actualPrice = calculateItemPrice(pricePercentage, userLevel);
        priceText.setText(String.valueOf(actualPrice));
        coinIcon.setImageResource(R.drawable.coins);

        buyButton.setOnClickListener(v -> {
            if (currentUser == null || currentUser.getCoins() < actualPrice) {
                Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
                return;
            }

            InventoryItem item = new InventoryItem(itemKey, titleText, "clothing", 1, 2);
            item.setPpBonus(ppBonus);
            item.setAttackSuccessBonus(attackBonus);
            item.setExtraAttackChance(extraAttackChance);

            userRepository.buyItem(firebaseUser.getUid(), item, actualPrice, status -> {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
                if (status.contains("success")) loadCurrentUser();
            });
        });
    }

    private void setupWeapon(int itemId, int imageRes, String titleText,
                             String descText, String itemKey) {
        View itemView = findViewById(itemId);
        if (itemView == null) return;

        ImageView image = itemView.findViewById(R.id.item_image);
        TextView title = itemView.findViewById(R.id.item_title);
        TextView desc = itemView.findViewById(R.id.item_desc);
        TextView priceText = itemView.findViewById(R.id.item_price);
        ImageView coinIcon = itemView.findViewById(R.id.coin_icon);
        Button buyButton = itemView.findViewById(R.id.buy_button);

        image.setImageResource(imageRes);
        title.setText(titleText);
        desc.setText(descText);
        priceText.setText("Boss drop only");
        coinIcon.setVisibility(View.GONE);
        buyButton.setText("NOT FOR SALE");
        buyButton.setEnabled(false);
    }

    private int calculateItemPrice(int percentage, int level) {

        int priceBaseLevel = (level <= 1) ? 1 : (level - 1);

        int bossReward = calculateBossReward(priceBaseLevel);

        return (int) (bossReward * percentage / 100.0);
    }

    private int calculateBossReward(int level) {
        if (level <= 1) return 200;
        int reward = 200;
        for (int i = 2; i <= level; i++) {
            reward = (int) (reward * 1.2);
        }
        return reward;
    }
}