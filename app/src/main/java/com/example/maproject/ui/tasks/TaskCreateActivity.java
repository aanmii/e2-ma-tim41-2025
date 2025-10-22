package com.example.maproject.ui.tasks;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.maproject.R;
import com.example.maproject.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TaskCreateActivity extends AppCompatActivity {

    private static final String TAG = "TaskCreateActivity";

    private Spinner categorySpinner, frequencySpinner, difficultySpinner, importanceSpinner;
    private EditText titleInput, descriptionInput, executionTimeInput, durationIntervalInput;
    private Spinner durationUnitSpinner;
    private LinearLayout recurrenceContainer;
    private EditText recurrenceIntervalInput, recurrenceStartInput, recurrenceEndInput;
    private Spinner recurrenceUnitSpinner;
    private Button saveButton;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth;
    private String editingTaskId = null;
    private DocumentSnapshot editingDoc = null;

    private final ArrayList<String> categoryNames = new ArrayList<>();
    private final ArrayList<String> categoryIds = new ArrayList<>();

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_create);

        auth = FirebaseAuth.getInstance();
        ensureUser();

        // ==== Bind views ====
        categorySpinner = findViewById(R.id.spinner_category);
        frequencySpinner = findViewById(R.id.spinner_frequency);
        difficultySpinner = findViewById(R.id.spinner_difficulty);
        importanceSpinner = findViewById(R.id.spinner_importance);
        titleInput = findViewById(R.id.input_title);
        descriptionInput = findViewById(R.id.input_description);
        executionTimeInput = findViewById(R.id.input_execution_time);
        durationIntervalInput = findViewById(R.id.input_duration_interval);
        durationUnitSpinner = findViewById(R.id.spinner_duration_unit);
        recurrenceContainer = findViewById(R.id.container_recurrence);
        recurrenceIntervalInput = findViewById(R.id.input_recurrence_interval);
        recurrenceUnitSpinner = findViewById(R.id.spinner_recurrence_unit);
        recurrenceStartInput = findViewById(R.id.input_recurrence_start);
        recurrenceEndInput = findViewById(R.id.input_recurrence_end);
        saveButton = findViewById(R.id.button_save_task);

        setupSpinners();
        setupDateTimePickers();
        loadCategoriesIntoSpinner();

        frequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                recurrenceContainer.setVisibility(
                        "Recurring".equals(parent.getItemAtPosition(pos)) ? View.VISIBLE : View.GONE
                );
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        saveButton.setOnClickListener(v -> onSaveTask());

        // Edit existing
        String taskId = getIntent().getStringExtra("taskId");
        if (!TextUtils.isEmpty(taskId)) {
            editingTaskId = taskId;
            loadTaskForEdit(taskId);
        }
    }

    private void ensureUser() {
        if (auth.getCurrentUser() == null) {
            boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebuggable) {
                Log.i(TAG, "No user signed in - signing in anonymously for debug");
                auth.signInAnonymously()
                        .addOnSuccessListener(r -> Toast.makeText(this, "[DEBUG] Signed in anonymously", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void setupSpinners() {
        frequencySpinner.setAdapter(makeAdapter("One-time", "Recurring"));
        difficultySpinner.setAdapter(makeAdapter("Very Easy", "Easy", "Hard", "Extremely Hard"));
        importanceSpinner.setAdapter(makeAdapter("Normal", "Important", "Extremely Important", "Special"));
        durationUnitSpinner.setAdapter(makeAdapter("HOUR", "DAY", "WEEK", "MONTH"));
        recurrenceUnitSpinner.setAdapter(makeAdapter("HOUR", "DAY", "WEEK", "MONTH"));
    }

    private ArrayAdapter<String> makeAdapter(String... items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void setupDateTimePickers() {
        executionTimeInput.setOnClickListener(v -> pickDateTime(executionTimeInput));
        recurrenceStartInput.setOnClickListener(v -> pickDateTime(recurrenceStartInput));
        recurrenceEndInput.setOnClickListener(v -> pickDateTime(recurrenceEndInput));
    }

    private void pickDateTime(EditText target) {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this, (v, y, m, d) -> {
            c.set(y, m, d);
            new TimePickerDialog(this, (t, h, min) -> {
                c.set(Calendar.HOUR_OF_DAY, h);
                c.set(Calendar.MINUTE, min);
                target.setText(sdf.format(c.getTime())); // human-readable
                target.setTag(c.getTimeInMillis());      // store real millis
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    private void loadCategoriesIntoSpinner() {
        db.collection("categories").get().addOnSuccessListener(q -> {
            categoryNames.clear();
            categoryIds.clear();
            categoryNames.add("Default");
            categoryIds.add(null);
            for (DocumentSnapshot ds : q.getDocuments()) {
                String name = ds.getString("name");
                if (!TextUtils.isEmpty(name)) {
                    categoryNames.add(name);
                    categoryIds.add(ds.getId());
                }
            }
            categorySpinner.setAdapter(makeAdapter(categoryNames.toArray(new String[0])));
        });
    }

    private void loadTaskForEdit(String taskId) {
        db.collection("tasks").document(taskId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) { Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show(); finish(); return; }
            editingDoc = doc;

            titleInput.setText(doc.getString("title"));
            descriptionInput.setText(doc.getString("description"));

            Long start = doc.getLong("startDate");
            if (start != null) {
                executionTimeInput.setText(sdf.format(new Date(start)));
                executionTimeInput.setTag(start);
            }

            Long durInt = doc.getLong("durationInterval");
            if (durInt != null) durationIntervalInput.setText(String.valueOf(durInt));
            String durUnit = doc.getString("durationUnit");
            setSpinnerSelection(durationUnitSpinner, durUnit);

            Boolean isRec = doc.getBoolean("isRecurring");
            if (Boolean.TRUE.equals(isRec)) {
                frequencySpinner.setSelection(1);
                recurrenceContainer.setVisibility(View.VISIBLE);
                setSpinnerSelection(recurrenceUnitSpinner, doc.getString("recurrenceUnit"));
                Long recStart = doc.getLong("recurrenceStart");
                if (recStart != null) {
                    recurrenceStartInput.setText(sdf.format(new Date(recStart)));
                    recurrenceStartInput.setTag(recStart);
                }
                long recEnd = extractMillis(recurrenceEndInput);
                if (recEnd <= 0) recEnd = recStart + 90L * 24 * 60 * 60 * 1000;
            }
        });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equalsIgnoreCase(spinner.getItemAtPosition(i).toString())) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void onSaveTask() {
        try {
            String uid = (auth.getCurrentUser() != null)
                    ? auth.getCurrentUser().getUid()
                    : "__DEBUG_USER__";

            String title = titleInput.getText().toString().trim();
            if (TextUtils.isEmpty(title)) {
                titleInput.setError("Title required");
                return;
            }

            String catId = categoryIds.get(categorySpinner.getSelectedItemPosition());
            String catName = categoryNames.get(categorySpinner.getSelectedItemPosition());
            boolean isRecurring = "Recurring".equals(frequencySpinner.getSelectedItem());

            // Difficulty & importance
            Task.Difficulty diffEnum = Task.Difficulty.fromString(
                    difficultySpinner.getSelectedItem().toString().replace(" ", "_"));
            Task.Importance impEnum = Task.Importance.fromString(
                    importanceSpinner.getSelectedItem().toString().replace(" ", "_"));

            // Build map for Firestore
            Map<String, Object> data = new HashMap<>();
            data.put("userId", uid);
            data.put("title", title);
            data.put("description", descriptionInput.getText().toString().trim());
            data.put("categoryId", catId);
            data.put("categoryName", catName);
            data.put("difficulty", diffEnum.name());
            data.put("importance", impEnum.name());
            data.put("status", Task.Status.ACTIVE.name());
            data.put("createdTime", System.currentTimeMillis());
            data.put("isRecurring", isRecurring);

            long startMillis = extractMillis(executionTimeInput);
            data.put("startDate", startMillis);

            int durInt = parseIntSafe(durationIntervalInput.getText().toString(), 1);
            String durUnit = durationUnitSpinner.getSelectedItem().toString();
            data.put("durationInterval", durInt);
            data.put("durationUnit", durUnit);

            if (isRecurring) {
                int recInt = parseIntSafe(recurrenceIntervalInput.getText().toString(), 1);
                String recUnit = recurrenceUnitSpinner.getSelectedItem().toString();
                long recStart = extractMillis(recurrenceStartInput);
                long recEnd = extractMillis(recurrenceEndInput);
                data.put("recurrenceInterval", recInt);
                data.put("recurrenceUnit", recUnit);
                data.put("recurrenceStart", recStart);
                data.put("recurrenceEnd", recEnd);
            }

            saveButton.setEnabled(false);

            if (editingTaskId == null) {
                DocumentReference newDoc = db.collection("tasks").document();
                data.put("taskId", newDoc.getId());
                newDoc.set(data)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "✅ Task saved: " + title);
                            if (catId != null)
                                db.collection("categories").document(catId)
                                        .update("taskIds", FieldValue.arrayUnion(newDoc.getId()));
                            Toast.makeText(this, "Task saved", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "❌ Failed to save task", e);
                            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            saveButton.setEnabled(true);
                        });
            } else {
                db.collection("tasks").document(editingTaskId)
                        .update(data)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "❌ Failed to update task", e);
                            Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            saveButton.setEnabled(true);
                        });
            }

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error saving task", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            saveButton.setEnabled(true);
        }
    }

    private int parseIntSafe(String text, int def) {
        try { return Integer.parseInt(text.trim()); } catch (Exception e) { return def; }
    }

    private long extractMillis(EditText field) {
        Object tag = field.getTag();
        if (tag instanceof Long) return (Long) tag;
        try {
            return sdf.parse(field.getText().toString()).getTime();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}
