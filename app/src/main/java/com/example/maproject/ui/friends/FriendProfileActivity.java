package com.example.maproject.ui.friends;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.model.InventoryItem;
import com.example.maproject.service.LevelingService;
import com.example.maproject.ui.model.InventoryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FriendProfileActivity extends AppCompatActivity {

    private ImageView avatarImageView, qrCodeImageView, titleIconImageView, xpBarImageView;
    private TextView usernameTextView, levelTextView, titleTextView;
    private TextView powerPointsTextView, experiencePointsTextView, coinsTextView, badgesTextView, xpProgressTextView;
    private Button backButton;

    private RecyclerView activeRecycler;
    private InventoryAdapter activeAdapter;

    private FirebaseFirestore db;
    private LevelingService levelingService;
    private String friendId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        levelingService = new LevelingService();

        friendId = getIntent().getStringExtra("FRIEND_ID");
        if (friendId == null) {
            Toast.makeText(this, "Error: friend unknown", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();

        if (!friendId.equals(getCurrentUserId())) {
            View inventoryButton = findViewById(R.id.viewInventoryButton);
            if (inventoryButton != null) inventoryButton.setVisibility(View.GONE);

            View changePassButton = findViewById(R.id.changePasswordButton);
            if (changePassButton != null) changePassButton.setVisibility(View.GONE);

            View analyticsButton = findViewById(R.id.viewStatisticsButton);
            if (analyticsButton != null) analyticsButton.setVisibility(View.GONE);
        }


        loadFriendProfile();
        backButton.setOnClickListener(v -> finish());
    }

    private void initViews() {
        avatarImageView = findViewById(R.id.avatarImageView);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        levelTextView = findViewById(R.id.levelTextView);
        titleTextView = findViewById(R.id.titleTextView);
        powerPointsTextView = findViewById(R.id.powerPointsTextView);
        experiencePointsTextView = findViewById(R.id.experiencePointsTextView);
        coinsTextView = findViewById(R.id.coinsTextView);
        badgesTextView = findViewById(R.id.badgesTextView);
        xpBarImageView = findViewById(R.id.xpBarImageView);
        xpProgressTextView = findViewById(R.id.xpProgressTextView);
        titleIconImageView = findViewById(R.id.titleIconImageView);
        backButton = findViewById(R.id.backButton);

        activeRecycler = findViewById(R.id.active_equipment_recycler);

        activeRecycler.setLayoutManager(new LinearLayoutManager(this));

    }

    private void loadFriendProfile() {
        db.collection("users").document(friendId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String avatar = document.getString("avatar");
                    setAvatar(avatar);

                    String username = document.getString("username");
                    usernameTextView.setText(username != null ? username : "User");

                    long totalXp = document.getLong("totalExperiencePoints") != null ? document.getLong("totalExperiencePoints") : 0L;
                    int level = levelingService.calculateLevelFromXP(totalXp);
                    long currentLevelXP = levelingService.getCurrentLevelXP(totalXp, level);
                    long xpForNextLevel = levelingService.getXPForNextLevel(level);
                    int powerPoints = levelingService.calculatePPFromLevel(level);

                    levelTextView.setText("LEVEL " + level);
                    titleTextView.setText(levelingService.getTitleForLevel(level));
                    powerPointsTextView.setText(String.valueOf(powerPoints));

                    long coins = document.getLong("coins") != null ? document.getLong("coins") : 0;
                    long badges = document.getLong("badges") != null ? document.getLong("badges") : 0;

                    coinsTextView.setText(String.valueOf(coins));
                    badgesTextView.setText(String.valueOf(badges));
                    experiencePointsTextView.setText(String.valueOf(totalXp));

                    xpProgressTextView.setText(currentLevelXP + " / " + xpForNextLevel + " XP");
                    updateXPBar(currentLevelXP, xpForNextLevel);

                    setTitleIcon(level);
                    generateQRCode(friendId);

                    setupEquipmentLists(document);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error occurred while loading the profile", Toast.LENGTH_SHORT).show());
    }

    private void setupEquipmentLists(DocumentSnapshot document) {
        List<InventoryItem> activeEquipment = new ArrayList<>();
        List<InventoryItem> availableEquipment = new ArrayList<>();

        List<Map<String, Object>> activeList = (List<Map<String, Object>>) document.get("activeEquipment");
        if (activeList != null) {
            for (Map<String, Object> map : activeList) {
                InventoryItem item = new InventoryItem(
                        (String) map.get("itemId"),
                        (String) map.get("name"),
                        (String) map.get("type"),
                        ((Long) map.get("quantity")).intValue(),
                        ((Long) map.get("remainingBattles")).intValue()
                );
                item.setActive(map.get("active") != null ? (Boolean) map.get("active") : false);
                activeEquipment.add(item);
            }
        }

        List<Map<String, Object>> availableList = (List<Map<String, Object>>) document.get("availableEquipment");
        if (availableList != null) {
            for (Map<String, Object> map : availableList) {
                InventoryItem item = new InventoryItem(
                        (String) map.get("itemId"),
                        (String) map.get("name"),
                        (String) map.get("type"),
                        ((Long) map.get("quantity")).intValue(),
                        ((Long) map.get("remainingBattles")).intValue()
                );
                item.setActive(map.get("active") != null ? (Boolean) map.get("active") : false);
                availableEquipment.add(item);
            }
        }

        activeAdapter = new InventoryAdapter(activeEquipment, friendId, true, null, null);
        activeRecycler.setAdapter(activeAdapter);

    }

    private void updateXPBar(long currentXP, long maxXP) {
        double percentage = maxXP > 0 ? (double) currentXP / maxXP : 0.0;
        percentage = Math.min(percentage, 1.0);

        int xpImageIndex;
        if (percentage <= 0.01) xpImageIndex = 1;
        else if (percentage <= 0.25) xpImageIndex = 2;
        else if (percentage <= 0.50) xpImageIndex = 3;
        else if (percentage <= 0.75) xpImageIndex = 4;
        else xpImageIndex = 5;

        int xpIconResId = getResources().getIdentifier("xp_" + xpImageIndex, "drawable", getPackageName());
        xpBarImageView.setImageResource(xpIconResId != 0 ? xpIconResId : R.drawable.xp_1);
    }

    private void setAvatar(String avatarName) {
        if (avatarName == null) return;
        int avatarResId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
        if (avatarResId != 0) avatarImageView.setImageResource(avatarResId);
    }

    private void setTitleIcon(int level) {
        int iconResource;
        if (level <= 1) iconResource = R.drawable.title_1;
        else if (level <= 2) iconResource = R.drawable.title_2;
        else if (level <= 3) iconResource = R.drawable.title_3;
        else if (level <= 4) iconResource = R.drawable.title_4;
        else iconResource = R.drawable.title_5;

        titleIconImageView.setImageResource(iconResource);
    }

    private void generateQRCode(String userId) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(userId, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            qrCodeImageView.setImageBitmap(bmp);
        } catch (WriterException e) {
            Toast.makeText(this, "Error while loading the QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
}
