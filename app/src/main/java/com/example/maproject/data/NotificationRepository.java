package com.example.maproject.data;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.maproject.model.Notification;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private final FirebaseFirestore db;

    public NotificationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // --- LOAD ALL NOTIFICATIONS ---
    public void loadNotifications(String userId, MutableLiveData<List<Notification>> liveData) {
        db.collection("notifications")
                .whereEqualTo("receiverId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading notifications", error);
                        liveData.postValue(new ArrayList<>());
                        return;
                    }

                    List<Notification> notifications = new ArrayList<>();

                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            try {
                                Notification notification = new Notification();
                                notification.setNotificationId(doc.getId());
                                notification.setReceiverId(doc.getString("receiverId"));
                                notification.setType(doc.getString("type"));
                                notification.setContent(doc.getString("content"));
                                notification.setReferenceId(doc.getString("referenceId"));
                                notification.setRead(Boolean.TRUE.equals(doc.getBoolean("isRead")));

                                // Sigurno parsiranje timestamp-a
                                Object tsObj = doc.get("timestamp");
                                if (tsObj instanceof Timestamp) {
                                    notification.setTimestamp((Timestamp) tsObj);
                                } else if (tsObj instanceof Long) {
                                    notification.setTimestamp(new Timestamp(new Date((Long) tsObj)));
                                } else {
                                    notification.setTimestamp(Timestamp.now());
                                }

                                notifications.add(notification);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing notification document: " + doc.getId(), e);
                            }
                        }
                    }

                    liveData.postValue(notifications);
                    Log.d(TAG, "Loaded " + notifications.size() + " notifications for user " + userId);
                });
    }

    // --- MARK AS READ ---
    public void markAsRead(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) {
            Log.e(TAG, "Cannot mark notification as read: invalid ID");
            return;
        }

        db.collection("notifications").document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification marked as read: " + notificationId))
                .addOnFailureListener(e -> Log.e(TAG, "Error marking notification as read: " + notificationId, e));
    }

    // --- CREATE NEW NOTIFICATION ---
    public void createNotification(String receiverId, String type, String content, String referenceId, OnCompleteListener listener) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String currentUserId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;

        // Izbegni slanje notifikacije samom sebi
        if (currentUserId != null && currentUserId.equals(receiverId)) {
            Log.d(TAG, "Skipping notification: sender and receiver are the same user");
            if (listener != null) listener.onComplete(true);
            return;
        }

        // Provera da ne postoji ista aktivna (nepročitana) notifikacija
        db.collection("notifications")
                .whereEqualTo("receiverId", receiverId)
                .whereEqualTo("type", type)
                .whereEqualTo("referenceId", referenceId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Log.d(TAG, "Notification already exists (type=" + type + "), skipping creation");
                        if (listener != null) listener.onComplete(true);
                        return;
                    }

                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("receiverId", receiverId);
                    notificationData.put("type", type);
                    notificationData.put("content", content);
                    notificationData.put("referenceId", referenceId);
                    notificationData.put("timestamp", Timestamp.now());
                    notificationData.put("isRead", false);

                    db.collection("notifications")
                            .add(notificationData)
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

    // --- DELETE NOTIFICATION ---
    public void deleteNotification(String notificationId, OnCompleteListener listener) {
        if (notificationId == null || notificationId.isEmpty()) {
            Log.e(TAG, "Cannot delete notification: invalid ID");
            if (listener != null) listener.onComplete(false);
            return;
        }

        db.collection("notifications").document(notificationId)
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

    // --- SEND (WRAPPER ZA MODEL OBJEKAT) ---
    public void sendNotification(Notification notification) {
        if (notification == null) {
            Log.e(TAG, "sendNotification: Notification object is null");
            return;
        }

        createNotification(
                notification.getReceiverId(),
                notification.getType(),
                notification.getContent(),
                notification.getReferenceId(),
                success -> {
                    if (success) {
                        Log.d(TAG, "Notification sent successfully to " + notification.getReceiverId());
                    } else {
                        Log.e(TAG, "Failed to send notification to " + notification.getReceiverId());
                    }
                }
        );
    }

    // --- CALLBACK INTERFEJS ---
    public interface OnCompleteListener {
        void onComplete(boolean success);
    }
}
