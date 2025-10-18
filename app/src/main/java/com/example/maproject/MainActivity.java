package com.example.maproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.maproject.data.AllianceRepository;
import com.example.maproject.data.NotificationRepository;
import com.example.maproject.data.UserRepository;
import com.example.maproject.model.AllianceInvitation;
import com.example.maproject.ui.alliance.AllianceActivity;
import com.example.maproject.ui.auth.LoginActivity;
import com.example.maproject.ui.boss.BossFightActivity; // DODATO
import com.example.maproject.ui.friends.FriendsActivity;
import com.example.maproject.ui.model.ProfileActivity;
import com.example.maproject.ui.notifications.NotificationsActivity;
import com.example.maproject.ui.shop.ShopActivity;
import com.example.maproject.viewmodel.AuthViewModel;
import com.example.maproject.viewmodel.ViewModelFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeTextView, notificationBadgeTextView;
    private View profileButton, friendsButton, notificationsButton, allianceButton, shopButton;
    private View bossFightButton; // DODATO
    private View logoutButton;

    private AuthViewModel authViewModel;
    private SharedPreferences sharedPreferences;

    private FirebaseFirestore db;
    private NotificationRepository notificationRepository;
    private AllianceRepository allianceRepository;

    private String currentUserId;
    private String currentUsername;

    private ListenerRegistration invitationListener;
    private ListenerRegistration notificationListener;
    private ListenerRegistration newNotificationListener;

    private ActivityResultLauncher<String> notificationPermissionLauncher;

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

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("notification_type")) {
            String type = intent.getStringExtra("notification_type");
            String referenceId = intent.getStringExtra("reference_id");

            if ("ALLIANCE_INVITE".equals(type)) {
                openAllianceFromNotification(referenceId);
            } else if ("CHAT_MESSAGE".equals(type)) {
                if (referenceId != null && !referenceId.isEmpty()) {
                    Intent chatIntent = new Intent(this, AllianceActivity.class);
                    chatIntent.putExtra("ALLIANCE_ID", referenceId);
                    startActivity(chatIntent);
                }
            }
        }
    }

    private void openAllianceFromNotification(String invitationId) {
        if (invitationId == null) return;

        FirebaseFirestore.getInstance().collection("invitations")
                .document(invitationId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String allianceId = doc.getString("allianceId");
                        if (allianceId != null && !allianceId.isEmpty()) {
                            Intent intent = new Intent(this, AllianceActivity.class);
                            intent.putExtra("ALLIANCE_ID", allianceId);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Alliance not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void initViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        notificationBadgeTextView = findViewById(R.id.notificationBadgeTextView);
        profileButton = findViewById(R.id.profileButton);
        friendsButton = findViewById(R.id.friendsButton);
        notificationsButton = findViewById(R.id.notificationsButton);
        allianceButton = findViewById(R.id.allianceButton);
        shopButton = findViewById(R.id.shopButton);
        bossFightButton = findViewById(R.id.bossFightButton); // DODATO
        logoutButton = findViewById(R.id.logoutButton);
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
            welcomeTextView.setText("Welcome! 👋");

            loadUserData();
            initializeFCMToken();
            startAllianceInvitationListener();
        } else {
            navigateToLogin();
        }
    }

    private void startAllianceInvitationListener() {
        if (currentUserId == null) return;

        invitationListener = db.collection("invitations")
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("MainActivity", "Listen failed for invitations.", e);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            AllianceInvitation invitation = doc.toObject(AllianceInvitation.class);
                            invitation.setInvitationId(doc.getId());

                            if (!isFinishing() && !isDestroyed()) {
                                showImmediateInvitationDialog(invitation);
                            }
                            return;
                        }
                    }
                });
    }

    private void showImmediateInvitationDialog(AllianceInvitation invitation) {
        new AlertDialog.Builder(this)
                .setTitle("⚔️ Alliance invite")
                .setMessage(invitation.getSenderUsername() + " invites you into the alliance \"" + invitation.getAllianceName() + "\".")
                .setCancelable(false)
                .setPositiveButton("Accept", (dialog, which) -> {
                    allianceRepository.respondToInvitation(invitation, currentUserId, currentUsername, success -> {
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Invite accepted into " + invitation.getAllianceName(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error while accepting", Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Reject", (dialog, which) -> {
                    db.collection("invitations").document(invitation.getInvitationId())
                            .update("status", "REJECTED")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Invite rejected", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error while rejecting", Toast.LENGTH_SHORT).show();
                            });
                })
                .show();
    }

    private void setupButtons() {
        profileButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        friendsButton.setOnClickListener(v -> startActivity(new Intent(this, FriendsActivity.class)));
        notificationsButton.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        allianceButton.setOnClickListener(v -> openAlliance());
        shopButton.setOnClickListener(v -> startActivity(new Intent(this, ShopActivity.class)));

        // DODATO - Boss Fight dugme
        bossFightButton.setOnClickListener(v -> startActivity(new Intent(this, BossFightActivity.class)));

        logoutButton.setOnClickListener(v -> {
            authViewModel.logout();
            sharedPreferences.edit().putBoolean("isLoggedIn", false).apply();
            Toast.makeText(this, "Logout successful", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        });
    }

    private void openAlliance() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    String allianceId = doc.getString("currentAllianceId");
                    if (allianceId != null && !allianceId.isEmpty()) {
                        Intent intent = new Intent(this, AllianceActivity.class);
                        intent.putExtra("ALLIANCE_ID", allianceId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "You are not in any alliance", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserData() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUsername = doc.getString("username");
                        welcomeTextView.setText("Welcome, " + currentUsername + "! 👋");
                    }
                });
    }

    private void initializeFCMToken() {
        if (currentUserId == null) {
            Log.e("FCM_INIT", "UserID is NULL.");
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("FCM_INIT", "FCM init failed", task.getException());
                        return;
                    }

                    String token = task.getResult();

                    Map<String, Object> tokenUpdate = new HashMap<>();
                    tokenUpdate.put("fcmToken", token);

                    db.collection("users").document(currentUserId)
                            .set(tokenUpdate, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FCM_INIT", "Token successfully fetched to Firestore. Token: " + token);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FCM_INIT", "Error while fetching FCM.", e);
                            });
                });
    }

    private void navigateToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (invitationListener != null) invitationListener.remove();
        if (notificationListener != null) notificationListener.remove();
        if (newNotificationListener != null) newNotificationListener.remove();
    }
}