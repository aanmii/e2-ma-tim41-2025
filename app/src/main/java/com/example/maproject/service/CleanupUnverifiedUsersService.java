package com.example.maproject.service;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class CleanupUnverifiedUsersService {

    private static final String TAG = "CleanupService";
    private static final long TWENTY_FOUR_HOURS_MILLIS = 5 * 60 * 1000; // for testing purposes its 5 mins

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

                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Deleted unverified Firestore user (Auth handled by Cloud Function): "
                                            + document.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to delete user: " + document.getId(), e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching unverified users", e);
                });
    }
}
