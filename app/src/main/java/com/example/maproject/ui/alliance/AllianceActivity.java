package com.example.maproject.ui.alliance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.data.AllianceRepository;
import com.example.maproject.model.Alliance;
import com.example.maproject.model.AllianceInvitation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class AllianceActivity extends AppCompatActivity {

    private AllianceRepository allianceRepository;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUsername;

    private View allianceLayout, invitationsLayout;
    private TextView allianceNameTextView, leaderNameTextView, membersTextView, missionStatusTextView;
    private Button leaveOrDeleteButton, startMissionButton, chatButton;
    private RecyclerView invitationsRecyclerView;
    private AllianceInvitationsAdapter invitationsAdapter;

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
        setupInvitationsRecyclerView();
        observeAllianceAndInvitations();
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

        invitationsLayout = findViewById(R.id.invitationsLayout);
        invitationsRecyclerView = findViewById(R.id.invitationsRecyclerView);

        allianceLayout.setVisibility(View.GONE);
        invitationsLayout.setVisibility(View.GONE);
    }

    private void loadCurrentUsername() {
        allianceRepository.getUsername(currentUserId, username -> currentUsername = username);
    }

    private void setupInvitationsRecyclerView() {
        invitationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        invitationsAdapter = new AllianceInvitationsAdapter(new AllianceInvitationsAdapter.InvitationActionListener() {
            @Override
            public void onAccept(AllianceInvitation invitation) {
                handleAcceptInvitation(invitation);
            }

            @Override
            public void onReject(AllianceInvitation invitation) {
                handleRejectInvitation(invitation);
            }
        });
        invitationsRecyclerView.setAdapter(invitationsAdapter);
    }

    private void observeAllianceAndInvitations() {
        MutableLiveData<Alliance> allianceLiveData = new MutableLiveData<>();
        allianceLiveData.observe(this, alliance -> {
            currentAlliance = alliance;
            if (alliance != null) displayAllianceView(alliance);
            else {
                allianceLayout.setVisibility(View.GONE);
                observeInvitations();
            }
        });
        allianceRepository.loadUserAlliance(currentUserId, allianceLiveData);
    }

    private void observeInvitations() {
        MutableLiveData<List<AllianceInvitation>> invitationsLiveData = new MutableLiveData<>();
        invitationsLiveData.observe(this, invitations -> {
            if (invitations != null && !invitations.isEmpty()) {
                invitationsLayout.setVisibility(View.VISIBLE);
                invitationsAdapter.updateInvitations(invitations);

                checkIfUserHasAlliance();
            } else {
                invitationsLayout.setVisibility(View.GONE);
            }
        });
        allianceRepository.loadInvitations(currentUserId, invitationsLiveData);
    }

    private void checkIfUserHasAlliance() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String allianceId = userDoc.getString("currentAllianceId");
                    boolean hasAlliance = (allianceId != null && !allianceId.isEmpty());
                    invitationsAdapter.setUserHasAlliance(hasAlliance);
                });
    }

    private void displayAllianceView(Alliance alliance) {
        invitationsLayout.setVisibility(View.GONE);
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

    private void handleAcceptInvitation(AllianceInvitation invitation) {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String currentAllianceId = userDoc.getString("currentAllianceId");

                    if (currentAllianceId != null && !currentAllianceId.isEmpty()) {
                        checkCurrentAllianceStatus(currentAllianceId, invitation);
                    } else {
                        acceptInvitationDirectly(invitation);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error occurred while checking the alliance status", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkCurrentAllianceStatus(String currentAllianceId, AllianceInvitation newInvitation) {
        db.collection("alliances").document(currentAllianceId)
                .get()
                .addOnSuccessListener(allianceDoc -> {
                    if (allianceDoc.exists()) {
                        String currentAllianceName = allianceDoc.getString("name");
                        Boolean isMissionActive = allianceDoc.getBoolean("missionActive");

                        if (Boolean.TRUE.equals(isMissionActive)) {
                            showMissionActiveDialog(currentAllianceName);
                        } else {
                            showSwitchAllianceDialog(currentAllianceName, newInvitation);
                        }
                    } else {
                        acceptInvitationDirectly(newInvitation);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error occurred while checking the mission status", Toast.LENGTH_SHORT).show();
                });
    }

    private void showMissionActiveDialog(String currentAllianceName) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Active mission")
                .setMessage("You are currently in alliance \"" + currentAllianceName +
                        "\" which has active mission.\n\nU cant leave alliance while the mission is active!")
                .setPositiveButton("Okay", null)
                .show();
    }

    private void showSwitchAllianceDialog(String currentAllianceName, AllianceInvitation newInvitation) {
        new AlertDialog.Builder(this)
                .setTitle("⚔️ You are already a member in a different alliance")
                .setMessage("You are currently in alliance \"" + currentAllianceName + "\".\n\n" +
                        newInvitation.getSenderUsername() + " invites you in an alliance \"" +
                        newInvitation.getAllianceName() + "\".\n\n" +
                        "What do you want to do?")
                .setCancelable(false)
                .setPositiveButton("Leave your current alliance", (dialog, which) -> {
                    acceptInvitationDirectly(newInvitation);
                })
                .setNegativeButton("Stay in your current alliance", (dialog, which) -> {
                    handleRejectInvitation(newInvitation);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void acceptInvitationDirectly(AllianceInvitation invitation) {
        allianceRepository.respondToInvitation(invitation, currentUserId, currentUsername, success -> {
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "You are successfully added in alliance " + invitation.getAllianceName(), Toast.LENGTH_SHORT).show();
                    observeAllianceAndInvitations();
                } else {
                    Toast.makeText(this, "Error while adding in alliance", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void handleRejectInvitation(AllianceInvitation invitation) {
        if (invitation.getInvitationId() == null || invitation.getInvitationId().isEmpty()) {
            Toast.makeText(this, "Error: Incomplete call", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("invitations").document(invitation.getInvitationId())
                .update("status", "REJECTED")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Invitation rejected", Toast.LENGTH_SHORT).show();
                    observeInvitations();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error while rejecting the invite", Toast.LENGTH_SHORT).show();
                });
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