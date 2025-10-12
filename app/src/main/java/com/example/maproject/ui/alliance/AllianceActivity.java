package com.example.maproject.ui.alliance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class AllianceActivity extends AppCompatActivity {

    private AllianceRepository allianceRepository;
    private String currentUserId;

    // Elementi za prikaz saveza
    private LinearLayout allianceLayout;
    private TextView allianceNameTextView, leaderNameTextView, membersTextView, missionStatusTextView;
    private Button leaveOrDeleteButton, startMissionButton, chatButton;
    private RecyclerView membersRecyclerView; // Za bolji prikaz članova

    // Elementi za prikaz pozivnica
    private LinearLayout invitationsLayout;
    private RecyclerView invitationsRecyclerView;
    private AllianceInvitationsAdapter invitationsAdapter;

    // Trenutni podaci
    private Alliance currentAlliance;
    private String currentUsername;

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
        // Prikaz saveza
        allianceLayout = findViewById(R.id.allianceLayout);
        allianceNameTextView = findViewById(R.id.allianceNameTextView);
        leaderNameTextView = findViewById(R.id.leaderNameTextView);
        membersTextView = findViewById(R.id.membersTextView);
        missionStatusTextView = findViewById(R.id.missionStatusTextView);
        leaveOrDeleteButton = findViewById(R.id.leaveOrDeleteButton);
        startMissionButton = findViewById(R.id.startMissionButton);
        chatButton = findViewById(R.id.chatButton);

        // Prikaz pozivnica
        invitationsLayout = findViewById(R.id.invitationsLayout);
        invitationsRecyclerView = findViewById(R.id.invitationsRecyclerView);

        // Inicijalno sakrij oba
        allianceLayout.setVisibility(View.GONE);
        invitationsLayout.setVisibility(View.GONE);
    }

    private void setupInvitationsRecyclerView() {
        // Ovde bi trebalo kreirati novi Adapter za pozivnice (AllianceInvitationsAdapter)
        // Za sada ćemo samo postaviti RecyclerView
        invitationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Adapter bi trebalo da omogući klik na PRIHVATI/ODBIJ
        // invitationsAdapter = new AllianceInvitationsAdapter(new ArrayList<>(), this::handleInvitationAction);
        // invitationsRecyclerView.setAdapter(invitationsAdapter);
    }

    private void loadCurrentUsername() {
        FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    currentUsername = doc.getString("username");
                });
    }

    private void observeAllianceAndInvitations() {
        // 1. Posmatraj trenutni savez
        MutableLiveData<Alliance> allianceLiveData = new MutableLiveData<>();
        allianceLiveData.observe(this, alliance -> {
            currentAlliance = alliance;
            if (alliance != null) {
                displayAllianceView(alliance);
            } else {
                // Ako nema saveza, prikaži pozivnice
                allianceLayout.setVisibility(View.GONE);
                observeInvitations();
            }
        });
        allianceRepository.loadUserAlliance(currentUserId, allianceLiveData);
    }

    private void observeInvitations() {
        // 2. Posmatraj pozivnice
        MutableLiveData<List<AllianceInvitation>> invitationsLiveData = new MutableLiveData<>();
        invitationsLiveData.observe(this, invitations -> {
            if (invitations != null && !invitations.isEmpty()) {
                invitationsLayout.setVisibility(View.VISIBLE);
                // Ažuriraj adapter
                // if (invitationsAdapter != null) invitationsAdapter.updateInvitations(invitations);
            } else {
                invitationsLayout.setVisibility(View.GONE);
                // Ovde možete prikazati dugme za "Kreiraj savez" ili "Pronađi savez"
            }
        });
        allianceRepository.loadInvitations(currentUserId, invitationsLiveData);
    }

    private void displayAllianceView(Alliance alliance) {
        invitationsLayout.setVisibility(View.GONE);
        allianceLayout.setVisibility(View.VISIBLE);

        allianceNameTextView.setText(alliance.getName());
        leaderNameTextView.setText("Vođa: " + alliance.getLeaderUsername());

        // Prikaz članova
        StringBuilder membersList = new StringBuilder("Članovi: ");
        for (Map.Entry<String, String> entry : alliance.getMembers().entrySet()) {
            membersList.append(entry.getValue()).append(", ");
        }
        membersTextView.setText(membersList.substring(0, membersList.length() - 2));

        // Status misije
        if (alliance.isMissionActive()) {
            missionStatusTextView.setText("Status: Misija je aktivna!");
            missionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            leaveOrDeleteButton.setEnabled(false); // Ne može napustiti/ukinuti
        } else {
            missionStatusTextView.setText("Status: Nema aktivne misije.");
            missionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            leaveOrDeleteButton.setEnabled(true);
        }

        // Dugmad za vođu vs. člana
        boolean isLeader = alliance.getLeaderId().equals(currentUserId);
        startMissionButton.setVisibility(isLeader ? View.VISIBLE : View.GONE);
        chatButton.setVisibility(View.VISIBLE);

        if (isLeader) {
            leaveOrDeleteButton.setText("UKINI SAVEZ");
            leaveOrDeleteButton.setOnClickListener(v -> deleteAlliance(alliance.getAllianceId()));
        } else {
            leaveOrDeleteButton.setText("NAPUSTI SAVEZ");
            leaveOrDeleteButton.setOnClickListener(v -> leaveAlliance(alliance.getAllianceId()));
        }

        chatButton.setOnClickListener(v -> openChat(alliance.getAllianceId(), alliance.getName()));
    }

    private void deleteAlliance(String allianceId) {
        MutableLiveData<Boolean> deleteLiveData = new MutableLiveData<>();
        deleteLiveData.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Savez je uspešno ukinut.", Toast.LENGTH_SHORT).show();
                // UI će se sam ažurirati jer Firestore listener radi
            } else {
                Toast.makeText(this, "Greška: Misija je aktivna ili niste vođa.", Toast.LENGTH_SHORT).show();
            }
        });
        allianceRepository.deleteAlliance(allianceId, deleteLiveData);
    }

    private void leaveAlliance(String allianceId) {
        // Implementacija napuštanja saveza (slično kao delete, ali samo za jednog korisnika)

        MutableLiveData<Boolean> leaveLiveData = new MutableLiveData<>();
        leaveLiveData.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Uspešno si napustio/la savez.", Toast.LENGTH_SHORT).show();
                // UI se automatski ažurira putem Firestore listenera
            } else {
                Toast.makeText(this, "Nije moguće napustiti: Misija je aktivna!", Toast.LENGTH_SHORT).show();
            }
        });

        // POZIV NOVE METODE U REPOSITORY-U
        allianceRepository.leaveAlliance(allianceId, currentUserId, leaveLiveData);
    }

    // Potrebna implementacija u Repository-u i ovde
    public void handleInvitationAction(AllianceInvitation invitation, String action) {
        if (action.equals("ACCEPT")) {
            MutableLiveData<Boolean> acceptLiveData = new MutableLiveData<>();
            acceptLiveData.observe(this, success -> {
                if (success) {
                    Toast.makeText(this, "Pridružio si se savezu " + invitation.getAllianceName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Nije moguće: Misija u starom savezu je aktivna!", Toast.LENGTH_LONG).show();
                }
            });
            allianceRepository.acceptInvitation(invitation, currentUserId, currentUsername, acceptLiveData);
        } else { // REJECT
            MutableLiveData<Boolean> rejectLiveData = new MutableLiveData<>();
            rejectLiveData.observe(this, success -> {
                if (success) {
                    Toast.makeText(this, "Pozivnica odbijena.", Toast.LENGTH_SHORT).show();
                }
            });
            allianceRepository.rejectInvitation(invitation.getInvitationId(), rejectLiveData);
        }
    }

    private void openChat(String allianceId, String allianceName) {
        // Ovde ide Intent za otvaranje ChatActivity.java
        // Intent intent = new Intent(this, AllianceChatActivity.class);
        // intent.putExtra("ALLIANCE_ID", allianceId);
        // intent.putExtra("ALLIANCE_NAME", allianceName);
        // startActivity(intent);
        Toast.makeText(this, "Otvaram chat za savez: " + allianceName, Toast.LENGTH_SHORT).show();
    }


}