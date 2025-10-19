package com.example.maproject.ui.alliance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.example.maproject.R;
import com.example.maproject.data.AllianceRepository;
import com.example.maproject.model.Alliance;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class AllianceActivity extends AppCompatActivity {

    private AllianceRepository allianceRepository;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUsername;

    private View allianceLayout;
    private TextView allianceNameTextView, leaderNameTextView, membersTextView, missionStatusTextView;
    private Button leaveOrDeleteButton, startMissionButton, chatButton;

    private Alliance currentAlliance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        allianceRepository = new AllianceRepository();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadCurrentUsername();
        observeCurrentAlliance();
    }

    private void initViews() {
        allianceLayout = findViewById(R.id.allianceLayout);
        allianceNameTextView = findViewById(R.id.allianceNameTextView);
        leaderNameTextView = findViewById(R.id.leaderNameTextView);
        membersTextView = findViewById(R.id.membersTextView);
        missionStatusTextView = findViewById(R.id.missionStatusTextView);
        leaveOrDeleteButton = findViewById(R.id.leaveOrDeleteButton);
        startMissionButton = findViewById(R.id.startMissionButton);
        chatButton = findViewById(R.id.chatButton);

        allianceLayout.setVisibility(View.GONE);
    }

    private void loadCurrentUsername() {
        allianceRepository.getUsername(currentUserId, username -> currentUsername = username);
    }

    private void observeCurrentAlliance() {
        MutableLiveData<Alliance> allianceLiveData = new MutableLiveData<>();
        allianceLiveData.observe(this, alliance -> {
            currentAlliance = alliance;
            if (alliance != null) {
                displayAllianceView(alliance);
            } else {
                Toast.makeText(this, "You are not in any alliance", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        allianceRepository.loadUserAlliance(currentUserId, allianceLiveData);
    }

    private void displayAllianceView(Alliance alliance) {
        allianceLayout.setVisibility(View.VISIBLE);

        allianceNameTextView.setText(alliance.getName());
        leaderNameTextView.setText("Leader: " + alliance.getLeaderUsername());

        StringBuilder membersList = new StringBuilder("Members: ");
        for (Map.Entry<String, String> entry : alliance.getMembers().entrySet()) {
            membersList.append(entry.getValue()).append(", ");
        }
        membersTextView.setText(membersList.substring(0, membersList.length() - 2));

        if (alliance.isMissionActive()) {
            missionStatusTextView.setText("Status: Mission is active!");
            missionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            leaveOrDeleteButton.setEnabled(false);
        } else {
            missionStatusTextView.setText("Status: No active mission.");
            missionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            leaveOrDeleteButton.setEnabled(true);
        }

        boolean isLeader = alliance.getLeaderId().equals(currentUserId);
        startMissionButton.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        chatButton.setVisibility(View.VISIBLE);

        leaveOrDeleteButton.setText(isLeader ? "DELETE ALLIANCE" : "LEAVE ALLIANCE");
        leaveOrDeleteButton.setOnClickListener(v -> {
            if (isLeader) deleteAlliance(alliance.getAllianceId());
            else leaveAlliance(alliance.getAllianceId());
        });

        chatButton.setOnClickListener(v -> openChat(alliance.getAllianceId(), alliance.getName()));
    }

    private void deleteAlliance(String allianceId) {
        allianceRepository.deleteAlliance(allianceId, success -> {
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Alliance deleted", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "You cant delete alliance while the mission is active!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void leaveAlliance(String allianceId) {
        allianceRepository.leaveAlliance(allianceId, currentUserId, success -> {
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Alliance left", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "You cant leave the alliance while the mission is active", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void openChat(String allianceId, String allianceName) {
        Intent intent = new Intent(this, AllianceChatActivity.class);
        intent.putExtra("ALLIANCE_ID", allianceId);
        intent.putExtra("ALLIANCE_NAME", allianceName);
        startActivity(intent);
    }
}