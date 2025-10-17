package com.example.maproject.ui.alliance;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.data.ChatRepository;
import com.example.maproject.data.NotificationRepository;
import com.example.maproject.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AllianceChatActivity extends AppCompatActivity {

    private static final String TAG = "AllianceChatActivity";

    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendMessageButton;
    private Toolbar chatToolbar;

    private ChatAdapter chatAdapter;
    private ChatRepository chatRepository;
    private NotificationRepository notificationRepository;
    private String allianceId;
    private String allianceName;
    private String currentUserId;
    private String currentUsername;
    private List<String> allianceMemberIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance_chat);

        allianceId = getIntent().getStringExtra("ALLIANCE_ID");
        allianceName = getIntent().getStringExtra("ALLIANCE_NAME");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatRepository = new ChatRepository();
        notificationRepository = new NotificationRepository();
        allianceMemberIds = new ArrayList<>();

        initViews();
        setupToolbar();
        loadCurrentUsername();
        loadAllianceMembers();
        setupChat();
        setupSendButton();
    }

    private void initViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendMessageButton = findViewById(R.id.sendMessageButton);
        chatToolbar = findViewById(R.id.chatToolbar);
    }

    private void setupToolbar() {
        if (allianceName != null) {
            chatToolbar.setTitle(allianceName);
        } else {
            chatToolbar.setTitle("Group chat");
        }
        setSupportActionBar(chatToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void loadCurrentUsername() {
        FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    currentUsername = doc.getString("username");
                })
                .addOnFailureListener(e -> {
                    currentUsername = "Anon";
                    Toast.makeText(this, "Error: cant load username", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAllianceMembers() {
        if (allianceId == null) return;

        FirebaseFirestore.getInstance().collection("alliances").document(allianceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> memberIds = (List<String>) doc.get("memberIds");
                        if (memberIds != null) {
                            allianceMemberIds = memberIds;
                            Log.d(TAG, "Alliance members loaded: " + memberIds.size());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading alliance members", e));
    }

    private void setupChat() {
        if (allianceId == null || currentUserId == null) {
            Toast.makeText(this, "Error: info for chat is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatAdapter = new ChatAdapter(new ArrayList<>(), currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>();

        messagesLiveData.observe(this, messages -> {
            if (messages != null) {
                chatAdapter.updateMessages(messages);
                if (!messages.isEmpty()) {
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                }
            }
        });

        chatRepository.loadMessages(allianceId, messagesLiveData);
    }

    private void setupSendButton() {
        sendMessageButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String messageContent = messageEditText.getText().toString().trim();

        if (messageContent.isEmpty()) {
            Toast.makeText(this, "Message cant be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUsername == null) {
            Toast.makeText(this, "Please wait, your name is loading", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatMessage message = new ChatMessage(currentUserId, currentUsername, messageContent);

        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();

        resultLiveData.observe(this, success -> {
            if (success) {
                messageEditText.setText("");
                sendNotificationsToMembers(messageContent);
            } else {
                Toast.makeText(this, "Error while sending message.", Toast.LENGTH_SHORT).show();
            }
        });

        chatRepository.sendMessage(allianceId, message, resultLiveData);
    }

    private void sendNotificationsToMembers(String messageContent) {
        if (allianceMemberIds.isEmpty()) {
            Log.w(TAG, "No members to send notifications to");
            return;
        }

        String notificationContent = currentUsername + " sent message to " + allianceName;

        if (messageContent.length() > 50) {
            notificationContent = currentUsername + ": " + messageContent.substring(0, 47) + "...";
        } else {
            notificationContent = currentUsername + ": " + messageContent;
        }

        for (String memberId : allianceMemberIds) {
            if (!memberId.equals(currentUserId)) {
                notificationRepository.createNotification(
                        memberId,
                        "CHAT_MESSAGE",
                        notificationContent,
                        allianceId,
                        success -> {
                            if (success) {
                                Log.d(TAG, "Notification sent to member: " + memberId);
                            } else {
                                Log.e(TAG, "Failed to send notification to member: " + memberId);
                            }
                        }
                );
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}