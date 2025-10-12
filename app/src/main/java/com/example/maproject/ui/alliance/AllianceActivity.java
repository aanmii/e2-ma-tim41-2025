package com.example.maproject.ui.alliance;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.data.AllianceRepository;
import com.example.maproject.model.Alliance;
import com.example.maproject.model.AllianceInvitation;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Map;

public class AllianceActivity extends AppCompatActivity {

    private AllianceRepository allianceRepository;
    private String currentUserId;
    private String currentUsername;

    // UI elementi
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
        invitationsAdapter = new AllianceInvitationsAdapter(invitation -> handleInvitationAction(invitation));
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
            } else {
                invitationsLayout.setVisibility(View.GONE);
            }
        });
        allianceRepository.loadInvitations(currentUserId, invitationsLiveData);
    }

    private void displayAllianceView(Alliance alliance) {
        invitationsLayout.setVisibility(View.GONE);
        allianceLayout.setVisibility(View.VISIBLE);

        allianceNameTextView.setText(alliance.getName());
        leaderNameTextView.setText("Vođa: " + alliance.getLeaderUsername());

        StringBuilder membersList = new StringBuilder("Članovi: ");
        for (Map.Entry<String, String> entry : alliance.getMembers().entrySet()) {
            membersList.append(entry.getValue()).append(", ");
        }
        membersTextView.setText(membersList.substring(0, membersList.length() - 2));

        if (alliance.isMissionActive()) {
            missionStatusTextView.setText("Status: Misija je aktivna!");
            missionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            leaveOrDeleteButton.setEnabled(false);
        } else {
            missionStatusTextView.setText("Status: Nema aktivne misije.");
            missionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            leaveOrDeleteButton.setEnabled(true);
        }

        boolean isLeader = alliance.getLeaderId().equals(currentUserId);
        startMissionButton.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        chatButton.setVisibility(View.VISIBLE);

        leaveOrDeleteButton.setText(isLeader ? "UKINI SAVEZ" : "NAPUSTI SAVEZ");
        leaveOrDeleteButton.setOnClickListener(v -> {
            if (isLeader) deleteAlliance(alliance.getAllianceId());
            else leaveAlliance(alliance.getAllianceId());
        });

        chatButton.setOnClickListener(v -> openChat(alliance.getAllianceId(), alliance.getName()));
    }

    private void handleInvitationAction(AllianceInvitation invitation) {
        allianceRepository.respondToInvitation(invitation, currentUserId, currentUsername, success -> {
            if (success) {
                Toast.makeText(this, "Uspešno prihvaćen poziv u savez " + invitation.getAllianceName(), Toast.LENGTH_SHORT).show();
                observeAllianceAndInvitations(); // refresh UI
            } else {
                Toast.makeText(this, "Nije moguće: Misija u starom savezu je aktivna!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteAlliance(String allianceId) {
        allianceRepository.deleteAlliance(allianceId, success -> {
            if (success) Toast.makeText(this, "Savez ukinut", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Ne može se ukinuti: misija aktivna", Toast.LENGTH_SHORT).show();
        });
    }

    private void leaveAlliance(String allianceId) {
        allianceRepository.leaveAlliance(allianceId, currentUserId, success -> {
            if (success) Toast.makeText(this, "Napustio/la si savez", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Ne može se napustiti: misija aktivna", Toast.LENGTH_SHORT).show();
        });
    }

    private void openChat(String allianceId, String allianceName) {
        Toast.makeText(this, "Otvaram chat za savez: " + allianceName, Toast.LENGTH_SHORT).show();
    }
}
