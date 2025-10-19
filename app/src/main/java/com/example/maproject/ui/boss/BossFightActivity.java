package com.example.maproject.ui.boss;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.example.maproject.data.BossFightRepository;
import com.example.maproject.model.BossFight;
import com.google.firebase.auth.FirebaseAuth;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class BossFightActivity extends AppCompatActivity {

    // UI Components
    private TextView tvBossLevel, tvBossHp, tvUserPp, tvAttacksRemaining;
    private TextView tvSuccessChance, tvEquipmentBonuses;
    private ProgressBar bossHpBar;
    private ProgressBar playerPpBar;
    private Button btnAttack, btnFinishFight;
    private View resultContainer;
    private TextView tvResult, tvCoinsEarned, tvItemDropped;
    private ImageView ivBossSprite;

    // Drop overlay
    private View dropOverlay;
    private ImageView ivDroppedItem;
    private TextView tvDroppedItemName;
    private Button btnCloseDropOverlay;

    // Treasure chest
    private ImageView ivTreasureChest;

    // Data
    private BossFightRepository fightRepo;
    private String userId;
    private BossFight currentFight;

    // Sensors (shake)
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener shakeListener;
    private long lastShakeTime = 0;
    private static final float SHAKE_THRESHOLD = 13f; // tuned threshold

    private Handler mainHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_fight);

        fightRepo = new BossFightRepository();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            userId = null;
        }

        initViews();
        setupShakeListener();
        prepareFight();
    }

    private void initViews() {
        ivBossSprite = findViewById(R.id.ivBossSprite);
        tvBossLevel = findViewById(R.id.tvBossLevel);
        tvBossHp = findViewById(R.id.tvBossHp);
        tvUserPp = findViewById(R.id.tvUserPp);
        tvAttacksRemaining = findViewById(R.id.tvAttacksRemaining);
        tvSuccessChance = findViewById(R.id.tvSuccessChance);
        tvEquipmentBonuses = findViewById(R.id.tvEquipmentBonuses);
        bossHpBar = findViewById(R.id.bossHpBar);
        playerPpBar = findViewById(R.id.playerPpBar);
        btnAttack = findViewById(R.id.btnAttack);
        btnFinishFight = findViewById(R.id.btnFinishFight);

        resultContainer = findViewById(R.id.resultContainer);
        tvResult = findViewById(R.id.tvResult);
        tvCoinsEarned = findViewById(R.id.tvCoinsEarned);
        tvItemDropped = findViewById(R.id.tvItemDropped);

        // Drop overlay
        dropOverlay = findViewById(R.id.dropOverlay);
        ivDroppedItem = findViewById(R.id.ivDroppedItem);
        tvDroppedItemName = findViewById(R.id.tvDroppedItemName);
        btnCloseDropOverlay = findViewById(R.id.btnCloseDropOverlay);

        ivTreasureChest = findViewById(R.id.ivTreasureChest);

        btnAttack.setOnClickListener(v -> performAttack());
        btnFinishFight.setOnClickListener(v -> finish());
        btnCloseDropOverlay.setOnClickListener(v -> dropOverlay.setVisibility(View.GONE));

        ivTreasureChest.setOnClickListener(v -> openChest());

        resultContainer.setVisibility(View.GONE);
        dropOverlay.setVisibility(View.GONE);
        ivTreasureChest.setVisibility(View.GONE);
    }

    private void setupShakeListener() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        shakeListener = new SensorEventListener() {
            private float[] gravity = new float[3];
            private float[] linear_acceleration = new float[3];

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

                final float alpha = 0.8f;

                // Isolate the force of gravity with a low-pass filter.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                // Remove the gravity contribution with a high-pass filter.
                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];

                float magnitude = (float) Math.sqrt(
                        linear_acceleration[0] * linear_acceleration[0] +
                                linear_acceleration[1] * linear_acceleration[1] +
                                linear_acceleration[2] * linear_acceleration[2]);

                if (magnitude > SHAKE_THRESHOLD) {
                    long now = System.currentTimeMillis();
                    if (now - lastShakeTime > 600) { // debounce
                        lastShakeTime = now;
                        onShakeDetected();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }

    private void onShakeDetected() {
        // If results are visible and chest is shown, open chest on shake (spec requirement)
        if (resultContainer != null && resultContainer.getVisibility() == View.VISIBLE
                && ivTreasureChest != null && ivTreasureChest.getVisibility() == View.VISIBLE) {
            openChest();
            return;
        }

        // Otherwise, if there is an active fight with attacks remaining, treat shake as an attack
        if (currentFight == null || !currentFight.hasAttacksRemaining()) return;

        // Vibration feedback using modern API when available
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // deprecated on older devices but fallback
                v.vibrate(80);
            }
        }

        // perform attack on UI thread
        mainHandler.post(this::performAttack);
    }

    private void prepareFight() {
        fightRepo.prepareBossFight(userId, new BossFightRepository.OnBossFightPreparedListener() {
            @Override
            public void onPrepared(BossFight fight) {
                currentFight = fight;
                displayFightInfo();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BossFightActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayFightInfo() {
        if (currentFight == null) return;

        tvBossLevel.setText("Boss Level " + currentFight.getBossLevel());
        updateBossHp();

        // Prikaži PP boost
        String ppText = "Your Power: " + currentFight.getUserBasePp() + " PP";
        if (currentFight.getUserFinalPp() > currentFight.getUserBasePp()) {
            int increase = currentFight.getUserFinalPp() - currentFight.getUserBasePp();
            int percentIncrease = (int) (((currentFight.getUserFinalPp() - currentFight.getUserBasePp()) * 100.0) / currentFight.getUserBasePp());
            ppText += " → " + currentFight.getUserFinalPp() + " PP (+" + percentIncrease + "% )";
        }
        tvUserPp.setText(ppText);

        // Player PP bar: clamp to 100 for UI
        int ppProgress = Math.min(100, currentFight.getUserFinalPp());
        playerPpBar.setProgress(ppProgress);

        tvAttacksRemaining.setText("Attacks: " + currentFight.getRemainingAttacks() + " / " + currentFight.getTotalAttacks());
        tvSuccessChance.setText("Hit Chance: " + currentFight.getSuccessChance() + "%");

        StringBuilder bonuses = new StringBuilder("⚔️ EQUIPMENT BONUSES ⚔️\n\n");

        boolean hasBonuses = false;

        if (currentFight.getUserFinalPp() > currentFight.getUserBasePp()) {
            int ppIncrease = currentFight.getUserFinalPp() - currentFight.getUserBasePp();
            int percentIncrease = (int) (((currentFight.getUserFinalPp() - currentFight.getUserBasePp()) * 100.0) / currentFight.getUserBasePp());
            bonuses.append("💪 Power: +").append(ppIncrease).append(" PP (+").append(percentIncrease).append("% )\n");
            hasBonuses = true;
        }

        if (currentFight.getAttackSuccessBonus() > 0) {
            bonuses.append("🎯 Hit Chance: +").append(currentFight.getAttackSuccessBonus()).append("%\n");
            hasBonuses = true;
        }

        if (currentFight.getExtraAttacks() > 0) {
            bonuses.append("⚡ Extra Attacks: +").append(currentFight.getExtraAttacks()).append("\n");
            hasBonuses = true;
        }

        if (currentFight.getCoinBonusPercent() > 0) {
            bonuses.append("💰 Coin Reward: +").append(currentFight.getCoinBonusPercent()).append("%\n");
            hasBonuses = true;
        }

        if (!hasBonuses) {
            bonuses.append("No equipment bonuses active.\nActivate equipment in your inventory!");
        }

        tvEquipmentBonuses.setText(bonuses.toString());
    }

    private void updateBossHp() {
        tvBossHp.setText("Boss HP: " + currentFight.getBossCurrentHp() + " / " + currentFight.getBossMaxHp());

        int progress = (int) ((currentFight.getBossCurrentHp() * 100.0) / currentFight.getBossMaxHp());
        bossHpBar.setProgress(progress);

        // If boss is low, flash sprite slightly
        if (currentFight.getBossCurrentHp() <= 0) {
            ivBossSprite.setAlpha(0.6f);
        } else if (progress < 30) {
            // subtle scale animation to show damage
            ScaleAnimation anim = new ScaleAnimation(1f, 1.08f, 1f, 1.08f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(200);
            anim.setRepeatCount(1);
            anim.setRepeatMode(Animation.REVERSE);
            ivBossSprite.startAnimation(anim);
        }
    }

    private void performAttack() {
        if (currentFight == null || !currentFight.hasAttacksRemaining()) {
            Toast.makeText(this, "No attacks remaining!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hit = fightRepo.performAttack(currentFight);

        if (hit) {
            animateHit();
            Toast.makeText(this, "HIT! Dealt " + currentFight.getUserFinalPp() + " damage!", Toast.LENGTH_SHORT).show();
        } else {
            animateMiss();
            Toast.makeText(this, "MISS! Attack failed.", Toast.LENGTH_SHORT).show();
        }

        updateBossHp();
        tvAttacksRemaining.setText("Attacks: " + currentFight.getRemainingAttacks() + " / " + currentFight.getTotalAttacks());

        if (currentFight.isBossDefeated() || !currentFight.hasAttacksRemaining()) {
            completeFight();
        }
    }

    private void animateHit() {
        // Boss sprite quick scale (hit)
        ScaleAnimation anim = new ScaleAnimation(1f, 0.92f, 1f, 0.92f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(120);
        anim.setRepeatCount(1);
        anim.setRepeatMode(Animation.REVERSE);
        ivBossSprite.startAnimation(anim);
    }

    private void animateMiss() {
        // Boss sprite small shake (miss)
        ScaleAnimation anim = new ScaleAnimation(1f, 1.02f, 1f, 1.02f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(120);
        anim.setRepeatCount(1);
        anim.setRepeatMode(Animation.REVERSE);
        ivBossSprite.startAnimation(anim);
    }

    private void completeFight() {
        btnAttack.setEnabled(false);

        fightRepo.completeBossFight(userId, currentFight, new BossFightRepository.OnFightCompletedListener() {
            @Override
            public void onCompleted(BossFight fight) {
                currentFight = fight;
                displayResults();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BossFightActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayResults() {
        resultContainer.setVisibility(View.VISIBLE);

        if (currentFight.isVictory()) {
            tvResult.setText("🎉 VICTORY! 🎉");
            tvResult.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvResult.setText("💀 DEFEAT 💀");
            tvResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        tvCoinsEarned.setText("Coins earned: " + currentFight.getCoinsEarned());


        if (currentFight.getItemDropped() != null && currentFight.getItemDroppedName() != null) {
            tvItemDropped.setText("Item dropped: " + currentFight.getItemDroppedName());
            tvItemDropped.setVisibility(View.VISIBLE);

            showDropOverlay(currentFight.getItemDropped(), currentFight.getItemDroppedName());
        } else {
            tvItemDropped.setVisibility(View.GONE);
        }

        // Show treasure chest for visual reward opening
        ivTreasureChest.setVisibility(View.VISIBLE);

        Toast.makeText(this,
                "Equipment effects applied!\nOne-time potions consumed.\nClothing durability reduced.",
                Toast.LENGTH_LONG).show();
    }

    private void openChest() {
        // simple scale pop animation and reveal
        ivTreasureChest.setClickable(false);
        ScaleAnimation anim = new ScaleAnimation(1f, 1.2f, 1f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(300);
        anim.setRepeatCount(0);
        ivTreasureChest.startAnimation(anim);

        // vibrate for feedback
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(120);

        // If there is an item drop, show the drop overlay after small delay
        mainHandler.postDelayed(() -> {
            if (currentFight.getItemDropped() != null) {
                showDropOverlay(currentFight.getItemDropped(), currentFight.getItemDroppedName());
            } else {
                // no item - briefly animate coins text
                tvCoinsEarned.setAlpha(0f);
                tvCoinsEarned.animate().alpha(1f).setDuration(400);
            }
        }, 350);
    }

    private void displayDropIcon(String itemId) {
        int imageRes = getImageResourceForItem(itemId);
        ivDroppedItem.setImageResource(imageRes);
    }

    private void showDropOverlay(String itemId, String itemName) {
        tvDroppedItemName.setText(itemName);

        int imageRes = getImageResourceForItem(itemId);
        ivDroppedItem.setImageResource(imageRes);

        dropOverlay.setVisibility(View.VISIBLE);
    }

    private int getImageResourceForItem(String itemId) {
        switch (itemId) {
            case "sword": return R.drawable.ic_sword;
            case "bow": return R.drawable.ic_bow;
            case "gloves": return R.drawable.ic_gloves;
            case "shield": return R.drawable.ic_shield;
            case "boots": return R.drawable.ic_boots;
            default: return R.drawable.ic_potion1;
        }
    }
}
