package com.example.maproject.data;

import com.example.maproject.model.Task;
import com.google.firebase.firestore.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TaskRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CompletableFuture<Void> createTask(Task task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DocumentReference ref = db.collection("tasks").document();
        task.setTaskId(ref.getId());
        ref.set(task)
                .addOnSuccessListener(aVoid -> future.complete(null))
                .addOnFailureListener(future::completeExceptionally);
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
        db.collection("tasks").document(task.getTaskId())
                .set(task)
                .addOnSuccessListener(aVoid -> future.complete(null))
                .addOnFailureListener(future::completeExceptionally);
        return future;
    }

    public CompletableFuture<Void> deleteTask(String taskId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        db.collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> future.complete(null))
                .addOnFailureListener(future::completeExceptionally);
        return future;
    }
}
