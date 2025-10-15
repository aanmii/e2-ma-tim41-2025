package com.example.maproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.maproject.data.AllianceRepository;
import com.example.maproject.data.NotificationRepository;
import com.example.maproject.data.UserRepository;
import com.example.maproject.service.NotificationListenerService;
import com.example.maproject.ui.alliance.AllianceActivity;
import com.example.maproject.ui.auth.LoginActivity;
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
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeTextView, notificationBadgeTextView;
    private View profileButton, friendsButton, notificationsButton, allianceButton, shopButton;
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
    }

    private void initViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        notificationBadgeTextView = findViewById(R.id.notificationBadgeTextView);
        profileButton = findViewById(R.id.profileButton);
        friendsButton = findViewById(R.id.friendsButton);
        notificationsButton = findViewById(R.id.notificationsButton);
        allianceButton = findViewById(R.id.allianceButton);
        shopButton = findViewById(R.id.shopButton);
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
            welcomeTextView.setText("Dobrodošli! 👋");

            loadUserData();
            initializeFCMToken();
            // Ovde možeš dodati notifikacije i saveze
        } else {
            navigateToLogin();
        }
    }

    private void setupButtons() {
        profileButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        friendsButton.setOnClickListener(v -> startActivity(new Intent(this, FriendsActivity.class)));
        notificationsButton.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        allianceButton.setOnClickListener(v -> openAlliance());
        shopButton.setOnClickListener(v -> startActivity(new Intent(this, ShopActivity.class)));
        logoutButton.setOnClickListener(v -> {
            authViewModel.logout();
            sharedPreferences.edit().putBoolean("isLoggedIn", false).apply();
            Toast.makeText(this, "Uspešno ste se odjavili", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, "Nisi član nijednog saveza", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserData() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUsername = doc.getString("username");
                        welcomeTextView.setText("Dobrodošli, " + currentUsername + "! 👋");
                    }
                });
    }

    private void initializeFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) return;
                    String token = task.getResult();
                    db.collection("users").document(currentUserId)
                            .update("fcmToken", token);
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
