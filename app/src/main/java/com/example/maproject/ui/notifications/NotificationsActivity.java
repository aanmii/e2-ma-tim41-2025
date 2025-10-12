package com.example.maproject.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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
        loadNotifications();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.notificationsRecyclerView);
        progressBar = findViewById(R.id.notificationsProgressBar);
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

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);

        MutableLiveData<List<Notification>> notificationsLiveData = new MutableLiveData<>();
        notificationsLiveData.observe(this, notifications -> {
            progressBar.setVisibility(View.GONE);
            if (notifications != null && !notifications.isEmpty()) {
                adapter.updateNotifications(notifications);
                noNotificationsTextView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.GONE);
                noNotificationsTextView.setVisibility(View.VISIBLE);
            }
        });

        notificationRepository.loadNotifications(currentUserId, notificationsLiveData);
    }

    @Override
    public void onNotificationClick(Notification notification) {
        // Označi kao pročitano
        if (!notification.isRead()) {
            notificationRepository.markAsRead(notification.getNotificationId());
        }

        // Otvori odgovarajući ekran
        switch (notification.getType()) {
            case "ALLIANCE_INVITE":
                // Ostani na notifikacijama - ovde korisnik može prihvatiti/odbiti
                break;
            case "ALLIANCE_ACCEPTED":
            case "CHAT_MESSAGE":
                Intent intent = new Intent(this, AllianceActivity.class);
                intent.putExtra("ALLIANCE_ID", notification.getReferenceId());
                startActivity(intent);
                break;
        }
    }
}