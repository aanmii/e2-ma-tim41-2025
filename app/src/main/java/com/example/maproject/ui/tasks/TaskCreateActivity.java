package com.example.maproject.ui.tasks;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.example.maproject.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TaskCreateActivity extends AppCompatActivity {

    private static final String TAG = "TaskCreateActivity";

    private Spinner categorySpinner;
    private Spinner frequencySpinner;
    private Spinner difficultySpinner;
    private Spinner importanceSpinner;
    private EditText titleInput;
    private EditText descriptionInput;
    private EditText executionTimeInput;
    private LinearLayout recurrenceContainer;
    private EditText recurrenceIntervalInput;
    private Spinner recurrenceUnitSpinner;
    private EditText recurrenceStartInput;
    private EditText recurrenceEndInput;
    private Button saveButton;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth;

    private String editingTaskId = null;
    private DocumentSnapshot editingDoc = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use direct layout reference
        setContentView(R.layout.activity_task_create);

        auth = FirebaseAuth.getInstance();

        // If no user is signed in and app is debuggable, sign in anonymously to allow testing writes
        if (auth.getCurrentUser() == null) {
            boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebuggable) {
                Log.i(TAG, "No user signed in and app is debuggable - attempting anonymous sign-in for testing");
                auth.signInAnonymously().addOnSuccessListener(result -> {
                    if (auth.getCurrentUser() != null) {
                        Log.i(TAG, "Anonymous sign-in successful: " + auth.getCurrentUser().getUid());
                        Toast.makeText(this, "[DEBUG] Signed in anonymously for testing", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Log.w(TAG, "Anonymous sign-in failed", e);
                    Toast.makeText(this, "[DEBUG] Anonymous sign-in failed: " + (e.getMessage() != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show();
                });
            }
        }

        // Use generated R.id references
        categorySpinner = findViewById(R.id.spinner_category);
        frequencySpinner = findViewById(R.id.spinner_frequency);
        difficultySpinner = findViewById(R.id.spinner_difficulty);
        importanceSpinner = findViewById(R.id.spinner_importance);
        titleInput = findViewById(R.id.input_title);
        descriptionInput = findViewById(R.id.input_description);
        executionTimeInput = findViewById(R.id.input_execution_time);
        recurrenceContainer = findViewById(R.id.container_recurrence);
        recurrenceIntervalInput = findViewById(R.id.input_recurrence_interval);
        recurrenceUnitSpinner = findViewById(R.id.spinner_recurrence_unit);
        recurrenceStartInput = findViewById(R.id.input_recurrence_start);
        recurrenceEndInput = findViewById(R.id.input_recurrence_end);
        saveButton = findViewById(R.id.button_save_task);

        setupSpinners();
        setupDateTimePickers();

        if (frequencySpinner != null) {
            frequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String freq = (String) parent.getItemAtPosition(position);
                    if ("Recurring".equals(freq)) {
                        if (recurrenceContainer != null) recurrenceContainer.setVisibility(View.VISIBLE);
                    } else {
                        if (recurrenceContainer != null) recurrenceContainer.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        if (saveButton != null) saveButton.setOnClickListener(v -> onSaveTask());

        loadCategoriesIntoSpinner();

        // Check for edit mode
        String taskId = getIntent().getStringExtra("taskId");
        if (!TextUtils.isEmpty(taskId)) {
            editingTaskId = taskId;
            loadTaskForEdit(taskId);
        }
    }

    private void setupSpinners() {
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"One-time", "Recurring"});
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (frequencySpinner != null) frequencySpinner.setAdapter(freqAdapter);

        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Very Easy", "Easy", "Hard", "Extremely Hard"});
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (difficultySpinner != null) difficultySpinner.setAdapter(difficultyAdapter);

        ArrayAdapter<String> importanceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Normal", "Important", "Extremely Important", "Special"});
        importanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (importanceSpinner != null) importanceSpinner.setAdapter(importanceAdapter);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"DAY", "WEEK"});
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (recurrenceUnitSpinner != null) recurrenceUnitSpinner.setAdapter(unitAdapter);
    }

    private void setupDateTimePickers() {
        if (executionTimeInput != null) executionTimeInput.setOnClickListener(v -> pickDateTime(executionTimeInput));
        if (recurrenceStartInput != null) recurrenceStartInput.setOnClickListener(v -> pickDateTime(recurrenceStartInput));
        if (recurrenceEndInput != null) recurrenceEndInput.setOnClickListener(v -> pickDateTime(recurrenceEndInput));
    }

    private void pickDateTime(EditText target) {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            TimePickerDialog tp = new TimePickerDialog(TaskCreateActivity.this, (timeView, hourOfDay, minute) -> {
                c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                c.set(Calendar.MINUTE, minute);
                if (target != null) target.setText(String.valueOf(c.getTimeInMillis()));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
            tp.show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    private void loadCategoriesIntoSpinner() {
        // Load categories from Firestore; show category name in spinner
        db.collection("categories").get().addOnSuccessListener(query -> {
            List<String> names = new java.util.ArrayList<>();
            names.add("Default");
            for (com.google.firebase.firestore.DocumentSnapshot ds : query.getDocuments()) {
                String name = ds.getString("name");
                if (!TextUtils.isEmpty(name)) names.add(name);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (categorySpinner != null) categorySpinner.setAdapter(adapter);

            // If editing, ensure we set the category selection after categories loaded
            if (editingDoc != null) {
                String catName = editingDoc.getString("categoryName");
                if (catName != null) {
                    int pos = names.indexOf(catName);
                    if (pos >= 0 && categorySpinner != null) categorySpinner.setSelection(pos);
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show());
    }

    private void loadTaskForEdit(String taskId) {
        DocumentReference ref = db.collection("tasks").document(taskId);
        ref.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            editingDoc = doc;

            String status = doc.getString("status");
            Long exec = doc.getLong("executionTime");
            Boolean isRecurring = doc.getBoolean("isRecurring");

            // Disallow editing if status is COMPLETED/NOT_DONE/CANCELLED
            if ("COMPLETED".equals(status) || "NOT_DONE".equals(status) || "CANCELLED".equals(status)) {
                Toast.makeText(this, "This task cannot be edited", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            long now = System.currentTimeMillis();
            long threeDaysMs = 3L * 24L * 60L * 60L * 1000L;
            if (exec != null && now > exec + threeDaysMs) {
                // expired more than 3 days
                Toast.makeText(this, "This task expired and cannot be edited", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // For recurring tasks only future instances can be modified: disallow if executionTime < now
            if (isRecurring != null && isRecurring && exec != null && exec < now) {
                Toast.makeText(this, "Past occurrence cannot be edited. Only future instances can be modified.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // populate fields
            if (titleInput != null) titleInput.setText(doc.getString("title"));
            if (descriptionInput != null) descriptionInput.setText(doc.getString("description"));
            if (exec != null && executionTimeInput != null) executionTimeInput.setText(String.valueOf(exec));

            String diff = doc.getString("difficulty");
            if (diff != null && difficultySpinner != null) {
                switch (diff) {
                    case "VERY_EASY": difficultySpinner.setSelection(0); break;
                    case "EASY": difficultySpinner.setSelection(1); break;
                    case "HARD": difficultySpinner.setSelection(2); break;
                    case "EXTREMELY_HARD": difficultySpinner.setSelection(3); break;
                }
            }

            String imp = doc.getString("importance");
            if (imp != null && importanceSpinner != null) {
                switch (imp) {
                    case "NORMAL": importanceSpinner.setSelection(0); break;
                    case "IMPORTANT": importanceSpinner.setSelection(1); break;
                    case "EXTREMELY_IMPORTANT": importanceSpinner.setSelection(2); break;
                    case "SPECIAL": importanceSpinner.setSelection(3); break;
                }
            }

            if (isRecurring != null && isRecurring) {
                if (frequencySpinner != null) frequencySpinner.setSelection(1);
                if (recurrenceContainer != null) recurrenceContainer.setVisibility(View.VISIBLE);
                Long interval = doc.getLong("recurrenceInterval");
                if (interval != null && recurrenceIntervalInput != null) recurrenceIntervalInput.setText(String.valueOf(interval));
                String unit = doc.getString("recurrenceUnit");
                if (unit != null && unit.equals("WEEK") && recurrenceUnitSpinner != null) recurrenceUnitSpinner.setSelection(1);
                Long start = doc.getLong("recurrenceStart");
                if (start != null && recurrenceStartInput != null) recurrenceStartInput.setText(String.valueOf(start));
                Long end = doc.getLong("recurrenceEnd");
                if (end != null && recurrenceEndInput != null) recurrenceEndInput.setText(String.valueOf(end));
            } else {
                if (frequencySpinner != null) frequencySpinner.setSelection(0);
                if (recurrenceContainer != null) recurrenceContainer.setVisibility(View.GONE);
            }

            // ensure category spinner set after categories loaded (we handle in loadCategories)
            loadCategoriesIntoSpinner();

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load task", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void onSaveTask() {
        // Prefer authenticated UID, but allow a debug fallback when running locally in debug builds
        String creatorUid = null;
        if (auth.getCurrentUser() != null) {
            creatorUid = auth.getCurrentUser().getUid();
        } else {
            boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebuggable) {
                creatorUid = "__DEBUG_USER__";
                Log.w(TAG, "No authenticated user - using debug fallback UID for testing: " + creatorUid);
                Toast.makeText(this, "[DEBUG] No user signed in - saving task with debug UID", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "You must be signed in to create a task", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "User not signed in, aborting task creation");
                return;
            }
        }

        if (titleInput == null) {
            Toast.makeText(this, "Internal error: title input missing", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "titleInput is null");
            return;
        }

        String title = titleInput.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            titleInput.setError("Title required");
            return;
        }

        String selectedCategory = null;
        try {
            Object sel = categorySpinner != null ? categorySpinner.getSelectedItem() : null;
            selectedCategory = sel != null ? sel.toString() : "Default";
        } catch (Exception e) {
            Log.w(TAG, "Could not read category spinner selection", e);
            selectedCategory = "Default";
        }

        String frequency = (String) (frequencySpinner != null ? frequencySpinner.getSelectedItem() : "One-time");
        String difficulty = (String) (difficultySpinner != null ? difficultySpinner.getSelectedItem() : "Very Easy");
        String importance = (String) (importanceSpinner != null ? importanceSpinner.getSelectedItem() : "Normal");
        String executionMsStr = executionTimeInput != null ? executionTimeInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(executionMsStr)) {
            // In debug builds, auto-fill execution time to help testing when user forgets to pick a date
            boolean isDebuggable = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebuggable) {
                long nowPlus1Min = System.currentTimeMillis() + 60_000L;
                executionMsStr = String.valueOf(nowPlus1Min);
                if (executionTimeInput != null) executionTimeInput.setText(executionMsStr);
                Log.i(TAG, "[DEBUG] Auto-filled execution time for testing: " + executionMsStr);
            } else {
                if (executionTimeInput != null) executionTimeInput.setError("Execution time required");
                return;
            }
        }

        long executionMs;
        try {
            executionMs = Long.parseLong(executionMsStr);
        } catch (NumberFormatException ex) {
            if (executionTimeInput != null) executionTimeInput.setError("Invalid date/time");
            Toast.makeText(this, "Invalid execution time format", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid execution time: " + executionMsStr, ex);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("description", descriptionInput != null ? descriptionInput.getText().toString().trim() : "");
        data.put("categoryName", selectedCategory);
        data.put("executionTime", executionMs);
        data.put("createdBy", creatorUid);

        // difficulty & importance mapping to enums
        Task.Difficulty diffEnum = null;
        switch (difficulty) {
            case "Very Easy": diffEnum = Task.Difficulty.VERY_EASY; break;
            case "Easy": diffEnum = Task.Difficulty.EASY; break;
            case "Hard": diffEnum = Task.Difficulty.HARD; break;
            case "Extremely Hard": diffEnum = Task.Difficulty.EXTREMELY_HARD; break;
        }
        if (diffEnum == null) diffEnum = Task.Difficulty.VERY_EASY;
        data.put("difficulty", diffEnum.name());

        Task.Importance impEnum = null;
        switch (importance) {
            case "Normal": impEnum = Task.Importance.NORMAL; break;
            case "Important": impEnum = Task.Importance.IMPORTANT; break;
            case "Extremely Important": impEnum = Task.Importance.EXTREMELY_IMPORTANT; break;
            case "Special": impEnum = Task.Importance.SPECIAL; break;
        }
        if (impEnum == null) impEnum = Task.Importance.NORMAL;
        data.put("importance", impEnum.name());

        // disable button while saving
        if (saveButton != null) saveButton.setEnabled(false);

        if ("Recurring".equals(frequency)) {
            String intervalStr = recurrenceIntervalInput != null ? recurrenceIntervalInput.getText().toString().trim() : "";
            if (TextUtils.isEmpty(intervalStr)) {
                if (recurrenceIntervalInput != null) recurrenceIntervalInput.setError("Interval required");
                if (saveButton != null) saveButton.setEnabled(true);
                return;
            }
            int interval;
            try {
                interval = Integer.parseInt(intervalStr);
            } catch (NumberFormatException nfe) {
                if (recurrenceIntervalInput != null) recurrenceIntervalInput.setError("Invalid interval");
                Toast.makeText(this, "Invalid recurrence interval", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Invalid recurrence interval: " + intervalStr, nfe);
                if (saveButton != null) saveButton.setEnabled(true);
                return;
            }
            data.put("isRecurring", true);
            data.put("recurrenceInterval", interval);
            data.put("recurrenceUnit", recurrenceUnitSpinner != null ? recurrenceUnitSpinner.getSelectedItem().toString() : "DAY");
            data.put("recurrenceStart", (recurrenceStartInput == null || TextUtils.isEmpty(recurrenceStartInput.getText().toString())) ? null : Long.parseLong(recurrenceStartInput.getText().toString()));
            data.put("recurrenceEnd", (recurrenceEndInput == null || TextUtils.isEmpty(recurrenceEndInput.getText().toString())) ? null : Long.parseLong(recurrenceEndInput.getText().toString()));

            if (editingTaskId == null) {
                // New recurring: generate a recurrenceGroupId
                String groupId = UUID.randomUUID().toString();
                data.put("recurrenceGroupId", groupId);
                Log.d(TAG, "Creating recurring task with data: " + data);
                DocumentReference newDoc = db.collection("tasks").document();
                newDoc.set(data).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Recurring task created", Toast.LENGTH_SHORT).show();
                    finish();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create recurring task", e);
                    Toast.makeText(this, "Failed to create recurring task: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    if (saveButton != null) saveButton.setEnabled(true);
                });
            } else {
                // existing recurring task: update only this doc (we ensured it's future instance)
                Log.d(TAG, "Updating recurring task " + editingTaskId + " with data: " + data);
                db.collection("tasks").document(editingTaskId).update(data).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show();
                    finish();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update recurring task", e);
                    Toast.makeText(this, "Failed to update task: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    if (saveButton != null) saveButton.setEnabled(true);
                });
            }

        } else {
            data.put("isRecurring", false);
            Log.d(TAG, "Creating task with data: " + data);
            if (editingTaskId == null) {
                db.collection("tasks").add(data).addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Task created (id=" + docRef.getId() + ")", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Task created: " + docRef.getId());
                    finish();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create task", e);
                    Toast.makeText(this, "Failed to create task: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    if (saveButton != null) saveButton.setEnabled(true);
                });
            } else {
                Log.d(TAG, "Updating task " + editingTaskId + " with data: " + data);
                db.collection("tasks").document(editingTaskId).update(data).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show();
                    finish();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update task", e);
                    Toast.makeText(this, "Failed to update task: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    if (saveButton != null) saveButton.setEnabled(true);
                });
            }
        }
    }
}
