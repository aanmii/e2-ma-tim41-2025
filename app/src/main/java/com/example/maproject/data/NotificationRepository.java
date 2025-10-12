package com.example.maproject.data;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.maproject.model.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {
    private final FirebaseFirestore db;

    public NotificationRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void loadNotifications(String userId, MutableLiveData<List<Notification>> notificationsLiveData) {
        db.collection("notifications")
                .whereEqualTo("receiverId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e("NotificationRepository", "Error loading notifications for user: " + userId, error);
                        notificationsLiveData.postValue(new ArrayList<>());
                        return;
                    }
                    if (querySnapshot != null) {
                        List<Notification> notifications = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            try {
                                Notification notification = doc.toObject(Notification.class);
                                if (notification != null) notifications.add(notification);
                            } catch (Exception e) {
                                Log.e("NotificationRepository", "Failed to convert document to Notification: " + doc.getId(), e);
                            }
                        }
                        notificationsLiveData.postValue(notifications);
                    }
                });
    }

    public void sendNotification(Notification notification) {
        String notificationId = db.collection("notifications").document().getId();
        notification.setNotificationId(notificationId);

        db.collection("notifications").document(notificationId)
                .set(notification.toMap())
                .addOnSuccessListener(aVoid -> Log.d("NotificationRepository", "Notification sent: " + notification.getType()))
                .addOnFailureListener(e -> Log.e("NotificationRepository", "Failed to send notification", e));
    }

    public void markAsRead(String notificationId) {
        db.collection("notifications").document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> Log.d("NotificationRepository", "Notification marked as read: " + notificationId))
                .addOnFailureListener(e -> Log.e("NotificationRepository", "Failed to mark notification as read", e));
    }
}
