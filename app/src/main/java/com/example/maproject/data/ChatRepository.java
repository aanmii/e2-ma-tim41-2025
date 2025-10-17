package com.example.maproject.data;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.example.maproject.model.Alliance;
import com.example.maproject.model.ChatMessage;
import com.example.maproject.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatRepository {
    private static final String TAG = "ChatRepository";
    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;

    public ChatRepository() {
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
    }

    public void loadMessages(String allianceId, MutableLiveData<List<ChatMessage>> messagesLiveData) {
        db.collection("alliances").document(allianceId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading messages", error);
                        messagesLiveData.postValue(new ArrayList<>());
                        return;
                    }
                    if (querySnapshot != null) {
                        List<ChatMessage> messages = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            ChatMessage message = doc.toObject(ChatMessage.class);
                            if (message != null) {
                                messages.add(message);
                            }
                        }
                        messagesLiveData.postValue(messages);
                    }
                });
    }

    public void sendMessage(String allianceId, ChatMessage message, MutableLiveData<Boolean> resultLiveData) {
        db.collection("alliances").document(allianceId)
                .collection("messages")
                .add(message.toMap())
                .addOnSuccessListener(documentReference -> {
                    sendChatNotifications(allianceId, message);

                    resultLiveData.postValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    resultLiveData.postValue(false);
                });
    }

    private void sendChatNotifications(String allianceId, ChatMessage message) {
        db.collection("alliances").document(allianceId).get()
                .addOnSuccessListener(allianceDoc -> {
                    Alliance alliance = allianceDoc.toObject(Alliance.class);
                    if (alliance == null || alliance.getMembers() == null) {
                        Log.w(TAG, "Alliance or members not found for notifications.");
                        return;
                    }

                    Map<String, String> members = alliance.getMembers();
                    String senderId = message.getSenderId();
                    String senderUsername = message.getSenderUsername();
                    String content = message.getContent();
                    for (String memberId : members.keySet()) {
                        if (!memberId.equals(senderId)) {
                            String notificationContent = senderUsername + ": " + content;
                            if (notificationContent.length() > 80) {
                                notificationContent = notificationContent.substring(0, 77) + "...";
                            }

                            Notification notification = new Notification(
                                    memberId,
                                    "CHAT_MESSAGE",
                                    notificationContent,
                                    allianceId
                            );
                            notificationRepository.sendNotification(notification);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve alliance members for notifications.", e);
                });
    }
}