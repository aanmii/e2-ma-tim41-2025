package com.example.maproject.data;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.maproject.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private final FirebaseFirestore db;

    public NotificationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void loadNotifications(String userId, MutableLiveData<List<Notification>> liveData) {
        db.collection("notifications")
                .whereEqualTo("receiverId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading notifications", error);
                        liveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (querySnapshot != null) {
                        List<Notification> notifications = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Notification notification = doc.toObject(Notification.class);
                            notification.setNotificationId(doc.getId());
                            notifications.add(notification);

                            Log.d(TAG, "Loaded notification: " + doc.getId() +
                                    " - Content: " + notification.getContent() +
                                    " - Read: " + notification.isRead());
                        }

                        Log.d(TAG, "Total notifications loaded: " + notifications.size());
                        liveData.setValue(notifications);
                    } else {
                        liveData.setValue(new ArrayList<>());
                    }
                });
    }

    public void markAsRead(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) {
            Log.e(TAG, "Cannot mark notification as read: Invalid ID");
            return;
        }

        db.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification marked as read: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking notification as read: " + notificationId, e);
                });
    }

    public void createNotification(String receiverId, String type, String content, String referenceId, OnCompleteListener listener) {
        // VAŽNO: Proveri da primatelj nije isti kao pošiljalac
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId != null && currentUserId.equals(receiverId)) {
            Log.d(TAG, "Skipping notification: sender and receiver are the same");
            if (listener != null) listener.onComplete(true);
            return;
        }

        // Proveri da li već postoji ista notifikacija (spreči duplikate)
        db.collection("notifications")
                .whereEqualTo("receiverId", receiverId)
                .whereEqualTo("type", type)
                .whereEqualTo("referenceId", referenceId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Već postoji nepročitana notifikacija istog tipa
                        Log.d(TAG, "Notification already exists, skipping creation");
                        if (listener != null) listener.onComplete(true);
                        return;
                    }

                    // Kreiraj novu notifikaciju
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("receiverId", receiverId);
                    notification.put("type", type);
                    notification.put("content", content);
                    notification.put("referenceId", referenceId);
                    notification.put("timestamp", com.google.firebase.Timestamp.now());
                    notification.put("isRead", false);

                    db.collection("notifications")
                            .add(notification)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Notification created: " + documentReference.getId());
                                if (listener != null) listener.onComplete(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error creating notification", e);
                                if (listener != null) listener.onComplete(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for existing notifications", e);
                    if (listener != null) listener.onComplete(false);
                });
    }

    public void deleteNotification(String notificationId, OnCompleteListener listener) {
        if (notificationId == null || notificationId.isEmpty()) {
            Log.e(TAG, "Cannot delete notification: Invalid ID");
            if (listener != null) listener.onComplete(false);
            return;
        }

        db.collection("notifications")
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification deleted: " + notificationId);
                    if (listener != null) listener.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting notification: " + notificationId, e);
                    if (listener != null) listener.onComplete(false);
                });
    }

    // Metoda za slanje notifikacije (wrapper oko createNotification)
    public void sendNotification(com.example.maproject.model.Notification notification) {
        createNotification(
                notification.getReceiverId(),
                notification.getType(),
                notification.getContent(),
                notification.getReferenceId(),
                success -> {
                    if (success) {
                        Log.d(TAG, "Notification sent successfully");
                    } else {
                        Log.e(TAG, "Failed to send notification");
                    }
                }
        );
    }

    public interface OnCompleteListener {
        void onComplete(boolean success);
    }
}