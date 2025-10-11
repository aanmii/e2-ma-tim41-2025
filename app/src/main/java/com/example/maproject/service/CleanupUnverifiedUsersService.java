package com.example.maproject.service;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Servis za čišćenje neverifikovanih naloga nakon 24h
 * Ovo bi trebalo pokrenuti periodično (npr. putem WorkManager-a)
 */
public class CleanupUnverifiedUsersService {

    private static final String TAG = "CleanupService";
    private static final long TWENTY_FOUR_HOURS_MILLIS = 24 * 60 * 60 * 1000;

    public static void cleanupUnverifiedUsers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - TWENTY_FOUR_HOURS_MILLIS;

        db.collection("users")
                .whereEqualTo("isEmailVerified", false)
                .whereLessThan("registrationTimestamp", cutoffTime)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Obriši korisnika iz Firestore
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Deleted unverified user: " + document.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to delete user: " + document.getId(), e);
                                });

                        // Napomena: Firebase Auth korisnika ne možeš obrisati odavde
                        // To bi trebalo uraditi kroz Firebase Cloud Functions
                        // Ili korisnik mora ponovo da se registruje
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching unverified users", e);
                });
    }
}