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
import com.example.maproject.model.Notification;
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
                .addOnSuccessListener(doc -> {
                    currentUsername = doc.getString("username");
                });
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
        friendsLiveData.observe(this, friends -> {
            selectFriendsAdapter.updateFriends(friends);
        });
        friendsRepository.loadFriends(currentUserId, friendsLiveData);
    }

    private void onFriendSelected(User friend, boolean isSelected) {
        if (isSelected) {
            selectedFriends.add(friend);
        } else {
            selectedFriends.remove(friend);
        }
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

        MutableLiveData<String> createLiveData = new MutableLiveData<>();
        createLiveData.observe(this, allianceId -> {
            if (allianceId != null) {
                sendInvitations(allianceId, name);
            } else {
                Toast.makeText(this, "Greška pri kreiranju saveza", Toast.LENGTH_SHORT).show();
            }
        });

        allianceRepository.createAlliance(alliance, createLiveData);
    }

    private void sendInvitations(String allianceId, String allianceName) {
        int totalInvitations = selectedFriends.size();
        int[] sentCount = {0};

        for (User friend : selectedFriends) {
            AllianceInvitation invitation = new AllianceInvitation(
                    allianceId, allianceName, currentUserId, currentUsername, friend.getUserId()
            );

            MutableLiveData<Boolean> inviteLiveData = new MutableLiveData<>();
            inviteLiveData.observe(this, success -> {
                sentCount[0]++;
                if (success) {
                    // SLANJE SISTEMSKE NOTIFIKACIJE
                    Notification notification = new Notification(
                            friend.getUserId(),
                            "ALLIANCE_INVITE",
                            currentUsername + " te je pozvao/la u savez " + allianceName + ".",
                            invitation.getInvitationId()
                    );
                    notificationRepository.sendNotification(notification);
                }

                if (sentCount[0] == totalInvitations) {
                    Toast.makeText(this, "Savez kreiran i pozivnice poslate!", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

            allianceRepository.sendInvitation(invitation, inviteLiveData);
        }
    }
}