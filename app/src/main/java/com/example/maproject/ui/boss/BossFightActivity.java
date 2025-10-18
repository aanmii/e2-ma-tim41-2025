package com.example.maproject.ui.boss;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.example.maproject.data.BossFightRepository;
import com.example.maproject.model.BossFight;
import com.google.firebase.auth.FirebaseAuth;

public class BossFightActivity extends AppCompatActivity {

    // UI Components
    private TextView tvBossLevel, tvBossHp, tvUserPp, tvAttacksRemaining;
    private TextView tvSuccessChance, tvEquipmentBonuses;
    private ProgressBar bossHpBar;
    private Button btnAttack, btnFinishFight;
    private View resultContainer;
    private TextView tvResult, tvCoinsEarned, tvItemDropped;

    // Data
    private BossFightRepository fightRepo;
    private String userId;
    private BossFight currentFight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_fight);

        fightRepo = new BossFightRepository();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initViews();
        prepareFight();
    }

    private void initViews() {
        tvBossLevel = findViewById(R.id.tvBossLevel);
        tvBossHp = findViewById(R.id.tvBossHp);
        tvUserPp = findViewById(R.id.tvUserPp);
        tvAttacksRemaining = findViewById(R.id.tvAttacksRemaining);
        tvSuccessChance = findViewById(R.id.tvSuccessChance);
        tvEquipmentBonuses = findViewById(R.id.tvEquipmentBonuses);
        bossHpBar = findViewById(R.id.bossHpBar);
        btnAttack = findViewById(R.id.btnAttack);
        btnFinishFight = findViewById(R.id.btnFinishFight);

        resultContainer = findViewById(R.id.resultContainer);
        tvResult = findViewById(R.id.tvResult);
        tvCoinsEarned = findViewById(R.id.tvCoinsEarned);
        tvItemDropped = findViewById(R.id.tvItemDropped);

        btnAttack.setOnClickListener(v -> performAttack());
        btnFinishFight.setOnClickListener(v -> finish());

        resultContainer.setVisibility(View.GONE);
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
            ppText += " → " + currentFight.getUserFinalPp() + " PP (+" + percentIncrease + "%)";
        }
        tvUserPp.setText(ppText);

        tvAttacksRemaining.setText("Attacks: " + currentFight.getRemainingAttacks() + " / " + currentFight.getTotalAttacks());
        tvSuccessChance.setText("Hit Chance: " + currentFight.getSuccessChance() + "%");

        // Prikaži DETALJNE bonuse od opreme
        StringBuilder bonuses = new StringBuilder("⚔️ EQUIPMENT BONUSES ⚔️\n\n");

        boolean hasBonuses = false;

        if (currentFight.getUserFinalPp() > currentFight.getUserBasePp()) {
            int ppIncrease = currentFight.getUserFinalPp() - currentFight.getUserBasePp();
            int percentIncrease = (int) (((currentFight.getUserFinalPp() - currentFight.getUserBasePp()) * 100.0) / currentFight.getUserBasePp());
            bonuses.append("💪 Power: +").append(ppIncrease).append(" PP (+").append(percentIncrease).append("%)\n");
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
    }

    private void performAttack() {
        if (currentFight == null || !currentFight.hasAttacksRemaining()) {
            Toast.makeText(this, "No attacks remaining!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hit = fightRepo.performAttack(currentFight);

        if (hit) {
            Toast.makeText(this, "HIT! Dealt " + currentFight.getUserFinalPp() + " damage!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "MISS! Attack failed.", Toast.LENGTH_SHORT).show();
        }

        updateBossHp();
        tvAttacksRemaining.setText("Attacks: " + currentFight.getRemainingAttacks() + " / " + currentFight.getTotalAttacks());

        // Proveri da li je borba završena
        if (currentFight.isBossDefeated() || !currentFight.hasAttacksRemaining()) {
            completeFight();
        }
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

        if (currentFight.getItemDropped() != null) {
            tvItemDropped.setText("Item dropped: " + currentFight.getItemDropped());
            tvItemDropped.setVisibility(View.VISIBLE);
        } else {
            tvItemDropped.setVisibility(View.GONE);
        }

        Toast.makeText(this,
                "Equipment effects applied!\nOne-time potions consumed.\nClothing durability reduced.",
                Toast.LENGTH_LONG).show();
    }
}