package com.example.maproject.ui.model;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.example.maproject.R;

/**
 * SpecialMissionActivity
 * - Demo implementation of Alliance Special Mission (shortened durations for demo).
 */
public class SpecialMissionActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String allianceId;
    private boolean isLeader = false;

    private TextView statusText;
    private TextView bossHPText;
    private TextView membersCountText;
    private ProgressBar bossProgress;
    private LinearLayout playersLayout;

    // Buttons (class fields so we can enable/disable them)
    private MaterialButton startBtn;
    private MaterialButton storeBtn;
    private MaterialButton bossHitBtn;
    private MaterialButton easyBtn;
    private MaterialButton normalBtn;
    private MaterialButton importantBtn;
    private MaterialButton otherBtn;
    private MaterialButton noUnfinishedBtn;
    private MaterialButton msgBtn;

    private DocumentReference missionRef;

    // Demo mission duration: 2 minutes (for demonstration instead of 2 weeks)
    private static final long DEMO_DURATION_MS = 2 * 60 * 1000L;

    // Action specs (per player caps and damage)
    private static final String KEY_STORE = "storePurchase"; // max 5, -2 each
    private static final String KEY_BOSS_HIT = "bossHit"; // max 10, -2 each
    private static final String KEY_TASK_EASY = "taskEasy"; // max 10, -2 each (easy counts double)
    private static final String KEY_TASK_NORMAL = "taskNormal"; // max 10, -2 each (normal counts double)
    private static final String KEY_TASK_IMPORTANT = "taskImportant"; // max 10, -1 each
    private static final String KEY_TASK_OTHER = "taskOther"; // max 6, -4 each
    private static final String KEY_NO_UNFINISHED = "noUnfinished"; // max 1, -10 once
    private static final String KEY_MSG_DAY = "msgPerDay"; // 1 per day -> demo: 1 per minute, -4 each

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = auth.getCurrentUser().getUid();

        // Inflate new layout to match app styling
        setContentView(R.layout.activity_special_mission);

        // Bind views
        statusText = findViewById(R.id.missionStatusText);
        bossHPText = findViewById(R.id.bossHPText);
        membersCountText = findViewById(R.id.membersCountText);
        bossProgress = findViewById(R.id.bossProgressBar);
        playersLayout = findViewById(R.id.playersLayout);

        startBtn = findViewById(R.id.startMissionButton);
        storeBtn = findViewById(R.id.storeButton);
        bossHitBtn = findViewById(R.id.bossHitButton);
        easyBtn = findViewById(R.id.easyTaskButton);
        normalBtn = findViewById(R.id.normalTaskButton);
        importantBtn = findViewById(R.id.importantTaskButton);
        otherBtn = findViewById(R.id.otherTaskButton);
        noUnfinishedBtn = findViewById(R.id.noUnfinishedButton);
        msgBtn = findViewById(R.id.msgButton);

        startBtn.setOnClickListener(v -> startMissionClicked());
        storeBtn.setOnClickListener(v -> performAction(KEY_STORE, 1, -2, 5));
        bossHitBtn.setOnClickListener(v -> performAction(KEY_BOSS_HIT, 1, -2, 10));
        easyBtn.setOnClickListener(v -> performAction(KEY_TASK_EASY, 1, -2, 10));
        normalBtn.setOnClickListener(v -> performAction(KEY_TASK_NORMAL, 1, -2, 10));
        importantBtn.setOnClickListener(v -> performAction(KEY_TASK_IMPORTANT, 1, -1, 10));
        otherBtn.setOnClickListener(v -> performAction(KEY_TASK_OTHER, 1, -4, 6));
        noUnfinishedBtn.setOnClickListener(v -> performAction(KEY_NO_UNFINISHED, 1, -10, 1));
        msgBtn.setOnClickListener(v -> performAction(KEY_MSG_DAY, 1, -4, 9999));

        // Initially disable action buttons until we know mission state
        setActionButtonsEnabled(false);
        startBtn.setEnabled(false);

        loadUserAllianceAndMission();
    }

    private void setActionButtonsEnabled(boolean enabled) {
        storeBtn.setEnabled(enabled);
        bossHitBtn.setEnabled(enabled);
        easyBtn.setEnabled(enabled);
        normalBtn.setEnabled(enabled);
        importantBtn.setEnabled(enabled);
        otherBtn.setEnabled(enabled);
        noUnfinishedBtn.setEnabled(enabled);
        msgBtn.setEnabled(enabled);
    }

    private void loadUserAllianceAndMission() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    allianceId = documentSnapshot.getString("allianceId");
                    Boolean leader = documentSnapshot.getBoolean("isLeader");
                    isLeader = leader != null && leader;

                    if (allianceId == null || allianceId.isEmpty()) {
                        statusText.setText("You are not in an alliance");
                        return;
                    }

                    missionRef = db.collection("specialMissions").document(allianceId);

                    // Listen for mission changes
                    missionRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                statusText.setText("Error listening to mission updates: " + e.getMessage());
                                setActionButtonsEnabled(false);
                                startBtn.setEnabled(isLeader);
                                return;
                            }
                            if (snapshot == null || !snapshot.exists()) {
                                statusText.setText("No active special mission. Leader can start one.");
                                bossHPText.setText("Boss HP: --");
                                bossProgress.setProgress(0);
                                membersCountText.setText("Members: --");
                                playersLayout.removeAllViews();
                                setActionButtonsEnabled(false);
                                startBtn.setEnabled(isLeader);
                                return;
                            }

                            boolean active = Boolean.TRUE.equals(snapshot.getBoolean("active"));
                            Long bossHPobj = snapshot.getLong("bossHP");
                            long bossHP = bossHPobj != null ? bossHPobj : 0L;
                            Long maxHPobj = snapshot.getLong("maxHP");
                            long maxHP = maxHPobj != null ? maxHPobj : bossHP;
                            Long endAtObj = snapshot.getLong("endAt");
                            long endAt = endAtObj != null ? endAtObj : 0L;
                            Long membersCountObj = snapshot.getLong("membersCount");
                            int membersCount = membersCountObj != null ? membersCountObj.intValue() : 0;

                            statusText.setText(active ? "Special Mission is ACTIVE" : "Special Mission is INACTIVE");
                            bossHPText.setText("Boss HP: " + bossHP + " / " + maxHP);
                            membersCountText.setText("Members: " + membersCount + "  (Leader: " + (isLeader ? "Yes" : "No") + ")");

                            int percent = maxHP > 0 ? (int) Math.max(0, Math.min(100, (100 - (bossHP * 100 / maxHP)))) : 0;
                            bossProgress.setProgress(percent);

                            // enable/disable controls based on mission state
                            setActionButtonsEnabled(active);
                            startBtn.setEnabled(!active && isLeader);

                            if (active && (bossHP <= 0 || System.currentTimeMillis() >= endAt)) {
                                // mission ended -> try finalize
                                finalizeMission(snapshot);
                            }

                            // refresh players progress
                            loadPlayersProgress();
                        }
                    });

                })
                .addOnFailureListener(e -> {
                    statusText.setText("Error loading user: " + e.getMessage());
                });
    }

    private void loadPlayersProgress() {
        if (allianceId == null) return;
        db.collection("specialMissions").document(allianceId).collection("players")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    playersLayout.removeAllViews();
                    List<String> playerIds = new ArrayList<>();
                    for (DocumentSnapshot p : queryDocumentSnapshots.getDocuments()) {
                        playerIds.add(p.getId());
                    }
                    if (playerIds.isEmpty()) return;

                    // Firestore whereIn supports up to 10 items; chunk if needed
                    List<List<String>> chunks = chunkList(playerIds, 10);
                    Map<String, String> idToName = new HashMap<>();

                    for (List<String> chunk : chunks) {
                        db.collection("users").whereIn(FieldPath.documentId(), chunk)
                                .get()
                                .addOnSuccessListener(usersSnap -> {
                                    for (DocumentSnapshot u : usersSnap.getDocuments()) {
                                        String uid = u.getId();
                                        String name = u.getString("username");
                                        if (name == null) name = uid;
                                        idToName.put(uid, name);
                                    }
                                    // Once names collected for this chunk, update UI
                                    // but to keep it simple we rebuild the whole players list each chunk; it's acceptable for demo
                                    playersLayout.removeAllViews();
                                    for (DocumentSnapshot p : queryDocumentSnapshots.getDocuments()) {
                                        String uid = p.getId();
                                        Long dmgObj = p.getLong("totalDamage");
                                        long dmg = dmgObj != null ? dmgObj : 0L;
                                        String name = idToName.getOrDefault(uid, uid);
                                        TextView tv = new TextView(this);
                                        tv.setText(name + ": " + dmg + " dmg");
                                        tv.setPadding(8, 8, 8, 8);
                                        playersLayout.addView(tv);
                                    }
                                });
                    }
                });
    }

    private static List<List<String>> chunkList(List<String> list, int chunkSize) {
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            out.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + chunkSize))));
        }
        return out;
    }

    private void startMissionClicked() {
        if (!isLeader) {
            Toast.makeText(this, "Only the alliance leader can start a special mission", Toast.LENGTH_SHORT).show();
            return;
        }
        if (allianceId == null) return;

        // Count members (query users where allianceId == this)
        db.collection("users").whereEqualTo("allianceId", allianceId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int membersCount = queryDocumentSnapshots.size();
                    if (membersCount <= 0) membersCount = 1;
                    long hp = 100L * membersCount;
                    long now = System.currentTimeMillis();
                    long end = now + DEMO_DURATION_MS; // demo duration replaced

                    Map<String, Object> mission = new HashMap<>();
                    mission.put("active", true);
                    mission.put("bossHP", hp);
                    mission.put("maxHP", hp);
                    mission.put("startAt", now);
                    mission.put("endAt", end);
                    mission.put("membersCount", membersCount);

                    missionRef.set(mission)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Special mission started (demo duration)", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to start mission: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to count alliance members: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void performAction(String key, int countRequested, int damagePer, int maxPerPlayer) {
        if (missionRef == null) {
            Toast.makeText(this, "No mission reference", Toast.LENGTH_SHORT).show();
            return;
        }

        final DocumentReference playerRef = missionRef.collection("players").document(currentUserId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot missionSnap = transaction.get(missionRef);
            if (!missionSnap.exists()) {
                throw new FirebaseFirestoreException("No active mission", FirebaseFirestoreException.Code.NOT_FOUND);
            }
            boolean active = Boolean.TRUE.equals(missionSnap.getBoolean("active"));
            Long bossHPobj = missionSnap.getLong("bossHP");
            long bossHP = bossHPobj != null ? bossHPobj : 0L;
            Long endAtObj = missionSnap.getLong("endAt");
            long endAt = endAtObj != null ? endAtObj : 0L;
            if (!active || System.currentTimeMillis() >= endAt || bossHP <= 0) {
                throw new FirebaseFirestoreException("Mission is not active", FirebaseFirestoreException.Code.ABORTED);
            }

            DocumentSnapshot playerSnap = transaction.get(playerRef);
            Long usedObj = playerSnap.exists() ? playerSnap.getLong(key) : null;
            long used = usedObj != null ? usedObj : 0L;

            long canUse = Math.max(0, maxPerPlayer - used);
            if (canUse <= 0) {
                // Can't use more of this action
                return null; // no-op: we'll notify outside
            }

            int applied = (int) Math.min(canUse, countRequested);
            long totalDamage = applied * (long) Math.abs(damagePer);

            long newBossHP = bossHP - totalDamage;
            if (newBossHP < 0) newBossHP = 0;

            // update mission bossHP
            Map<String, Object> missionUpdates = new HashMap<>();
            missionUpdates.put("bossHP", newBossHP);
            transaction.update(missionRef, missionUpdates);

            // update player counters
            Map<String, Object> playerUpdates = new HashMap<>();
            long prevTotal = playerSnap.exists() && playerSnap.getLong("totalDamage") != null ? playerSnap.getLong("totalDamage") : 0L;
            long prevKey = playerSnap.exists() && playerSnap.getLong(key) != null ? playerSnap.getLong(key) : 0L;
            playerUpdates.put(key, prevKey + applied);
            playerUpdates.put("totalDamage", prevTotal + totalDamage);
            transaction.set(playerRef, playerUpdates, com.google.firebase.firestore.SetOptions.merge());

            return null;
        }).addOnSuccessListener(aVoid -> {
            // success
            Toast.makeText(this, "Action applied", Toast.LENGTH_SHORT).show();
            // refresh players
            loadPlayersProgress();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Action failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void finalizeMission(DocumentSnapshot missionSnapshot) {
        boolean active = Boolean.TRUE.equals(missionSnapshot.getBoolean("active"));
        if (!active) return;

        Long bossHPobj = missionSnapshot.getLong("bossHP");
        long bossHP = bossHPobj != null ? bossHPobj : 0L;

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot fresh = transaction.get(missionRef);
            if (!fresh.exists()) return null;
            boolean nowActive = Boolean.TRUE.equals(fresh.getBoolean("active"));
            if (!nowActive) return null;

            long freshBossHP = fresh.getLong("bossHP") != null ? fresh.getLong("bossHP") : 0L;
            transaction.update(missionRef, "active", false);

            if (freshBossHP <= 0) {
                // Boss defeated -> award members
                QuerySnapshot members = db.collection("users").whereEqualTo("allianceId", allianceId).get().getResult();
                if (members != null) {
                    for (DocumentSnapshot userDoc : members.getDocuments()) {
                        DocumentReference userRef = db.collection("users").document(userDoc.getId());
                        long coins = userDoc.getLong("coins") != null ? userDoc.getLong("coins") : 0L;
                        long badgeCount = userDoc.getLong("specialMissionsCompleted") != null ? userDoc.getLong("specialMissionsCompleted") : 0L;

                        long nextBossCoins = 200; // default for demo
                        long addCoins = (long) (nextBossCoins * 0.5);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("coins", coins + addCoins);
                        updates.put("specialMissionsCompleted", badgeCount + 1);
                        transaction.update(userRef, updates);
                    }
                }
            }

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, bossHP <= 0 ? "Mission ended: Boss defeated! Rewards distributed." : "Mission ended.", Toast.LENGTH_LONG).show();
            // refresh
            loadPlayersProgress();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to finalize mission: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
