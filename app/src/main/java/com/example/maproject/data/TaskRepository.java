package com.example.maproject.data;

import com.example.maproject.model.Task;
import com.example.maproject.service.StatisticsManagerService;
import com.google.firebase.firestore.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TaskRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final StatisticsManagerService statisticsManager = new StatisticsManagerService();

    public CompletableFuture<Void> createTask(Task task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DocumentReference ref = db.collection("tasks").document();
        task.setTaskId(ref.getId());

        DocumentReference taskRef = ref;
        String categoryId = task.getCategoryId();

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // write the task document
            transaction.set(taskRef, task);

            // if there's a categoryId, ensure the category has this taskId in its taskIds list
            if (categoryId != null) {
                DocumentReference catRef = db.collection("categories").document(categoryId);
                // try to read category; if it exists update array, otherwise create a minimal doc with taskIds
                DocumentSnapshot catSnap = transaction.get(catRef);
                if (catSnap.exists()) {
                    transaction.update(catRef, "taskIds", FieldValue.arrayUnion(task.getTaskId()));
                } else {
                    Map<String, Object> catData = new HashMap<>();
                    List<String> ids = new ArrayList<>();
                    ids.add(task.getTaskId());
                    catData.put("taskIds", ids);
                    // create the category doc (minimal) so the linkage exists; this will set only taskIds
                    transaction.set(catRef, catData);
                }
            }

            return null;
        }).addOnSuccessListener(aVoid -> {
            // update statistics after successful transaction
            statisticsManager.updateCreatedTaskCount(task.getUserId(), 1);
            future.complete(null);
        }).addOnFailureListener(future::completeExceptionally);

        return future;
    }

    public CompletableFuture<List<Task>> getTasksByUser(String userId) {
        CompletableFuture<List<Task>> future = new CompletableFuture<>();
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Task> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        list.add(doc.toObject(Task.class));
                    }
                    future.complete(list);
                })
                .addOnFailureListener(future::completeExceptionally);
        return future;
    }

    public CompletableFuture<Void> updateTask(Task task) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        DocumentReference taskRef = db.collection("tasks").document(task.getTaskId());

        // Use a transaction to safely move taskId between category.taskIds when categoryId changes
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot before = transaction.get(taskRef);
            String oldCatId = before.contains("categoryId") ? before.getString("categoryId") : null;
            // overwrite task doc
            transaction.set(taskRef, task);

            String newCatId = task.getCategoryId();
            if (!Objects.equals(oldCatId, newCatId)) {
                // remove from old category if it exists
                if (oldCatId != null) {
                    DocumentReference oldCatRef = db.collection("categories").document(oldCatId);
                    DocumentSnapshot oldSnap = transaction.get(oldCatRef);
                    if (oldSnap.exists()) {
                        transaction.update(oldCatRef, "taskIds", FieldValue.arrayRemove(task.getTaskId()));
                    }
                }

                // add to new category, creating it if necessary
                if (newCatId != null) {
                    DocumentReference newCatRef = db.collection("categories").document(newCatId);
                    DocumentSnapshot newSnap = transaction.get(newCatRef);
                    if (newSnap.exists()) {
                        transaction.update(newCatRef, "taskIds", FieldValue.arrayUnion(task.getTaskId()));
                    } else {
                        Map<String, Object> catData = new HashMap<>();
                        List<String> ids = new ArrayList<>();
                        ids.add(task.getTaskId());
                        catData.put("taskIds", ids);
                        transaction.set(newCatRef, catData);
                    }
                }
            }

            return null;
        }).addOnSuccessListener(aVoid -> future.complete(null)).addOnFailureListener(future::completeExceptionally);

        return future;
    }

    public CompletableFuture<Void> deleteTask(String taskId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DocumentReference taskRef = db.collection("tasks").document(taskId);

        // Run transaction to remove task and remove its id from category.taskIds if needed
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snap = transaction.get(taskRef);
            if (!snap.exists()) return null;
            String catId = snap.contains("categoryId") ? snap.getString("categoryId") : null;
            if (catId != null) {
                DocumentReference catRef = db.collection("categories").document(catId);
                DocumentSnapshot catSnap = transaction.get(catRef);
                if (catSnap.exists()) {
                    transaction.update(catRef, "taskIds", FieldValue.arrayRemove(taskId));
                }
            }
            transaction.delete(taskRef);
            return null;
        }).addOnSuccessListener(aVoid -> future.complete(null)).addOnFailureListener(future::completeExceptionally);

        return future;
    }
}