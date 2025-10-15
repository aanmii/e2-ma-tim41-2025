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
    private LinearLayout noNotificationsContainer; // Kontejner za prazno stanje
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
        // Pozivamo observer samo jednom
        setupNotificationObserver();
        // Vizuelni feedback pre nego što podaci stignu
        showLoadingState();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.notificationsRecyclerView);
        progressBar = findViewById(R.id.notificationsProgressBar);
        // Inicijalizacija kontejnera i teksta
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
        // Kreiramo LiveData samo ovde
        MutableLiveData<List<Notification>> notificationsLiveData = new MutableLiveData<>();

        // Postavljamo LiveData Observer
        notificationsLiveData.observe(this, notifications -> {
            progressBar.setVisibility(View.GONE);

            if (notifications != null && !notifications.isEmpty()) {
                // Prikaz liste
                adapter.updateNotifications(notifications);
                noNotificationsContainer.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                // Prikaz praznog stanja
                recyclerView.setVisibility(View.GONE);
                noNotificationsTextView.setText("Nema novih notifikacija");
                noNotificationsContainer.setVisibility(View.VISIBLE);
            }
        });

        // Prosleđujemo instancu LiveData repozitorijumu
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
                // Možda je potrebno prikazati dijalog za pozivnicu ovde, ali za sada ostajemo na ekranu.
                Toast.makeText(this, "Kliknuli ste na pozivnicu. Očekuje se implementacija dijaloga.", Toast.LENGTH_SHORT).show();
                break;
            case "ALLIANCE_ACCEPTED":
            case "CHAT_MESSAGE":
                // 💥 KRITIČNA POPRAVKA: Koristi se nova, ispravna metoda getReferenceId()
                String allianceId = notification.getReferenceId();

                // KRITIČNA PROVERA: Da li ID zaista postoji
                if (allianceId != null && !allianceId.isEmpty()) {
                    Intent intent = new Intent(this, AllianceActivity.class);
                    intent.putExtra("ALLIANCE_ID", allianceId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Greška: Nevalidan ID saveza.", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                Toast.makeText(this, "Nepoznat tip notifikacije.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
