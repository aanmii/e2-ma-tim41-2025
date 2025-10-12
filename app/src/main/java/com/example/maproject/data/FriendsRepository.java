package com.example.maproject.data;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.example.maproject.model.Friend;
import com.example.maproject.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FriendsRepository {
    private static final String TAG = "FriendsRepository";
    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;
    private final CollectionReference friendshipsCollection;
    private final CollectionReference usersCollection;

    public FriendsRepository() {
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
        friendshipsCollection = db.collection("friendships");
        usersCollection = db.collection("users");
    }

    public void searchUsers(String query, MutableLiveData<List<User>> usersLiveData) {
        usersCollection
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + '\uf8ff')
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        User user = document.toObject(User.class);
                        users.add(user);
                    }
                    usersLiveData.postValue(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching users", e);
                    usersLiveData.postValue(new ArrayList<>());
                });
    }

    public void getUserById(String userId, MutableLiveData<User> userLiveData) {
        usersCollection.document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        userLiveData.postValue(user);
                    } else {
                        userLiveData.postValue(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user", e);
                    userLiveData.postValue(null);
                });
    }

    public void addFriend(String senderId, User receiverUser, MutableLiveData<String> resultLiveData) {
        if (senderId.equals(receiverUser.getUserId())) {
            resultLiveData.postValue("ERROR: Cannot add yourself as a friend.");
            return;
        }

        friendshipsCollection
                .whereIn("status", List.of(Friend.FriendshipStatus.PENDING.name(), Friend.FriendshipStatus.ACCEPTED.name()))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Friend friendship = doc.toObject(Friend.class);
                        if ((friendship.getUserId().equals(senderId) && friendship.getFriendId().equals(receiverUser.getUserId())) ||
                                (friendship.getUserId().equals(receiverUser.getUserId()) && friendship.getFriendId().equals(senderId))) {
                            if (friendship.getStatus().equals(Friend.FriendshipStatus.PENDING.name())) {
                                resultLiveData.postValue("ERROR: Friend request is already pending.");
                                return;
                            } else if (friendship.getStatus().equals(Friend.FriendshipStatus.ACCEPTED.name())) {
                                resultLiveData.postValue("ERROR: You are already friends.");
                                return;
                            }
                        }
                    }

                    usersCollection.document(senderId).get()
                            .addOnSuccessListener(senderDoc -> {
                                if (!senderDoc.exists()) {
                                    resultLiveData.postValue("ERROR: Sender user data not found.");
                                    return;
                                }
                                User senderUser = senderDoc.toObject(User.class);
                                String friendshipId = friendshipsCollection.document().getId();

                                Friend friendshipRequest = new Friend(
                                        receiverUser.getUserId(),
                                        senderUser.getUserId(),
                                        senderUser.getUsername(),
                                        senderUser.getAvatar()
                                );
                                friendshipRequest.setFriendshipId(friendshipId);
                                friendshipRequest.setStatus(Friend.FriendshipStatus.PENDING);

                                friendshipsCollection.document(friendshipId).set(friendshipRequest.toMap())
                                        .addOnSuccessListener(aVoid -> {
                                            com.example.maproject.model.Notification notification =
                                                    new com.example.maproject.model.Notification(
                                                            receiverUser.getUserId(),
                                                            "FRIEND_REQUEST",
                                                            "Friend request from " + senderUser.getUsername(),
                                                            friendshipId
                                                    );
                                            notificationRepository.sendNotification(notification);
                                            resultLiveData.postValue("SUCCESS: Request Sent");
                                        })
                                        .addOnFailureListener(e -> resultLiveData.postValue("ERROR: Failed to send request."));
                            })
                            .addOnFailureListener(e -> resultLiveData.postValue("ERROR: Failed to retrieve sender data."));
                })
                .addOnFailureListener(e -> resultLiveData.postValue("ERROR: Database check failed."));
    }

    public ListenerRegistration loadFriends(String userId, MutableLiveData<List<User>> friendsLiveData) {
        return friendshipsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", Friend.FriendshipStatus.ACCEPTED.name())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading friends (real-time)", error);
                        friendsLiveData.postValue(new ArrayList<>());
                        return;
                    }

                    List<User> friends = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Friend friendship = document.toObject(Friend.class);
                            User friend = new User();
                            friend.setUserId(friendship.getFriendId());
                            friend.setUsername(friendship.getFriendUsername());
                            friend.setAvatar(friendship.getFriendAvatar());
                            friends.add(friend);
                        }
                    }
                    friendsLiveData.postValue(friends);
                });
    }

    public ListenerRegistration loadPendingRequests(String receiverId, MutableLiveData<List<Friend>> requestsLiveData) {
        return friendshipsCollection
                .whereEqualTo("userId", receiverId)
                .whereEqualTo("status", Friend.FriendshipStatus.PENDING.name())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading pending requests (real-time)", error);
                        requestsLiveData.postValue(new ArrayList<>());
                        return;
                    }

                    List<Friend> requests = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            requests.add(doc.toObject(Friend.class));
                        }
                    }
                    requestsLiveData.postValue(requests);
                });
    }

    public void acceptFriendRequest(Friend request, MutableLiveData<String> resultLiveData) {
        String senderId = request.getFriendId();
        String receiverId = request.getUserId();

        friendshipsCollection.document(request.getFriendshipId())
                .update("status", Friend.FriendshipStatus.ACCEPTED.name())
                .addOnSuccessListener(aVoid -> {
                    usersCollection.document(receiverId).get()
                            .addOnSuccessListener(receiverDoc -> {
                                if (!receiverDoc.exists()) {
                                    resultLiveData.postValue("ERROR: Receiver user data not found for acceptance.");
                                    return;
                                }
                                User receiverUser = receiverDoc.toObject(User.class);
                                usersCollection.document(senderId).get()
                                        .addOnSuccessListener(senderDoc -> {
                                            if (!senderDoc.exists()) {
                                                resultLiveData.postValue("ERROR: Sender user data not found for acceptance.");
                                                return;
                                            }
                                            User senderUser = senderDoc.toObject(User.class);

                                            String reverseFriendshipId = friendshipsCollection.document().getId();
                                            Friend reverseFriendship = new Friend(
                                                    senderId,
                                                    receiverId,
                                                    receiverUser.getUsername(),
                                                    receiverUser.getAvatar()
                                            );
                                            reverseFriendship.setFriendshipId(reverseFriendshipId);
                                            reverseFriendship.setStatus(Friend.FriendshipStatus.ACCEPTED);

                                            friendshipsCollection.document(reverseFriendshipId).set(reverseFriendship.toMap())
                                                    .addOnSuccessListener(aVoid2 -> resultLiveData.postValue("SUCCESS: Friendship accepted."))
                                                    .addOnFailureListener(e -> resultLiveData.postValue("ERROR: Failed to finalize friendship."));
                                        })
                                        .addOnFailureListener(e -> resultLiveData.postValue("ERROR: Failed to retrieve sender data."));
                            })
                            .addOnFailureListener(e -> resultLiveData.postValue("ERROR: Failed to retrieve receiver data."));
                })
                .addOnFailureListener(e -> resultLiveData.postValue("ERROR: Failed to update request status."));
    }

    public void rejectFriendRequest(Friend request, MutableLiveData<String> resultLiveData) {
        friendshipsCollection.document(request.getFriendshipId())
                .delete()
                .addOnSuccessListener(aVoid -> resultLiveData.postValue("SUCCESS: Request rejected."))
                .addOnFailureListener(e -> resultLiveData.postValue("ERROR: Failed to reject request."));
    }
}
