package com.example.maproject.ui.friends;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.maproject.R;
import com.example.maproject.data.FriendsRepository;
import com.example.maproject.model.Friend;
import com.example.maproject.model.User;
import com.example.maproject.ui.alliance.CreateAllianceActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity implements FriendRequestAdapter.OnRequestActionListener {

    private EditText searchEditText;
    private Button searchButton, scanQRButton, createAllianceButton, backButton;
    private RecyclerView friendsRecyclerView, searchResultsRecyclerView, requestsRecyclerView;
    private TextView requestsHeader;

    private FriendsAdapter friendsAdapter;
    private SearchUsersAdapter searchAdapter;
    private FriendRequestAdapter requestAdapter;

    private FriendsRepository friendsRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Authentication error. Please log in.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        friendsRepository = new FriendsRepository();

        initViews();
        setupRecyclerViews();
        loadFriends();
        loadPendingRequests();
        setupButtons();
    }

    private void initViews() {
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        scanQRButton = findViewById(R.id.scanQRButton);
        createAllianceButton = findViewById(R.id.createAllianceButton);
        backButton = findViewById(R.id.backButton);
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        requestsHeader = findViewById(R.id.requestsHeader);
    }

    private void setupRecyclerViews() {
        // Friends list (click opens profile)
        friendsAdapter = new FriendsAdapter(new ArrayList<>(), this::onFriendClick);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendsRecyclerView.setAdapter(friendsAdapter);

        // Search adapter: we assume SearchUsersAdapter has OnUserActionListener with onAddFriend + onViewProfile
        searchAdapter = new SearchUsersAdapter(new ArrayList<>(), new SearchUsersAdapter.OnUserActionListener() {
            @Override
            public void onAddFriend(User user) {
                FriendsActivity.this.onAddFriend(user);
            }

            @Override
            public void onViewProfile(User user) {
                if (user != null && user.getUserId() != null) {
                    Intent intent = new Intent(FriendsActivity.this, FriendProfileActivity.class);
                    intent.putExtra("FRIEND_ID", user.getUserId());
                    startActivity(intent);
                } else {
                    Toast.makeText(FriendsActivity.this, "Greška: ID korisnika nije pronađen", Toast.LENGTH_SHORT).show();
                }
            }
        });
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(searchAdapter);

        // Requests
        requestAdapter = new FriendRequestAdapter(new ArrayList<>(), this);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestsRecyclerView.setAdapter(requestAdapter);
    }

    private void loadFriends() {
        MutableLiveData<List<User>> friendsLiveData = new MutableLiveData<>();
        friendsLiveData.observe(this, friends -> {
            if (friends == null) friends = new ArrayList<>();
            friendsAdapter.updateFriends(friends);
        });
        friendsRepository.loadFriends(currentUserId, friendsLiveData);
    }

    private void loadPendingRequests() {
        MutableLiveData<List<Friend>> requestsLiveData = new MutableLiveData<>();
        requestsLiveData.observe(this, requests -> {
            if (requests == null) requests = new ArrayList<>();
            requestAdapter.updateRequests(requests);

            if (requestsHeader != null) {
                requestsHeader.setVisibility(requests.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
        friendsRepository.loadPendingRequests(currentUserId, requestsLiveData);
    }

    private void setupButtons() {
        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                searchUsers(query);
            } else {
                Toast.makeText(this, "Enter a username", Toast.LENGTH_SHORT).show();
            }
        });

        scanQRButton.setOnClickListener(v -> startQRScanner());

        createAllianceButton.setOnClickListener(v -> {
            Intent intent = new Intent(FriendsActivity.this, CreateAllianceActivity.class);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void searchUsers(String query) {
        MutableLiveData<List<User>> usersLiveData = new MutableLiveData<>();
        usersLiveData.observe(this, users -> {
            if (users == null) users = new ArrayList<>();
            List<User> filtered = new ArrayList<>();
            for (User user : users) {
                if (user != null && !user.getUserId().equals(currentUserId)) {
                    filtered.add(user);
                }
            }
            searchAdapter.updateUsers(filtered);
        });
        friendsRepository.searchUsers(query, usersLiveData);
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan friend's QR code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            addFriendByUserId(result.getContents());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void addFriendByUserId(String userId) {
        if (userId.equals(currentUserId)) {
            Toast.makeText(this, "ERROR: Cannot add yourself as a friend.", Toast.LENGTH_SHORT).show();
            return;
        }

        MutableLiveData<User> userLiveData = new MutableLiveData<>();
        userLiveData.observe(this, user -> {
            if (user != null) {
                showFriendConfirmationDialog(user);
            } else {
                Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show();
            }
        });
        friendsRepository.getUserById(userId, userLiveData);
    }

    private void showFriendConfirmationDialog(User scannedUser) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Send Friend Request?")
                .setMessage("Do you want to send a friend request to " + scannedUser.getUsername() + "?")
                .setPositiveButton("Send Request", (dialog, which) -> onAddFriend(scannedUser))
                .setNeutralButton("View Profile", (dialog, which) -> {
                    // Otvori profil direktno iz dijaloga
                    if (scannedUser != null && scannedUser.getUserId() != null) {
                        Intent intent = new Intent(FriendsActivity.this, FriendProfileActivity.class);
                        intent.putExtra("FRIEND_ID", scannedUser.getUserId());
                        startActivity(intent);
                    } else {
                        Toast.makeText(FriendsActivity.this, "Greška: ID korisnika nije pronađen", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onAddFriend(User user) {
        if (user == null) return;

        MutableLiveData<String> resultLiveData = new MutableLiveData<>();
        resultLiveData.observe(this, result -> {
            if (result != null && result.startsWith("SUCCESS")) {
                Toast.makeText(this, "Friend request successfully sent to " + user.getUsername(), Toast.LENGTH_LONG).show();
                searchAdapter.updateUsers(new ArrayList<>());
            } else if (result != null) {
                Toast.makeText(this, result.replace("ERROR: ", ""), Toast.LENGTH_LONG).show();
            }
        });
        friendsRepository.addFriend(currentUserId, user, resultLiveData);
    }

    // Otvara profil (koristi se i iz FriendsAdapter i iz search adaptera)
    private void onFriendClick(User friend) {
        if (friend != null && friend.getUserId() != null) {
            Intent intent = new Intent(FriendsActivity.this, FriendProfileActivity.class);
            intent.putExtra("FRIEND_ID", friend.getUserId());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Greška: ID prijatelja nije pronađen", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAcceptRequest(Friend request) {
        if (request == null) return;

        MutableLiveData<String> resultLiveData = new MutableLiveData<>();
        resultLiveData.observe(this, result -> {
            if (result != null && result.startsWith("SUCCESS")) {
                Toast.makeText(this, "Friend request accepted!", Toast.LENGTH_SHORT).show();
                loadFriends();
                loadPendingRequests();
            } else if (result != null) {
                Toast.makeText(this, result.replace("ERROR: ", ""), Toast.LENGTH_SHORT).show();
            }
        });
        friendsRepository.acceptFriendRequest(request, resultLiveData);
    }

    @Override
    public void onRejectRequest(Friend request) {
        if (request == null) return;

        MutableLiveData<String> resultLiveData = new MutableLiveData<>();
        resultLiveData.observe(this, result -> {
            if (result != null && result.startsWith("SUCCESS")) {
                Toast.makeText(this, "Friend request rejected.", Toast.LENGTH_SHORT).show();
                loadPendingRequests();
            } else if (result != null) {
                Toast.makeText(this, result.replace("ERROR: ", ""), Toast.LENGTH_SHORT).show();
            }
        });
        friendsRepository.rejectFriendRequest(request, resultLiveData);
    }
}
