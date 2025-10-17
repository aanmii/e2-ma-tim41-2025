package com.example.maproject.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.data.NotificationRepository;
import com.example.maproject.model.Notification;
import com.example.maproject.ui.alliance.AllianceActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity implements NotificationsAdapter.OnNotificationClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView noNotificationsTextView;
    private LinearLayout noNotificationsContainer;
    private Button backButton;

    private NotificationsAdapter adapter;
    private NotificationRepository notificationRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        notificationRepository = new NotificationRepository();

        initViews();
        setupRecyclerView();
        setupButtons();
        setupNotificationObserver();
        showLoadingState();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.notificationsRecyclerView);
        progressBar = findViewById(R.id.notificationsProgressBar);
        noNotificationsContainer = findViewById(R.id.noNotificationsContainer);
        noNotificationsTextView = findViewById(R.id.noNotificationsTextView);
        backButton = findViewById(R.id.backButton);
    }

    private void setupRecyclerView() {
        adapter = new NotificationsAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        backButton.setOnClickListener(v -> finish());
    }

    private void showLoadingState() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noNotificationsContainer.setVisibility(View.GONE);
    }

    private void setupNotificationObserver() {
        MutableLiveData<List<Notification>> notificationsLiveData = new MutableLiveData<>();

        notificationsLiveData.observe(this, notifications -> {
            progressBar.setVisibility(View.GONE);

            if (notifications != null && !notifications.isEmpty()) {
                adapter.updateNotifications(notifications);
                noNotificationsContainer.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.GONE);
                noNotificationsTextView.setText("No new notifications");
                noNotificationsContainer.setVisibility(View.VISIBLE);
            }
        });

        notificationRepository.loadNotifications(currentUserId, notificationsLiveData);
    }

    @Override
    public void onNotificationClick(Notification notification) {
        if (!notification.isRead()) {
            notificationRepository.markAsRead(notification.getNotificationId());
        }

        switch (notification.getType()) {
            case "ALLIANCE_INVITE":
                Intent allianceIntent = new Intent(this, AllianceActivity.class);
                startActivity(allianceIntent);
                break;

            case "FRIEND_REQUEST":
                Intent friendsIntent = new Intent(this, com.example.maproject.ui.friends.FriendsActivity.class);
                friendsIntent.putExtra("open_requests_tab", true);
                startActivity(friendsIntent);
                break;

            case "ALLIANCE_ACCEPTED":
            case "CHAT_MESSAGE":
                String allianceId = notification.getReferenceId();
                if (allianceId != null && !allianceId.isEmpty()) {
                    Intent chatIntent = new Intent(this, AllianceActivity.class);
                    chatIntent.putExtra("ALLIANCE_ID", allianceId);
                    startActivity(chatIntent);
                } else {
                    Toast.makeText(this, "Error: invalid alliance id", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                Toast.makeText(this, "Unknown notification type", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
