package com.example.maproject.ui.alliance;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.data.AllianceRepository;
import com.example.maproject.data.FriendsRepository;
import com.example.maproject.data.NotificationRepository;
import com.example.maproject.model.Alliance;
import com.example.maproject.model.AllianceInvitation;
import com.example.maproject.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CreateAllianceActivity extends AppCompatActivity {

    private EditText allianceNameEditText;
    private RecyclerView friendsRecyclerView;
    private Button createButton, backButton;

    private SelectFriendsAdapter selectFriendsAdapter;
    private List<User> selectedFriends;

    private FriendsRepository friendsRepository;
    private AllianceRepository allianceRepository;
    private NotificationRepository notificationRepository;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_alliance);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        friendsRepository = new FriendsRepository();
        allianceRepository = new AllianceRepository();
        notificationRepository = new NotificationRepository();
        db = FirebaseFirestore.getInstance();
        selectedFriends = new ArrayList<>();

        loadCurrentUsername();
        initViews();
        setupRecyclerView();
        loadFriends();
        setupButtons();
    }

    private void loadCurrentUsername() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> currentUsername = doc.getString("username"));
    }

    private void initViews() {
        allianceNameEditText = findViewById(R.id.allianceNameEditText);
        friendsRecyclerView = findViewById(R.id.selectFriendsRecyclerView);
        createButton = findViewById(R.id.createAllianceButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupRecyclerView() {
        selectFriendsAdapter = new SelectFriendsAdapter(new ArrayList<>(), this::onFriendSelected);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendsRecyclerView.setAdapter(selectFriendsAdapter);
    }

    private void loadFriends() {
        MutableLiveData<List<User>> friendsLiveData = new MutableLiveData<>();
        friendsLiveData.observe(this, friends -> selectFriendsAdapter.updateFriends(friends));
        friendsRepository.loadFriends(currentUserId, friendsLiveData);
    }

    private void onFriendSelected(User friend, boolean isSelected) {
        if (isSelected) selectedFriends.add(friend);
        else selectedFriends.remove(friend);
    }

    private void setupButtons() {
        createButton.setOnClickListener(v -> createAlliance());
        backButton.setOnClickListener(v -> finish());
    }

    private void createAlliance() {
        String name = allianceNameEditText.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Unesi naziv saveza", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFriends.isEmpty()) {
            Toast.makeText(this, "Izaberi bar jednog prijatelja", Toast.LENGTH_SHORT).show();
            return;
        }

        Alliance alliance = new Alliance(name, currentUserId, currentUsername);

        allianceRepository.createAlliance(alliance, success -> {
            if (success) {
                sendInvitations(alliance.getAllianceId(), alliance.getName());
            } else {
                runOnUiThread(() ->
                        Toast.makeText(CreateAllianceActivity.this, "Greška pri kreiranju saveza", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void sendInvitations(String allianceId, String allianceName) {
        int totalInvitations = selectedFriends.size();
        int[] sentCount = {0};

        for (User friend : selectedFriends) {
            AllianceInvitation invitation = new AllianceInvitation(
                    allianceId, allianceName, currentUserId, currentUsername, friend.getUserId()
            );

            allianceRepository.sendInvitation(invitation, success -> {
                if (success) {
                    // KLJUČNA IZMENA: Sada dobijamo invitationId iz invitation objekta
                    // nakon što je Firestore kreirao dokument
                    String invitationId = invitation.getInvitationId();

                    if (invitationId != null && !invitationId.isEmpty()) {
                        // Kreiraj notifikaciju sa invitationId kao referenceId
                        notificationRepository.createNotification(
                                friend.getUserId(),
                                "ALLIANCE_INVITE",
                                currentUsername + " te je pozvao/la u savez " + allianceName,
                                invitationId,  // Sada koristimo invitationId
                                notifSuccess -> {
                                    if (!notifSuccess) {
                                        android.util.Log.e("CreateAlliance", "Failed to send notification to " + friend.getUsername());
                                    }
                                }
                        );
                    } else {
                        android.util.Log.e("CreateAlliance", "Invitation ID is null for " + friend.getUsername());
                    }
                }

                sentCount[0]++;
                if (sentCount[0] == totalInvitations) {
                    runOnUiThread(() -> {
                        Toast.makeText(CreateAllianceActivity.this, "Savez kreiran i pozivnice poslate!", Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            });
        }
    }
}