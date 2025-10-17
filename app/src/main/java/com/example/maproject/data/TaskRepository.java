package com.example.maproject.data;

import com.example.maproject.model.Task;
import com.example.maproject.service.StatisticsManagerService;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.android.gms.tasks.OnCompleteListener;

import java.util.Map;
import java.util.HashMap;

public class TaskRepository {

    private final FirebaseFirestore db;
    private final StatisticsManagerService statisticsManager;
    private final CollectionReference tasksCollection;
    private static final String TASK_COLLECTION = "tasks";

    public TaskRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.statisticsManager = new StatisticsManagerService();
        this.tasksCollection = db.collection(TASK_COLLECTION);
    }

    public void createTask(Task task, OnCompleteListener<Void> onComplete) {
        tasksCollection.add(task.toMap())
                .addOnSuccessListener(documentReference -> {
                    task.setTaskId(documentReference.getId());

                    tasksCollection.document(task.getTaskId())
                            .set(task.toMap(), SetOptions.merge())
                            .addOnCompleteListener(taskResult -> {
                                if (taskResult.isSuccessful()) {
                                    statisticsManager.updateCreatedTaskCount(task.getUserId(), 1);
                                }
                                onComplete.onComplete(taskResult);
                            });
                })
                .addOnFailureListener(e -> {
                    onComplete.onComplete(com.google.android.gms.tasks.Tasks.forException(e));
                });
    }

    public void updateTaskStatus(Task task, Task.Status oldStatus, OnCompleteListener<Void> onComplete) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", task.getStatus().name());

        tasksCollection.document(task.getTaskId())
                .update(updates)
                .addOnCompleteListener(taskResult -> {
                    if (taskResult.isSuccessful()) {
                        statisticsManager.updateStatisticsOnTaskStatusChange(task.getUserId(), task, oldStatus);
                    }
                    onComplete.onComplete(taskResult);
                });
    }

    public void updateTask(String taskId, Map<String, Object> updates, OnCompleteListener<Void> onComplete) {
        tasksCollection.document(taskId)
                .update(updates)
                .addOnCompleteListener(onComplete);
    }

    public void deleteTask(Task task, OnCompleteListener<Void> onComplete) {
        final Task.Status currentStatus = task.getStatus();

        tasksCollection.document(task.getTaskId()).delete()
                .addOnCompleteListener(taskResult -> {
                    if (taskResult.isSuccessful()) {

                        Task taskForStats = task;
                        taskForStats.setStatus(Task.Status.ACTIVE);

                        statisticsManager.updateStatisticsOnTaskStatusChange(
                                task.getUserId(),
                                taskForStats,
                                currentStatus
                        );

                        statisticsManager.updateCreatedTaskCount(task.getUserId(), -1);
                    }
                    onComplete.onComplete(taskResult);
                });
    }

    public Query fetchTasksByUserId(String userId) {
        return tasksCollection.whereEqualTo("userId", userId)
                .orderBy("executionTime", Query.Direction.ASCENDING);
    }
}