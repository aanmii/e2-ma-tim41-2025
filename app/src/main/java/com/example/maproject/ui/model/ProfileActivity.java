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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class ProfileActivity extends AppCompatActivity {

    private ImageView avatarImageView, qrCodeImageView;
    private ImageView titleIconImageView;
    private ImageView xpBarImageView;

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
        viewStatisticsButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });
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
        changePasswordButton = findViewById(R.id.changePasswordButton);
        backButton = findViewById(R.id.backButton);

        xpBarImageView = findViewById(R.id.xpBarImageView);
        xpProgressTextView = findViewById(R.id.xpProgressTextView);
        titleIconImageView = findViewById(R.id.titleIconImageView);
    }

    private void loadUserProfile() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String avatar = document.getString("avatar");
                        setAvatar(avatar);

                        usernameTextView.setText(document.getString("username"));

                        String title = document.getString("title");
                        titleTextView.setText(title != null ? title : "Početnik");

                        // Koristi Object čitanje za robustnost numeričkih tipova
                        Object levelObj = document.get("level");
                        int currentLevel = (levelObj instanceof Number) ? ((Number) levelObj).intValue() : 0;
                        levelTextView.setText("⭐ LEVEL " + currentLevel);

                        setTitleIcon(currentLevel);

                        Object powerPointsObj = document.get("powerPoints");
                        long powerPoints = (powerPointsObj instanceof Number) ? ((Number) powerPointsObj).longValue() : 0L;
                        powerPointsTextView.setText(String.valueOf(powerPoints));

                        Object totalXpObj = document.get("totalExperiencePoints");
                        long totalXp = (totalXpObj instanceof Number) ? ((Number) totalXpObj).longValue() : 0L;
                        experiencePointsTextView.setText(String.valueOf(totalXp));

                        Object coinsObj = document.get("coins");
                        long coins = (coinsObj instanceof Number) ? ((Number) coinsObj).longValue() : 0L;
                        coinsTextView.setText(String.valueOf(coins));

                        Object badgesObj = document.get("badges");
                        long badges = (badgesObj instanceof Number) ? ((Number) badgesObj).longValue() : 0L;
                        badgesTextView.setText(String.valueOf(badges));

                        // OVO JE KRITIČNO POLJE ZA CURRENT XP
                        Object currentLevelXpObj = document.get("currentLevelXP");
                        long currentXP = (currentLevelXpObj instanceof Number) ? ((Number) currentLevelXpObj).longValue() : 0L;

                        long requiredXp = levelingService.getRequiredXPForLevelUp(currentLevel + 1);

                        if (requiredXp == 0) requiredXp = 200;

                        updateLevelAndXPView(currentLevel, currentXP, requiredXp);

                        generateQRCode(currentUserId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLevelAndXPView(int currentLevel, long currentXP, long requiredXPForNextLevel) {
        levelTextView.setText("⭐ LEVEL " + currentLevel);
        // OVO SETUJE TEKST (npr. 44/3300)
        xpProgressTextView.setText(currentXP + "/" + requiredXPForNextLevel);

        double percentage = requiredXPForNextLevel > 0 ? (double) currentXP / requiredXPForNextLevel : 0.0;

        if (percentage >= 1.0) {
            percentage = 1.0;
        }

        int xpImageIndex;

        // LOGIKA ZA 5 SLIKA (1-5), praga 25%
        if (percentage <= 0.01) {
            xpImageIndex = 1;
        } else if (percentage <= 0.25) {
            xpImageIndex = 2;
        } else if (percentage <= 0.50) {
            xpImageIndex = 3;
        } else if (percentage <= 0.75) {
            xpImageIndex = 4;
        } else {
            xpImageIndex = 5;
        }

        String xpIconName = "xp_" + xpImageIndex;
        int xpIconResId = getResources().getIdentifier(xpIconName, "drawable", getPackageName());

        if (xpIconResId != 0) {
            xpBarImageView.setImageResource(xpIconResId);
        } else {
            xpBarImageView.setImageResource(R.drawable.xp_1);
        }

        setTitleIcon(currentLevel);
    }

    private void setTitleIcon(int level) {
        ImageView titleIcon = findViewById(R.id.titleIconImageView);
        int iconResource;

        if (level <= 1) iconResource = R.drawable.title_1;
        else if (level <= 2) iconResource = R.drawable.title_2;
        else if (level <= 3) iconResource = R.drawable.title_3;
        else if (level <= 4) iconResource = R.drawable.title_4;
        else iconResource = R.drawable.title_5;

        titleIcon.setImageResource(iconResource);
    }

    private void setAvatar(String avatarName) {
        int avatarResId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
        if (avatarResId != 0) {
            avatarImageView.setImageResource(avatarResId);
        }
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
            Toast.makeText(this, "Greška pri generisanju QR koda", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupButtons() {
        changePasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> finish());
    }
}