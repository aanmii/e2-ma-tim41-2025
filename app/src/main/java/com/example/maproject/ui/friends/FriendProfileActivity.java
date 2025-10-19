package com.example.maproject.ui.friends;

import android.content.Intent;
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
    private Button backButton, viewEquipmentButton;

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
        hideSensitiveElements();
        setupButtons();
        loadFriendProfile();
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
        if (activeRecycler != null) {
            activeRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        }
    }

    private void hideSensitiveElements() {
        // Sakrij ceo Power Points i Coins red
        View powerPointsCoinsRow = findViewById(R.id.powerPointsCoinsRow);
        if (powerPointsCoinsRow != null) {
            powerPointsCoinsRow.setVisibility(View.GONE);
        }

        // Sakrij dugmad koja nisu relevantna
        View changePasswordButton = findViewById(R.id.changePasswordButton);
        if (changePasswordButton != null) changePasswordButton.setVisibility(View.GONE);

        View viewStatisticsButton = findViewById(R.id.viewStatisticsButton);
        if (viewStatisticsButton != null) viewStatisticsButton.setVisibility(View.GONE);

        View viewInventoryButton = findViewById(R.id.viewInventoryButton);
        if (viewInventoryButton != null) viewInventoryButton.setVisibility(View.GONE);

        View viewTasksButton = findViewById(R.id.viewTasksButton);
        if (viewTasksButton != null) viewTasksButton.setVisibility(View.GONE);

        // Active Equipment card OSTAJE VIDLJIV za prijatelje
        // View Equipment dugme ostaje vidljivo
    }

    private void setupButtons() {
        backButton.setOnClickListener(v -> finish());

        if (viewEquipmentButton != null) {
            viewEquipmentButton.setOnClickListener(v -> {
                Intent intent = new Intent(FriendProfileActivity.this, com.example.maproject.ui.model.InventoryActivity.class);
                intent.putExtra("USER_ID", friendId);
                startActivity(intent);
            });
        }
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
                    usernameTextView.setText(username != null ? username : "Unknown User");

                    long totalXp = document.getLong("totalExperiencePoints") != null ?
                            document.getLong("totalExperiencePoints") : 0L;

                    int level = levelingService.calculateLevelFromXP(totalXp);
                    long currentLevelXP = levelingService.getCurrentLevelXP(totalXp, level);
                    long xpForNextLevel = levelingService.getXPForNextLevel(level);

                    levelTextView.setText("LEVEL " + level);
                    titleTextView.setText(levelingService.getTitleForLevel(level));
                    experiencePointsTextView.setText(String.valueOf(totalXp));

                    long badges = document.getLong("badges") != null ? document.getLong("badges") : 0;
                    badgesTextView.setText(String.valueOf(badges));

                    xpProgressTextView.setText(currentLevelXP + " / " + xpForNextLevel + " XP");
                    updateXPBar(currentLevelXP, xpForNextLevel);

                    setTitleIcon(level);
                    generateQRCode(friendId);

                    setupEquipmentLists(document);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error occurred while loading the profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupEquipmentLists(DocumentSnapshot document) {
        if (activeRecycler == null) return;

        List<InventoryItem> activeEquipment = new ArrayList<>();
        List<Map<String, Object>> activeList = (List<Map<String, Object>>) document.get("activeEquipment");

        android.util.Log.d("FriendProfile", "Active equipment count: " + (activeList != null ? activeList.size() : 0));

        if (activeList != null) {
            for (Map<String, Object> map : activeList) {
                InventoryItem item = new InventoryItem(
                        (String) map.getOrDefault("itemId", "unknown"),
                        (String) map.getOrDefault("name", "Unknown"),
                        (String) map.getOrDefault("type", "Unknown"),
                        ((Long) map.getOrDefault("quantity", 0L)).intValue(),
                        ((Long) map.getOrDefault("remainingBattles", 0L)).intValue()
                );
                item.setActive(map.get("active") != null ? (Boolean) map.get("active") : false);
                activeEquipment.add(item);
                android.util.Log.d("FriendProfile", "Added item: " + item.getName());
            }
        }

        android.util.Log.d("FriendProfile", "Final list size: " + activeEquipment.size());
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
        if (avatarName != null && !avatarName.isEmpty()) {
            int avatarResId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
            if (avatarResId != 0) {
                avatarImageView.setImageResource(avatarResId);
            } else {
                avatarImageView.setImageResource(R.drawable.avatar_1);
            }
        } else {
            avatarImageView.setImageResource(R.drawable.avatar_1);
        }
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
            Bitmap bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);
            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            qrCodeImageView.setImageBitmap(bmp);
        } catch (WriterException e) {
            Toast.makeText(this, "Error while generating QR code", Toast.LENGTH_SHORT).show();
        }
    }
}