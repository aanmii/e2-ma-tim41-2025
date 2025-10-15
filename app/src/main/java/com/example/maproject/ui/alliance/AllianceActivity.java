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

                // Proveri da li korisnik ima savez
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

    private void handleAcceptInvitation(AllianceInvitation invitation) {
        // Prvo proveri da li korisnik već ima savez
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String currentAllianceId = userDoc.getString("currentAllianceId");

                    if (currentAllianceId != null && !currentAllianceId.isEmpty()) {
                        // Korisnik je već u savezu, proveri da li je misija aktivna
                        checkCurrentAllianceStatus(currentAllianceId, invitation);
                    } else {
                        // Korisnik nije u savezu, prihvati odmah
                        acceptInvitationDirectly(invitation);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška pri proveri statusa saveza", Toast.LENGTH_SHORT).show();
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
                            // Misija je aktivna, ne može napustiti
                            showMissionActiveDialog(currentAllianceName);
                        } else {
                            // Misija nije aktivna, ponudi opcije
                            showSwitchAllianceDialog(currentAllianceName, newInvitation);
                        }
                    } else {
                        // Trenutni savez ne postoji, prihvati novi
                        acceptInvitationDirectly(newInvitation);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška pri proveri misije", Toast.LENGTH_SHORT).show();
                });
    }

    private void showMissionActiveDialog(String currentAllianceName) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Aktivna Misija")
                .setMessage("Trenutno si u savezu \"" + currentAllianceName +
                        "\" koji ima aktivnu misiju.\n\nNe možeš napustiti savez dok je misija u toku!")
                .setPositiveButton("U redu", null)
                .show();
    }

    private void showSwitchAllianceDialog(String currentAllianceName, AllianceInvitation newInvitation) {
        new AlertDialog.Builder(this)
                .setTitle("⚔️ Već si u Savezu")
                .setMessage("Trenutno si član saveza \"" + currentAllianceName + "\".\n\n" +
                        newInvitation.getSenderUsername() + " te poziva u savez \"" +
                        newInvitation.getAllianceName() + "\".\n\n" +
                        "Šta želiš da uradiš?")
                .setCancelable(false)
                .setPositiveButton("Napusti trenutni i prihvati novi", (dialog, which) -> {
                    acceptInvitationDirectly(newInvitation);
                })
                .setNegativeButton("Ostani u trenutnom savezu", (dialog, which) -> {
                    handleRejectInvitation(newInvitation);
                })
                .setNeutralButton("Otkaži", null)
                .show();
    }

    private void acceptInvitationDirectly(AllianceInvitation invitation) {
        allianceRepository.respondToInvitation(invitation, currentUserId, currentUsername, success -> {
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Uspešno prihvaćen poziv u savez " + invitation.getAllianceName(), Toast.LENGTH_SHORT).show();
                    observeAllianceAndInvitations();
                } else {
                    Toast.makeText(this, "Greška pri prihvatanju poziva", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void handleRejectInvitation(AllianceInvitation invitation) {
        if (invitation.getInvitationId() == null || invitation.getInvitationId().isEmpty()) {
            Toast.makeText(this, "Greška: Neispravan poziv", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("invitations").document(invitation.getInvitationId())
                .update("status", "REJECTED")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Poziv odbijen", Toast.LENGTH_SHORT).show();
                    observeInvitations();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška pri odbijanju poziva", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteAlliance(String allianceId) {
        allianceRepository.deleteAlliance(allianceId, success -> {
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Savez ukinut", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Ne može se ukinuti: misija aktivna", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void leaveAlliance(String allianceId) {
        allianceRepository.leaveAlliance(allianceId, currentUserId, success -> {
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Napustio/la si savez", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Ne može se napustiti: misija aktivna", Toast.LENGTH_SHORT).show();
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