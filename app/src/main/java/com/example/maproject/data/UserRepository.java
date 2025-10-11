package com.example.maproject.data;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

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
        // Prvo proveri da li korisničko ime već postoji
        checkUsernameAvailability(username, isAvailable -> {
            if (!isAvailable) {
                registrationStatus.postValue("Korisničko ime je već zauzeto. Izaberite drugo.");
                return;
            }

            // Kreiraj Firebase Auth nalog
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
                            registrationStatus.postValue("Registracija neuspešna: " + error);
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
                        registrationStatus.postValue("Registracija uspešna! Poslali smo vam verifikacioni email. Proverite inbox i aktivirajte nalog u roku od 24h.");
                        Log.d(TAG, "Verification email sent.");
                    } else {
                        registrationStatus.postValue("Greška pri slanju verifikacionog emaila.");
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
                    registrationStatus.postValue("Greška pri čuvanju profila u bazi.");
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
                                    loginStatus.postValue("Nalog nije aktiviran. Proverite email za aktivacioni link.");
                                    Log.w(TAG, "Email not verified.");
                                }
                            });
                        }
                    } else {
                        String error = Objects.requireNonNull(task.getException()).getMessage();
                        loginStatus.postValue("Prijava neuspešna: " + error);
                        Log.e(TAG, "Login failed: " + error);
                    }
                });
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void logout() {
        auth.signOut();
    }

    // Interface za callback
    interface OnUsernameCheckListener {
        void onResult(boolean isAvailable);
    }
}