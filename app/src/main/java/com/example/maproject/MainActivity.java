package com.example.maproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.maproject.data.AllianceRepository;
import com.example.maproject.data.NotificationRepository;
import com.example.maproject.data.UserRepository;
import com.example.maproject.model.AllianceInvitation;
import com.example.maproject.model.Notification;
import com.example.maproject.ui.alliance.AllianceActivity;
import com.example.maproject.ui.auth.LoginActivity;
import com.example.maproject.ui.friends.FriendsActivity;
import com.example.maproject.ui.notifications.NotificationsActivity;
import com.example.maproject.ui.model.ProfileActivity;
import com.example.maproject.viewmodel.AuthViewModel;
import com.example.maproject.viewmodel.ViewModelFactory;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView welcomeTextView, notificationBadgeTextView;
    private Button logoutButton;
    private View profileButton, friendsButton, notificationsButton, allianceButton;
    private AuthViewModel authViewModel;
    private SharedPreferences sharedPreferences;

    private FirebaseFirestore db;
    private NotificationRepository notificationRepository;
    private AllianceRepository allianceRepository;

    private String currentUserId;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
        allianceRepository = new AllianceRepository();

        initViews();
        setupViewModel();
        checkUserAuthentication();
        setupButtons();

        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }

    private void initViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        notificationBadgeTextView = findViewById(R.id.notificationBadgeTextView);
        logoutButton = findViewById(R.id.logoutButton);
        profileButton = findViewById(R.id.profileButton);
        friendsButton = findViewById(R.id.friendsButton);
        notificationsButton = findViewById(R.id.notificationsButton);
        allianceButton = findViewById(R.id.allianceButton);
    }

    private void setupViewModel() {
        UserRepository userRepository = new UserRepository();
        ViewModelFactory factory = new ViewModelFactory(userRepository);
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);
    }

    private void checkUserAuthentication() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            welcomeTextView.setText("Dobrodošli! 👋");

            loadUserData();

            initializeFCMToken();

            listenForAllianceInvitations();

            setupNotificationsBadge();
        } else {
            navigateToLogin();
        }
    }

    private void loadUserData() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUsername = doc.getString("username");
                        welcomeTextView.setText("Dobrodošli, " + currentUsername + "! 👋");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user data", e));
    }

    private void initializeFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM token failed", task.getException());
                            return;
                        }

                        String token = task.getResult();
                        Log.d(TAG, "FCM Token: " + token);

                        db.collection("users").document(currentUserId)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved"))
                                .addOnFailureListener(e -> Log.e(TAG, "Error saving FCM token", e));
                    }
                });
    }

    private void setupNotificationsBadge() {
        MutableLiveData<List<Notification>> notificationsLiveData = new MutableLiveData<>();

        notificationsLiveData.observe(this, notifications -> {
            int unreadCount = 0;
            if (notifications != null) {
                for (Notification n : notifications) {
                    if (!n.isRead()) unreadCount++;
                }
            }

            if (unreadCount > 0) {
                notificationBadgeTextView.setText(String.valueOf(unreadCount));
                notificationBadgeTextView.setVisibility(android.view.View.VISIBLE);
            } else {
                notificationBadgeTextView.setVisibility(android.view.View.GONE);
            }
        });

        notificationRepository.loadNotifications(currentUserId, notificationsLiveData);
    }

    private void listenForAllianceInvitations() {
        db.collection("invitations")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for invitations", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                AllianceInvitation invitation = dc.getDocument().toObject(AllianceInvitation.class);
                                showInvitationDialog(invitation);
                            }
                        }
                    }
                });
    }

    private void showInvitationDialog(AllianceInvitation invitation) {
        new AlertDialog.Builder(this)
                .setTitle("⚔️ Poziv u Savez")
                .setMessage(invitation.getSenderUsername() + " te poziva u savez \"" +
                        invitation.getAllianceName() + "\"")
                .setCancelable(false)
                .setPositiveButton("✓ Prihvati", (dialog, which) -> acceptInvitation(invitation))
                .setNegativeButton("✗ Odbij", (dialog, which) -> declineInvitation(invitation))
                .show();
    }

    private void acceptInvitation(AllianceInvitation invitation) {
        allianceRepository.respondToInvitation(invitation, currentUserId, currentUsername, success -> {
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Uspešno si pristupio savezu!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, AllianceActivity.class);
                    intent.putExtra("ALLIANCE_ID", invitation.getAllianceId());
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Greška pri pristupanju savezu. Možda je misija već aktivna?",
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void declineInvitation(AllianceInvitation invitation) {
        db.collection("invitations").document(invitation.getInvitationId())
                .update("status", "REJECTED")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Poziv odbijen", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error declining invitation", e);
                    Toast.makeText(this, "Greška pri odbijanju poziva", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null) {
            String notificationType = intent.getStringExtra("notification_type");
            String referenceId = intent.getStringExtra("reference_id");

            if (notificationType != null && referenceId != null) {
                switch (notificationType) {
                    case "ALLIANCE_INVITE":
                        startActivity(new Intent(this, NotificationsActivity.class));
                        break;
                    case "ALLIANCE_ACCEPTED":
                    case "CHAT_MESSAGE":
                        Intent allianceIntent = new Intent(this, AllianceActivity.class);
                        allianceIntent.putExtra("ALLIANCE_ID", referenceId);
                        startActivity(allianceIntent);
                        break;
                }
            }
        }
    }

    private void setupButtons() {
        logoutButton.setOnClickListener(v -> {
            authViewModel.logout();
            sharedPreferences.edit().putBoolean("isLoggedIn", false).apply();
            Toast.makeText(this, "Uspešno ste se odjavili", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });

        friendsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, FriendsActivity.class);
            startActivity(intent);
        });

        notificationsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });

        allianceButton.setOnClickListener(v -> {
            MutableLiveData<com.example.maproject.model.Alliance> allianceLiveData = new MutableLiveData<>();
            allianceLiveData.observe(this, alliance -> {
                if (alliance != null) {
                    Intent intent = new Intent(this, AllianceActivity.class);
                    intent.putExtra("ALLIANCE_ID", alliance.getAllianceId());
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Nisi član nijednog saveza", Toast.LENGTH_SHORT).show();
                }
            });
            allianceRepository.loadUserAlliance(currentUserId, allianceLiveData);
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != null) {
            setupNotificationsBadge();
        }
    }
}