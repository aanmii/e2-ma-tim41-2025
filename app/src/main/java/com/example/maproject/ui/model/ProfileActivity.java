package com.example.maproject.ui.model;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.example.maproject.service.LevelingService;
import com.example.maproject.ui.auth.ChangePasswordActivity;
import com.example.maproject.ui.statistics.StatisticsActivity;
import com.example.maproject.ui.model.InventoryActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class ProfileActivity extends AppCompatActivity {

    private ImageView avatarImageView, qrCodeImageView, titleIconImageView, xpBarImageView;
    private TextView usernameTextView, levelTextView, titleTextView;
    private TextView powerPointsTextView, experiencePointsTextView, coinsTextView, badgesTextView;
    private TextView xpProgressTextView;
    private Button changePasswordButton, backButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentUserId;
    private LevelingService levelingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        levelingService = new LevelingService();

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        initViews();
        loadUserProfile();
        setupButtons();

        Button viewStatisticsButton = findViewById(R.id.viewStatisticsButton);
        viewStatisticsButton.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, StatisticsActivity.class)));
    }

    private void initViews() {
        avatarImageView = findViewById(R.id.avatarImageView);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        titleIconImageView = findViewById(R.id.titleIconImageView);
        xpBarImageView = findViewById(R.id.xpBarImageView);

        usernameTextView = findViewById(R.id.usernameTextView);
        levelTextView = findViewById(R.id.levelTextView);
        titleTextView = findViewById(R.id.titleTextView);
        powerPointsTextView = findViewById(R.id.powerPointsTextView);
        experiencePointsTextView = findViewById(R.id.experiencePointsTextView);
        coinsTextView = findViewById(R.id.coinsTextView);
        badgesTextView = findViewById(R.id.badgesTextView);
        xpProgressTextView = findViewById(R.id.xpProgressTextView);

        changePasswordButton = findViewById(R.id.changePasswordButton);
        backButton = findViewById(R.id.backButton);
    }

    private void loadUserProfile() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;

                    String username = document.getString("username");
                    String avatar = document.getString("avatar");

                    long totalXp = document.getLong("totalExperiencePoints") != null ?
                            document.getLong("totalExperiencePoints") : 0L;
                    int coins = document.getLong("coins") != null ? document.getLong("coins").intValue() : 0;
                    int badges = document.getLong("badges") != null ? document.getLong("badges").intValue() : 0;

                    int level = levelingService.calculateLevelFromXP(totalXp);
                    long currentLevelXP = levelingService.getCurrentLevelXP(totalXp, level);
                    long xpForNextLevel = levelingService.getXPForNextLevel(level);
                    int powerPoints = levelingService.calculatePPFromLevel(level);

                    usernameTextView.setText(username != null ? username : "User");
                    setAvatar(avatar);

                    levelTextView.setText("LEVEL " + level);
                    titleTextView.setText(levelingService.getTitleForLevel(level));
                    powerPointsTextView.setText(String.valueOf(powerPoints));
                    experiencePointsTextView.setText(String.valueOf(totalXp));
                    coinsTextView.setText(String.valueOf(coins));
                    badgesTextView.setText(String.valueOf(badges));

                    xpProgressTextView.setText(currentLevelXP + " / " + xpForNextLevel + " XP");
                    updateXPBar(currentLevelXP, xpForNextLevel);

                    setTitleIcon(level);
                    generateQRCode(currentUserId);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error occurred while loading the user profile", Toast.LENGTH_SHORT).show());
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

    private void setTitleIcon(int level) {
        int iconResource;
        if (level <= 1) iconResource = R.drawable.title_1;
        else if (level <= 2) iconResource = R.drawable.title_2;
        else if (level <= 3) iconResource = R.drawable.title_3;
        else if (level <= 4) iconResource = R.drawable.title_4;
        else iconResource = R.drawable.title_5;

        titleIconImageView.setImageResource(iconResource);
    }

    private void setAvatar(String avatarName) {
        if (avatarName == null) return;
        int avatarResId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
        if (avatarResId != 0) avatarImageView.setImageResource(avatarResId);
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

    private void setupButtons() {
        changePasswordButton.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class)));
        backButton.setOnClickListener(v -> finish());

        Button viewInventoryButton = findViewById(R.id.viewInventoryButton);
        viewInventoryButton.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, InventoryActivity.class)));
    }
}
