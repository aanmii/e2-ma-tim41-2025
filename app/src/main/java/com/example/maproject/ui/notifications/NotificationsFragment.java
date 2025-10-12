package com.example.maproject.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class NotificationsFragment extends Fragment implements NotificationsAdapter.OnNotificationClickListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView noNotificationsTextView;

    private NotificationsAdapter adapter;
    private NotificationRepository notificationRepository;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        notificationRepository = new NotificationRepository();

        recyclerView = view.findViewById(R.id.notificationsRecyclerView);
        progressBar = view.findViewById(R.id.notificationsProgressBar);
        noNotificationsTextView = view.findViewById(R.id.noNotificationsTextView);

        adapter = new NotificationsAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        loadNotifications();
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
        if (!notification.isRead()) notificationRepository.markAsRead(notification.getNotificationId());

        switch (notification.getType()) {
            case "ALLIANCE_INVITE":
                // Otvori pozivnice ekran
                break;
            case "ALLIANCE_ACCEPTED":
            case "CHAT_MESSAGE":
                Intent intent = new Intent(getActivity(), AllianceActivity.class);
                intent.putExtra("ALLIANCE_ID", notification.getReferenceId());
                startActivity(intent);
                break;
        }
    }
}
