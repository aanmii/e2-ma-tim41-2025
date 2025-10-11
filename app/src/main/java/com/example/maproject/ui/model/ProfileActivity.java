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
    private TextView usernameTextView, levelTextView, titleTextView;
    private TextView powerPointsTextView, experiencePointsTextView, coinsTextView, badgesTextView;
    private Button changePasswordButton, backButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

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
    }

    private void loadUserProfile() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Avatar
                        String avatar = document.getString("avatar");
                        setAvatar(avatar);

                        // Tekstualni podaci
                        usernameTextView.setText(document.getString("username"));
                        levelTextView.setText("Nivo: " + document.getLong("level"));
                        titleTextView.setText(document.getString("title"));
                        powerPointsTextView.setText("PP: " + document.getLong("powerPoints"));
                        experiencePointsTextView.setText("XP: " + document.getLong("experiencePoints"));
                        coinsTextView.setText("Novčići: " + document.getLong("coins"));
                        badgesTextView.setText("Bedževi: " + document.getLong("badges"));

                        // Generiši QR kod
                        generateQRCode(currentUserId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
                });
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