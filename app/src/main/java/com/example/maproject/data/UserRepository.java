package com.example.maproject.data;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.maproject.model.InventoryItem;
import com.example.maproject.model.User;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserRepository {

    private static final String TAG = "UserRepository";
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public UserRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void registerUser(String email, String password, String username, String avatar, MutableLiveData<String> registrationStatus) {

        checkUsernameAvailability(username, isAvailable -> {
            if (!isAvailable) {
                registrationStatus.postValue("Username is unavailable.");
                return;
            }

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            if (firebaseUser != null) {
                                saveUserProfile(firebaseUser.getUid(), email, username, avatar, registrationStatus);
                                sendVerificationEmail(firebaseUser, registrationStatus);
                            }
                        } else {
                            String error = Objects.requireNonNull(task.getException()).getMessage();
                            registrationStatus.postValue("Registration failed: " + error);
                            Log.e(TAG, "Registration failed: " + error);
                        }
                    });
        });
    }

    private void checkUsernameAvailability(String username, OnUsernameCheckListener listener) {
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listener.onResult(querySnapshot.isEmpty());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking username", e);
                    listener.onResult(false);
                });
    }

    private void sendVerificationEmail(FirebaseUser firebaseUser, MutableLiveData<String> registrationStatus) {
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                .setHandleCodeInApp(false)
                .setUrl("https://maproject-f8fb7.firebaseapp.com")
                .build();

        firebaseUser.sendEmailVerification(actionCodeSettings)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        registrationStatus.postValue("Registration successful! Check your email to verify your account. Link is valid for 1 hour.");
                        Log.d(TAG, "Verification email sent to: " + firebaseUser.getEmail());
                    } else {
                        firebaseUser.sendEmailVerification()
                                .addOnCompleteListener(retryTask -> {
                                    if (retryTask.isSuccessful()) {
                                        registrationStatus.postValue("Registration successful! Check your email (and spam folder) to verify your account.");
                                        Log.d(TAG, "Verification email sent (retry)");
                                    } else {
                                        registrationStatus.postValue("Registration successful, but failed to send verification email. Please contact support.");
                                        Log.e(TAG, "Failed to send verification email", retryTask.getException());
                                    }
                                });
                    }
                });
    }

    private void saveUserProfile(String userId, String email, String username, String avatar, MutableLiveData<String> registrationStatus) {
        User user = new User(userId, email, username, avatar);

        db.collection("users").document(userId).set(user.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile saved to Firestore.");
                })
                .addOnFailureListener(e -> {
                    registrationStatus.postValue("Error saving profile to Firestore.");
                    Log.e(TAG, "Error saving profile to Firestore", e);
                });
    }

    public void loginUser(String email, String password, MutableLiveData<String> loginStatus) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {

                            firebaseUser.reload().addOnCompleteListener(reloadTask -> {
                                if (reloadTask.isSuccessful()) {
                                    if (firebaseUser.isEmailVerified()) {
                                        loginStatus.postValue("LOGIN_SUCCESS");
                                        Log.d(TAG, "Login successful for: " + firebaseUser.getEmail());
                                        updateFirestoreVerificationStatus(firebaseUser.getUid(), true);
                                    } else {
                                        auth.signOut();
                                        loginStatus.postValue("Email not verified. Check your inbox and spam folder. Click 'Resend' if needed.");
                                        Log.w(TAG, "Email not verified for: " + firebaseUser.getEmail());
                                    }
                                } else {
                                    loginStatus.postValue("Error checking verification status. Please try again.");
                                    Log.e(TAG, "Failed to reload user", reloadTask.getException());
                                    auth.signOut();
                                }
                            });
                        }
                    } else {
                        String error = Objects.requireNonNull(task.getException()).getMessage();
                        loginStatus.postValue("Login failed: " + error);
                        Log.e(TAG, "Login failed: " + error);
                    }
                });
    }

    private void updateFirestoreVerificationStatus(String userId, boolean isVerified) {
        db.collection("users").document(userId)
                .update("isEmailVerified", isVerified)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore 'isEmailVerified' updated to: " + isVerified))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update 'isEmailVerified' in Firestore.", e));
    }

    public void resendVerificationEmail(MutableLiveData<String> status) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            status.postValue("No user logged in.");
            return;
        }

        if (user.isEmailVerified()) {
            status.postValue("Email is already verified!");
            return;
        }

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        status.postValue("Verification email sent! Check your inbox and spam folder.");
                        Log.d(TAG, "Verification email resent to: " + user.getEmail());
                    } else {
                        status.postValue("Failed to send verification email. Please try again later.");
                        Log.e(TAG, "Failed to resend verification email", task.getException());
                    }
                });
    }

// U UserRepository.java - ažuriraj buyItem metodu

    public void buyItem(String userId, InventoryItem item, int price, OnStatusListener listener) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null) {
                        listener.onStatus("User not found");
                        return;
                    }

                    if (user.getCoins() < price) {
                        listener.onStatus("Not enough coins!");
                        return;
                    }

                    // Oduzmi novčiće
                    user.setCoins(user.getCoins() - price);

                    // Dodaj stavku u inventar
                    List<InventoryItem> equipment = user.getEquipment();
                    if (equipment == null) equipment = new ArrayList<>();

                    // Proveri da li stavka već postoji
                    boolean found = false;
                    for (InventoryItem existing : equipment) {
                        if (existing.getItemId().equals(item.getItemId())) {
                            // Povećaj količinu
                            existing.setQuantity(existing.getQuantity() + 1);
                            found = true;

                            // Log za debug
                            Log.d("UserRepository", "Item stacked: " + item.getName() +
                                    " | New quantity: " + existing.getQuantity() +
                                    " | PP Bonus: " + existing.getPpBonus());
                            break;
                        }
                    }

                    if (!found) {
                        equipment.add(item);
                        Log.d("UserRepository", "New item added: " + item.getName() +
                                " | PP Bonus: " + item.getPpBonus() +
                                " | Attack Bonus: " + item.getAttackSuccessBonus() +
                                " | Extra Attack Chance: " + item.getExtraAttackChance());
                    }

                    user.setEquipment(equipment);

                    // Sačuvaj koristeći toMap() da bi se svi podaci sačuvali
                    db.collection("users").document(userId)
                            .set(user.toMap())
                            .addOnSuccessListener(aVoid -> {
                                listener.onStatus("Item purchased successfully!");
                                Log.d("UserRepository", "Equipment saved to Firestore");
                            })
                            .addOnFailureListener(e -> {
                                listener.onStatus("Purchase failed: " + e.getMessage());
                                Log.e("UserRepository", "Failed to save equipment", e);
                            });
                })
                .addOnFailureListener(e -> {
                    listener.onStatus("Error: " + e.getMessage());
                    Log.e("UserRepository", "Failed to load user", e);
                });
    }

    public interface OnStatusListener {
        void onStatus(String message);
    }
    private int calculateItemCost(InventoryItem item) {
        switch (item.getItemId()) {
            case "potion1": return 50;
            case "potion2": return 70;
            case "potion3": return 200;
            case "potion4": return 1000;
            case "gloves": return 60;
            case "boots": return 80;
            case "shield": return 60;
            case "sword": return 0;
            case "bow": return 0;
            default: return 100;
        }
    }

    public void getUser(String userId, OnUserLoadedListener listener) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        listener.onUserLoaded(null);
                        return;
                    }

                    User user = documentSnapshot.toObject(User.class);
                    listener.onUserLoaded(user);
                })
                .addOnFailureListener(e -> {
                    listener.onUserLoaded(null);
                });
    }

    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
    }

    public void logout() {
        auth.signOut();
    }

    interface OnUsernameCheckListener {
        void onResult(boolean isAvailable);
    }
}