package com.example.maproject.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.maproject.MainActivity;
import com.example.maproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class NotificationListenerService extends Service {

    private static final String TAG = "NotificationListener";
    private static final String CHANNEL_ID = "alliance_notifications";
    private static final String FOREGROUND_CHANNEL_ID = "notification_service";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;
    private String currentUserId;

    @Override
    public void onCreate() {
        super.onCreate();

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            try {
                createNotificationChannels();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());
                }

                startListeningForNotifications();

                Log.d(TAG, "NotificationListenerService started successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error in onCreate", e);
                stopSelf();
            }
        } else {
            Log.w(TAG, "No authenticated user, stopping service");
            stopSelf();
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alliance Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifikacije za savez, prijatelje i poruke");
            notificationManager.createNotificationChannel(channel);

            NotificationChannel foregroundChannel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Notification Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            foregroundChannel.setDescription("Servis za primanje notifikacija u pozadini");
            notificationManager.createNotificationChannel(foregroundChannel);
        }
    }

    private android.app.Notification createForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        int iconResId = getResources().getIdentifier("ic_notification", "drawable", getPackageName());
        if (iconResId == 0) {
            iconResId = android.R.drawable.ic_dialog_info;
        }

        return new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setContentTitle("Notification Service")
                .setContentText("Primam notifikacije...")
                .setSmallIcon(iconResId)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void startListeningForNotifications() {
        notificationListener = db.collection("notifications")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for notifications", error);
                        return;
                    }

                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String notificationId = dc.getDocument().getId();
                                String type = dc.getDocument().getString("type");
                                String content = dc.getDocument().getString("content");
                                String referenceId = dc.getDocument().getString("referenceId");

                                Log.d(TAG, "New notification: " + content);
                                showSystemNotification(notificationId, type, content, referenceId);
                            }
                        }
                    }
                });
    }

    private void showSystemNotification(String notificationId, String type, String content, String referenceId) {
        String title = getTitleForType(type);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("notification_type", type);
        intent.putExtra("reference_id", referenceId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notificationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int iconResId = getResources().getIdentifier("ic_notification", "drawable", getPackageName());
        if (iconResId == 0) {
            iconResId = android.R.drawable.ic_dialog_info;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(notificationId.hashCode(), builder.build());
        }
    }

    private String getTitleForType(String type) {
        if (type == null) return "🔔 Notification";

        switch (type) {
            case "ALLIANCE_INVITE":
                return "⚔️ Invitation to a new alliance";
            case "ALLIANCE_ACCEPTED":
                return "✅ Invitation accepted";
            case "CHAT_MESSAGE":
                return "💬 New message";
            default:
                return "🔔 Notification";
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}