package com.example.maproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.maproject.data.AllianceRepository;
import com.example.maproject.data.NotificationRepository;
import com.example.maproject.data.UserRepository;
import com.example.maproject.model.AllianceInvitation;
import com.example.maproject.service.NotificationListenerService;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

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

    private ListenerRegistration invitationListener;
    private ListenerRegistration notificationListener;

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
        setIntent(intent);
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

            // Pokreni servis za sistemske notifikacije SAMO na Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startNotificationListenerService();
            }
        } else {
            navigateToLogin();
        }
    }

    private void startNotificationListenerService() {
        try {
            Intent serviceIntent = new Intent(this, NotificationListenerService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting notification service", e);
            // Ne prekidaj app ako servis ne uspe da se pokrene
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
        if (notificationListener != null) {
            notificationListener.remove();
        }

        notificationListener = db.collection("notifications")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for notifications", error);
                        return;
                    }

                    if (querySnapshot != null) {
                        int unreadCount = querySnapshot.size();

                        Log.d(TAG, "Unread notifications count: " + unreadCount);

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Log.d(TAG, "Notification: " + doc.getId() + " - " +
                                    doc.getString("content") + " (read: " + doc.getBoolean("isRead") + ")");
                        }

                        if (unreadCount > 0) {
                            notificationBadgeTextView.setText(String.valueOf(unreadCount));
                            notificationBadgeTextView.setVisibility(View.VISIBLE);
                        } else {
                            notificationBadgeTextView.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void listenForAllianceInvitations() {
        if (invitationListener != null) {
            invitationListener.remove();
        }

        invitationListener = db.collection("invitations")
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
                                invitation.setInvitationId(dc.getDocument().getId());
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
        if (invitation.getInvitationId() == null || invitation.getInvitationId().isEmpty()) {
            Toast.makeText(this, "Greška: Neispravan poziv", Toast.LENGTH_SHORT).show();
            return;
        }

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
        if (invitation.getInvitationId() == null || invitation.getInvitationId().isEmpty()) {
            Toast.makeText(this, "Greška: Neispravan poziv", Toast.LENGTH_SHORT).show();
            return;
        }

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
            // Zaustavi servis za notifikacije
            stopService(new Intent(this, NotificationListenerService.class));

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
            if (currentUserId == null) {
                Toast.makeText(this, "Greška: Korisnički ID nije dostupan.", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").document(currentUserId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String allianceId = userDoc.getString("currentAllianceId");

                            if (allianceId != null && !allianceId.isEmpty()) {
                                Log.d(TAG, "Korisnik je clan saveza: " + allianceId);
                                Intent intent = new Intent(this, AllianceActivity.class);
                                intent.putExtra("ALLIANCE_ID", allianceId);
                                startActivity(intent);
                            } else {
                                Log.d(TAG, "Korisnik nije clan nijednog saveza.");
                                Toast.makeText(this, "Nisi član nijednog saveza", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Greška: Podaci o korisniku nisu pronađeni.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Greška pri proveri statusa saveza", e);
                        Toast.makeText(this, "Greška pri učitavanju saveza", Toast.LENGTH_SHORT).show();
                    });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (invitationListener != null) {
            invitationListener.remove();
        }
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}