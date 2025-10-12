package com.example.maproject.ui.alliance;

import android.os.Bundle;
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
import com.example.maproject.model.ChatMessage;
import com.example.maproject.ui.alliance.ChatAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AllianceChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendMessageButton;
    private Toolbar chatToolbar;

    private ChatAdapter chatAdapter;
    private ChatRepository chatRepository;
    private String allianceId;
    private String allianceName;
    private String currentUserId;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance_chat);

        allianceId = getIntent().getStringExtra("ALLIANCE_ID");
        allianceName = getIntent().getStringExtra("ALLIANCE_NAME");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatRepository = new ChatRepository();

        initViews();
        setupToolbar();
        loadCurrentUsername();
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
            chatToolbar.setTitle("Grupni Chat");
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
                    currentUsername = "AnonimniKorisnik";
                    Toast.makeText(this, "Greška: Ne mogu da učitam korisničko ime.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupChat() {
        if (allianceId == null || currentUserId == null) {
            Toast.makeText(this, "Greška: Nedostaju podaci za chat.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatAdapter = new ChatAdapter(new ArrayList<>(), currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        // KREIRAJEMO LiveData OBJEKAT
        MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>();

        // POSMATRAMO PROMENE U LiveData OBJEKTU
        messagesLiveData.observe(this, messages -> {
            if (messages != null) {
                chatAdapter.updateMessages(messages);
                if (!messages.isEmpty()) {
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                }
            }
        });

        // PROSLEĐUJEMO LiveData OBJEKAT REPOZITORIJUMU
        chatRepository.loadMessages(allianceId, messagesLiveData);
    }

    private void setupSendButton() {
        sendMessageButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String messageContent = messageEditText.getText().toString().trim();

        if (messageContent.isEmpty()) {
            Toast.makeText(this, "Poruka ne može biti prazna.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUsername == null) {
            Toast.makeText(this, "Molimo sačekajte, učitavamo vaše ime...", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatMessage message = new ChatMessage(currentUserId, currentUsername, messageContent);

        // KREIRAMO LiveData OBJEKAT ZA REZULTAT SLANJA
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();

        // POSMATRAMO REZULTAT
        resultLiveData.observe(this, success -> {
            if (success) {
                messageEditText.setText("");
            } else {
                Toast.makeText(this, "Greška pri slanju poruke.", Toast.LENGTH_SHORT).show();
            }
        });

        // PROSLEĐUJEMO LiveData OBJEKAT REPOZITORIJUMU
        chatRepository.sendMessage(allianceId, message, resultLiveData);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}