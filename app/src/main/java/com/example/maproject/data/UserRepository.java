package com.example.maproject.data;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.maproject.model.InventoryItem;
import com.example.maproject.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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
                                sendVerificationEmail(firebaseUser, registrationStatus);
                                saveUserProfile(firebaseUser.getUid(), email, username, avatar, registrationStatus);
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
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        registrationStatus.postValue("Registration succesful! Check your inbox to verify your email in the next 24h.");
                        Log.d(TAG, "Verification email sent.");
                    } else {
                        registrationStatus.postValue("Failed to send verification mail.");
                        Log.e(TAG, "Failed to send verification email.", task.getException());
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
                            // Proveri da li je email verifikovan
                            firebaseUser.reload().addOnCompleteListener(reloadTask -> {
                                if (firebaseUser.isEmailVerified()) {
                                    loginStatus.postValue("LOGIN_SUCCESS");
                                    Log.d(TAG, "Login successful.");
                                } else {
                                    auth.signOut();
                                    loginStatus.postValue("Email not verified. Check your spam inbox.");
                                    Log.w(TAG, "Email not verified.");
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

    public void buyItem(String userId, InventoryItem item, MutableLiveData<String> status) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        status.postValue("Korisnik ne postoji!");
                        return;
                    }

                    User user = documentSnapshot.toObject(User.class);
                    if (user == null) {
                        status.postValue("Greška pri učitavanju korisnika.");
                        return;
                    }

                    // Provera da li korisnik ima dovoljno novčića
                    int cost = calculateItemCost(item);
                    if (user.getCoins() < cost) {
                        status.postValue("Nemate dovoljno novčića!");
                        return;
                    }

                    // Oduzmi novčiće
                    user.setCoins(user.getCoins() - cost);

                    // Dodaj item
                    user.addItemToEquipment(item);

                    // Sačuvaj nazad u Firestore
                    db.collection("users").document(userId).set(user.toMap())
                            .addOnSuccessListener(aVoid -> status.postValue("Kupljeno: " + item.getName()))
                            .addOnFailureListener(e -> status.postValue("Greška prilikom kupovine."));
                })
                .addOnFailureListener(e -> status.postValue("Greška pri učitavanju korisnika."));
    }

    private int calculateItemCost(InventoryItem item) {
        // TODO: implementiraj logiku za cene na osnovu tipa i opisa
        // Primer hardkodiranih cena:
        switch (item.getItemId()) {
            case "potion1": return 50;
            case "potion2": return 70;
            case "potion3": return 200;
            case "potion4": return 1000;
            case "gloves": return 60;
            case "boots": return 80;
            case "shield": return 60;
            case "sword": return 0; // oružje samo od boss-a
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

    // Callback interface
    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
    }


    public void logout() {
        auth.signOut();
    }

    // Interface za callback
    interface OnUsernameCheckListener {
        void onResult(boolean isAvailable);
    }
}