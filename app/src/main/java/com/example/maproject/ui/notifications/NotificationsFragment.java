package com.example.maproject.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.data.AllianceRepository;
import com.example.maproject.data.NotificationRepository;
import com.example.maproject.model.Notification;
import com.example.maproject.ui.alliance.AllianceActivity; // Pretpostavljamo da imate AllianceActivity
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment implements NotificationsAdapter.OnNotificationClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView noNotificationsTextView;

    private NotificationsAdapter adapter;
    private NotificationRepository notificationRepository;
    private AllianceRepository allianceRepository; // Treba nam za obradu pozivnica

    private String currentUserId;

    public NotificationsFragment() {
        // Obavezni prazan konstruktor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        notificationRepository = new NotificationRepository();
        allianceRepository = new AllianceRepository();

        initViews(view);
        setupRecyclerView();
        loadNotifications();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.notificationsRecyclerView);
        progressBar = view.findViewById(R.id.notificationsProgressBar);
        noNotificationsTextView = view.findViewById(R.id.noNotificationsTextView);
    }

    private void setupRecyclerView() {
        adapter = new NotificationsAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);

        MutableLiveData<List<Notification>> notificationsLiveData = new MutableLiveData<>();
        notificationsLiveData.observe(getViewLifecycleOwner(), notifications -> {
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
        // Prvo označi notifikaciju kao pročitanu
        if (!notification.isRead()) {
            notificationRepository.markAsRead(notification.getNotificationId());
        }

        switch (notification.getType()) {
            case "ALLIANCE_INVITE":
                // Preusmeri korisnika na ekran za Pozivnice (gde može da prihvati/odbije)
                handleAllianceInvite(notification);
                break;

            case "ALLIANCE_ACCEPTED":
            case "CHAT_MESSAGE":
                // Preusmeri korisnika u Alliance/Chat aktivnost
                handleAllianceMessage(notification);
                break;

            // Ovde dodaj ostale tipove notifikacija (npr. MISSION_COMPLETED)
        }
    }

    private void handleAllianceInvite(Notification notification) {
        // Pozivnica se odnosi na AllianceInvitation ID, preusmeriti na Invites/Alliance screen.
        // Pretpostavljamo da imate aktivnost koja prikazuje sve pozivnice (InvitationsActivity)
        Toast.makeText(getContext(), "Pregled pozivnica: " + notification.getContent(), Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(getActivity(), InvitationsActivity.class);
        // startActivity(intent);
    }

    private void handleAllianceMessage(Notification notification) {
        // Preusmeri direktno u Alliance/Chat aktivnost
        Intent intent = new Intent(getActivity(), AllianceActivity.class);
        // Prosleđujemo Alliance ID da bi AllianceActivity znao koji chat da otvori.
        intent.putExtra("ALLIANCE_ID", notification.getReferenceId());
        startActivity(intent);
    }
}