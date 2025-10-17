package com.example.maproject.ui.friends;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.example.maproject.service.LevelingService;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class FriendProfileActivity extends AppCompatActivity {

    private ImageView avatarImageView, qrCodeImageView, titleIconImageView, xpBarImageView;
    private TextView usernameTextView, levelTextView, titleTextView;
    private TextView powerPointsTextView, experiencePointsTextView, coinsTextView, badgesTextView, xpProgressTextView;
    private Button backButton;

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
        hideUnnecessaryButtons();
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
    }

    private void hideUnnecessaryButtons() {
        findViewById(R.id.changePasswordButton).setVisibility(android.view.View.GONE);
        findViewById(R.id.viewStatisticsButton).setVisibility(android.view.View.GONE);
        findViewById(R.id.viewInventoryButton).setVisibility(android.view.View.GONE);

        backButton.setOnClickListener(v -> finish());
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

                    usernameTextView.setText(document.getString("username"));
                    titleTextView.setText(document.getString("title"));

                    int currentLevel = ((Number) document.get("level")).intValue();
                    levelTextView.setText("LEVEL " + currentLevel);
                    setTitleIcon(currentLevel);

                    long powerPoints = getLong(document.get("powerPoints"));
                    long totalXp = getLong(document.get("totalExperiencePoints"));
                    long coins = getLong(document.get("coins"));
                    long badges = getLong(document.get("badges"));

                    powerPointsTextView.setText(String.valueOf(powerPoints));
                    experiencePointsTextView.setText(String.valueOf(totalXp));
                    coinsTextView.setText(String.valueOf(coins));
                    badgesTextView.setText(String.valueOf(badges));

                    long currentLevelXP = levelingService.getCurrentLevelXP(totalXp, currentLevel);
                    long xpForNextLevel = levelingService.getXPForNextLevel(currentLevel);
                    updateXPBar(currentLevel, currentLevelXP, xpForNextLevel);

                    generateQRCode(friendId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error occurred while loading the profile", Toast.LENGTH_SHORT).show()
                );
    }

    private long getLong(Object obj) {
        return (obj instanceof Number) ? ((Number) obj).longValue() : 0L;
    }

    private void updateXPBar(int currentLevel, long currentXP, long requiredXPForNextLevel) {
        xpProgressTextView.setText(currentXP + "/" + requiredXPForNextLevel);

        double percentage = requiredXPForNextLevel > 0 ? (double) currentXP / requiredXPForNextLevel : 0.0;
        int xpImageIndex;
        if (percentage <= 0.01) xpImageIndex = 1;
        else if (percentage <= 0.25) xpImageIndex = 2;
        else if (percentage <= 0.50) xpImageIndex = 3;
        else if (percentage <= 0.75) xpImageIndex = 4;
        else xpImageIndex = 5;

        String xpIconName = "xp_" + xpImageIndex;
        int xpIconResId = getResources().getIdentifier(xpIconName, "drawable", getPackageName());
        xpBarImageView.setImageResource(xpIconResId);
    }

    private void setAvatar(String avatarName) {
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
}
