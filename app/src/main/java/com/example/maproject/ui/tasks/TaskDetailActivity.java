package com.example.maproject.ui.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.example.maproject.model.Task;
import com.example.maproject.model.User;
import com.example.maproject.service.LevelUpProcessor;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TaskDetailActivity extends AppCompatActivity {

    private TextView textTitle, textCategory, textExecutionTime, textDifficulty, textImportance, textDescription, textStatus;
    private Button buttonMarkDone, buttonPauseReactivate, buttonCancel, buttonEdit, buttonDelete;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String taskId;
    private DocumentReference taskRef;
    private Map<String, Object> currentData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        textTitle = findViewById(R.id.text_title);
        textCategory = findViewById(R.id.text_category);
        textExecutionTime = findViewById(R.id.text_execution_time);
        textDifficulty = findViewById(R.id.text_difficulty);
        textImportance = findViewById(R.id.text_importance);
        textDescription = findViewById(R.id.text_description);
        textStatus = findViewById(R.id.text_status);

        buttonMarkDone = findViewById(R.id.button_mark_done);
        buttonPauseReactivate = findViewById(R.id.button_pause_reactivate);
        buttonCancel = findViewById(R.id.button_cancel);
        buttonEdit = findViewById(R.id.button_edit);
        buttonDelete = findViewById(R.id.button_delete);

        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null) {
            Toast.makeText(this, "No task specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        taskRef = db.collection("tasks").document(taskId);
        loadTask();

        buttonMarkDone.setOnClickListener(v -> markDone());
        buttonCancel.setOnClickListener(v -> cancelTask());
        buttonPauseReactivate.setOnClickListener(v -> togglePauseReactivate());
        buttonEdit.setOnClickListener(v -> openEdit());
        buttonDelete.setOnClickListener(v -> deleteTask());
    }

    private void loadTask() {
        taskRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            currentData = doc.getData();
            if (currentData == null) currentData = new HashMap<>();

            String title = doc.getString("title");
            String category = doc.getString("categoryName");
            Long exec = doc.getLong("executionTime");
            String difficulty = doc.getString("difficulty");
            String importance = doc.getString("importance");
            String description = doc.getString("description");
            String status = doc.getString("status");

            textTitle.setText(title != null ? title : "(no title)");
            textCategory.setText(getString(R.string.label_category, category != null ? category : ""));
            textExecutionTime.setText(getString(R.string.label_execution, exec != null ? new Date(exec).toString() : ""));
            textDifficulty.setText(getString(R.string.label_difficulty, difficulty != null ? difficulty : ""));
            textImportance.setText(getString(R.string.label_importance, importance != null ? importance : ""));
            textDescription.setText(getString(R.string.label_description, description != null ? description : ""));
            textStatus.setText(getString(R.string.label_status, status != null ? status : ""));

            enforceRules(doc);
        });
    }

    private void enforceRules(DocumentSnapshot doc) {
        String status = doc.getString("status");
        Long exec = doc.getLong("executionTime");

        long now = System.currentTimeMillis();

        // Disable editing/deleting/marking for Not Done or Cancelled or Completed
        if ("NOT_DONE".equals(status) || "CANCELLED".equals(status)) {
            disableAllExceptView("This task cannot be modified (Not Done/Cancelled).");
            return;
        }

        // If completed, just disable editing/deleting
        if ("COMPLETED".equals(status)) {
            disableAllExceptView("This task is completed and cannot be modified.");
            return;
        }

        // If task is older than 3 days past execution and still active, auto-mark Not Done and disable
        if (exec != null) {
            long threeDaysMs = 3L * 24L * 60L * 60L * 1000L;
            if (now > exec + threeDaysMs && "ACTIVE".equals(status)) {
                taskRef.update("status", Task.Status.NOT_DONE.name()).addOnSuccessListener(aVoid -> {
                    textStatus.setText(getString(R.string.status_not_done));
                    disableAllExceptView("Task expired and marked Not Done automatically.");
                });
                // no explicit return needed here
            }
        }

        // By default disable everything first, then enable allowed actions below based on status.
        buttonMarkDone.setEnabled(false);
        buttonPauseReactivate.setEnabled(false);
        buttonCancel.setEnabled(false);
        buttonEdit.setEnabled(false);
        buttonDelete.setEnabled(false);

        // Only ACTIVE tasks can be marked Done, Cancelled, or Paused.
        if ("ACTIVE".equals(status)) {
            // Mark Done button only allowed if execution time has passed
            boolean canMarkDone = exec != null && now >= exec;
            buttonMarkDone.setEnabled(canMarkDone);

            // Cancel allowed for ACTIVE tasks
            buttonCancel.setEnabled(true);

            // Pause button: per spec, ACTIVE tasks can be paused
            buttonPauseReactivate.setText(getString(R.string.pause));
            buttonPauseReactivate.setEnabled(true);

            // Edit button: allowed for ACTIVE tasks
            buttonEdit.setEnabled(true);

            // Delete button: allowed for non-final statuses
            buttonDelete.setEnabled(true);

            return;
        }

        // PAUSED tasks: only allow Reactivate. Do not allow marking Done or Cancel.
        if ("PAUSED".equals(status)) {
            // Reactivate allowed for paused tasks (per spec)
            buttonPauseReactivate.setText(getString(R.string.reactivate));
            buttonPauseReactivate.setEnabled(true);

            // keep other actions disabled (mark done, cancel, edit)

            // Deletion remains allowed for non-final statuses (PAUSED is allowed)
            buttonDelete.setEnabled(true);
        }

        // For any other non-final statuses (if introduced), keep conservative defaults:
        // allow edit only if ACTIVE (handled above), otherwise keep everything disabled except view.
    }

    private void disableAllExceptView(String message) {
        buttonMarkDone.setEnabled(false);
        buttonPauseReactivate.setEnabled(false);
        buttonCancel.setEnabled(false);
        buttonEdit.setEnabled(false);
        buttonDelete.setEnabled(false);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void markDone() {
        Long exec = (Long) currentData.get("executionTime");
        long now = System.currentTimeMillis();
        if (exec != null && now < exec) {
            Toast.makeText(this, "Cannot mark task done before its execution time", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Task.Status.COMPLETED.name());
        updates.put("completedTime", System.currentTimeMillis());

        taskRef.update(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Task marked done", Toast.LENGTH_SHORT).show();

            // After marking done, determine whether this completion should award XP.
            String userId = (String) currentData.get("userId");
            String difficulty = (String) currentData.get("difficulty");
            String importance = (String) currentData.get("importance");

            if (userId == null) {
                // fallback: try to award to task's creator (if task stored creator under createdBy)
                userId = (String) currentData.get("createdBy");
            }

            // Build query parameters depending on limits
            long sinceTs = -1;
            int allowed = Integer.MAX_VALUE;
            boolean hasLimit = false;
            int limitKind = 0; // 0 = none, 1 = SPECIAL, 2 = EXTREMELY_HARD, 3 = VERY_EASY+NORMAL, 4 = EASY+IMPORTANT, 5 = HARD+EXTREMELY_IMPORTANT

            if (importance != null && importance.equals("SPECIAL")) {
                hasLimit = true;
                allowed = 1;
                limitKind = 1;
                sinceTs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30); // rolling 30 days for "per month"
            } else if (difficulty != null && difficulty.equals("EXTREMELY_HARD")) {
                hasLimit = true;
                allowed = 1;
                limitKind = 2;
                sinceTs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
            } else if ("VERY_EASY".equals(difficulty) && "NORMAL".equals(importance)) {
                hasLimit = true;
                allowed = 5;
                limitKind = 3;
                // start of day
                sinceTs = System.currentTimeMillis() - (System.currentTimeMillis() % TimeUnit.DAYS.toMillis(1));
            } else if ("EASY".equals(difficulty) && "IMPORTANT".equals(importance)) {
                hasLimit = true;
                allowed = 5;
                limitKind = 4;
                sinceTs = System.currentTimeMillis() - (System.currentTimeMillis() % TimeUnit.DAYS.toMillis(1));
            } else if ("HARD".equals(difficulty) && "EXTREMELY_IMPORTANT".equals(importance)) {
                hasLimit = true;
                allowed = 2;
                limitKind = 5;
                sinceTs = System.currentTimeMillis() - (System.currentTimeMillis() % TimeUnit.DAYS.toMillis(1));
            }

            if (userId == null) {
                // nothing we can do to award XP
                loadTask();
                return;
            }

            if (hasLimit) {
                final String finalUserId = userId;
                final String finalDifficulty = difficulty;
                final String finalImportance = importance;
                final int finalAllowed = allowed;
                final int finalLimitKind = limitKind;

                FirebaseFirestore.getInstance().collection("tasks")
                        .whereEqualTo("userId", finalUserId)
                        .whereEqualTo("status", Task.Status.COMPLETED.name())
                        .whereGreaterThanOrEqualTo("completedTime", sinceTs)
                        .get()
                        .addOnSuccessListener(qs -> {
                            int count = 0;
                            for (DocumentSnapshot d : qs.getDocuments()) {
                                String dDiff = d.getString("difficulty");
                                String dImp = d.getString("importance");

                                boolean matches = false;
                                switch (finalLimitKind) {
                                    case 1: // SPECIAL
                                        if ("SPECIAL".equals(dImp)) matches = true;
                                        break;
                                    case 2: // EXTREMELY_HARD
                                        if ("EXTREMELY_HARD".equals(dDiff)) matches = true;
                                        break;
                                    case 3: // VERY_EASY + NORMAL
                                        if ("VERY_EASY".equals(dDiff) && "NORMAL".equals(dImp)) matches = true;
                                        break;
                                    case 4: // EASY + IMPORTANT
                                        if ("EASY".equals(dDiff) && "IMPORTANT".equals(dImp)) matches = true;
                                        break;
                                    case 5: // HARD + EXTREMELY_IMPORTANT
                                        if ("HARD".equals(dDiff) && "EXTREMELY_IMPORTANT".equals(dImp)) matches = true;
                                        break;
                                    default:
                                        break;
                                }

                                if (matches) count++;
                            }

                            if (count <= finalAllowed - 1) { // <= because current task should be counted as well
                                // award XP
                                awardXPToUser(finalUserId, finalDifficulty, finalImportance);
                            } else {
                                Toast.makeText(this, "This completion does not award XP (limit reached)", Toast.LENGTH_SHORT).show();
                            }

                            loadTask();
                        })
                        .addOnFailureListener(e -> {
                            // On failure to count, do not award XP to avoid accidental double-awards
                            Toast.makeText(this, "Could not determine XP eligibility", Toast.LENGTH_SHORT).show();
                            loadTask();
                        });
            } else {
                // no limit applies, award XP
                awardXPToUser(userId, difficulty, importance);
                loadTask();
            }

        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show());
    }

    private void awardXPToUser(String userId, String difficulty, String importance) {
        // Build a minimal Task object with difficulty/importance for XP calculation
        Task t = new Task();
        try {
            if (difficulty != null) t.setDifficulty(Task.Difficulty.valueOf(difficulty));
        } catch (IllegalArgumentException ignored) {}
        try {
            if (importance != null) t.setImportance(Task.Importance.valueOf(importance));
        } catch (IllegalArgumentException ignored) {}

        // load user and run LevelUpProcessor
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) return;
                    User user = userDoc.toObject(User.class);
                    if (user == null) return;
                    new LevelUpProcessor().awardXPAndCheckLevel(user, t);
                    Toast.makeText(this, "XP awarded for completion", Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelTask() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Task.Status.CANCELLED.name());
        taskRef.update(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Task cancelled", Toast.LENGTH_SHORT).show();
            loadTask();
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to cancel task", Toast.LENGTH_SHORT).show());
    }

    private void togglePauseReactivate() {
        taskRef.get().addOnSuccessListener(doc -> {
            String status = doc.getString("status");
            if ("PAUSED".equals(status)) {
                taskRef.update("status", Task.Status.ACTIVE.name()).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task reactivated", Toast.LENGTH_SHORT).show();
                    loadTask();
                });
            } else {
                taskRef.update("status", Task.Status.PAUSED.name()).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task paused", Toast.LENGTH_SHORT).show();
                    loadTask();
                });
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show());
    }

    private void openEdit() {
        Intent i = new Intent(this, TaskCreateActivity.class);
        i.putExtra("taskId", taskId);
        startActivity(i);
        finish();
    }

    private void deleteTask() {
        String status = (String) currentData.get("status");
        if (Task.Status.COMPLETED.name().equals(status) || Task.Status.NOT_DONE.name().equals(status) || Task.Status.CANCELLED.name().equals(status)) {
            Toast.makeText(this, "Cannot delete completed/not done/cancelled tasks", Toast.LENGTH_SHORT).show();
            return;
        }

        String groupId = (String) currentData.get("recurrenceGroupId");
        long now = System.currentTimeMillis();

        if (groupId != null) {
            // delete future occurrences in the same group
            db.collection("tasks").whereEqualTo("recurrenceGroupId", groupId).whereGreaterThanOrEqualTo("executionTime", now)
                    .get().addOnSuccessListener(q -> {
                        for (DocumentSnapshot d : q.getDocuments()) {
                            d.getReference().delete();
                        }
                        Toast.makeText(this, "Future occurrences deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }).addOnFailureListener(e -> Toast.makeText(this, "Failed to delete occurrences", Toast.LENGTH_SHORT).show());
        } else {
            // one-time: delete this document
            taskRef.delete().addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                finish();
            }).addOnFailureListener(e -> Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show());
        }
    }
}
