package com.example.maproject.ui.tasks;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import android.content.Intent;
import com.example.maproject.ui.tasks.TaskCreateActivity;
import com.example.maproject.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TaskManagement extends AppCompatActivity {

    private static final String TAG = "TaskManagement";
    private GridLayout weekGrid;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth;
    private Calendar currentWeek;

    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat headerFormat = new SimpleDateFormat("EEE dd MMM", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_task_management);

            // ====== UI references ======
            auth = FirebaseAuth.getInstance();
            weekGrid = findViewById(R.id.week_calendar_grid);
            FrameLayout calendarFrame = findViewById(R.id.calendar_frame);
            ImageButton addTaskButton = findViewById(R.id.button_add_task);
            Button categoriesBtn = findViewById(R.id.button_manage_categories);
            TextView monthText = findViewById(R.id.text_current_month);
            ImageButton prev = findViewById(R.id.button_prev_week);
            ImageButton next = findViewById(R.id.button_next_week);


        addTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(TaskManagement.this, TaskCreateActivity.class);
            startActivity(intent);
            // optional animation
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

            // ====== Categories ======
            categoriesBtn.setOnClickListener(v ->
                    startActivity(new android.content.Intent(this, CategoryManagementActivity.class)));

            // ====== Calendar setup ======
            currentWeek = Calendar.getInstance();
            monthText.setText(monthFormat.format(currentWeek.getTime()));

            prev.setOnClickListener(v -> {
                currentWeek.add(Calendar.WEEK_OF_YEAR, -1);
                monthText.setText(monthFormat.format(currentWeek.getTime()));
                drawEmptyCalendar(currentWeek);
                loadWeeklyTasks();
            });

            next.setOnClickListener(v -> {
                currentWeek.add(Calendar.WEEK_OF_YEAR, 1);
                monthText.setText(monthFormat.format(currentWeek.getTime()));
                drawEmptyCalendar(currentWeek);
                loadWeeklyTasks();
            });

            drawEmptyCalendar(currentWeek);
            loadWeeklyTasks();
        }


        private void drawEmptyCalendar(Calendar weekStart) {
        // 1) reset UI
        weekGrid.removeAllViews();
        final LinearLayout hourLabelsContainer = findViewById(R.id.hour_labels_container);
        hourLabelsContainer.removeAllViews();

        // 2) start week on Monday
        Calendar d = (Calendar) weekStart.clone();
        d.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        // 3) add header row (row = 0)
        final int DAY_WIDTH   = 280;   // keep in sync with task overlay width
        final int HOUR_HEIGHT = 100;   // keep in sync with task overlay height
        for (int col = 0; col < 7; col++) {
            TextView header = new TextView(this);
            header.setText(headerFormat.format(d.getTime())); // e.g. Mon 20 Oct
            header.setGravity(Gravity.CENTER);
            header.setTextColor(Color.WHITE);
            header.setBackgroundColor(Color.parseColor("#FF80AB"));
            header.setPadding(8, 10, 8, 10);

            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.rowSpec = GridLayout.spec(0);
            p.columnSpec = GridLayout.spec(col);
            p.width = DAY_WIDTH;
            p.setGravity(Gravity.FILL_HORIZONTAL);
            weekGrid.addView(header, p);

            d.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 4) add the 24 hour rows (row = 1..24) + time labels
        for (int hour = 0; hour < 24; hour++) {
            // hour label (in the same vertical scroll as the grid)
            TextView lbl = new TextView(this);
            lbl.setText(String.format(Locale.getDefault(), "%02d:00", hour));
            lbl.setTextColor(Color.parseColor("#FF4081"));
            lbl.setTextSize(12f);
            lbl.setHeight(HOUR_HEIGHT);
            lbl.setGravity(Gravity.CENTER);
            hourLabelsContainer.addView(lbl);

            // 7 cells for the row
            for (int col = 0; col < 7; col++) {
                FrameLayout cell = new FrameLayout(this);
                cell.setBackgroundResource(R.drawable.calendar_cell_border); // your 1dp pink stroke

                GridLayout.LayoutParams p = new GridLayout.LayoutParams();
                p.rowSpec = GridLayout.spec(hour + 1); // +1 because header is row 0
                p.columnSpec = GridLayout.spec(col);
                p.width = DAY_WIDTH;
                p.height = HOUR_HEIGHT;
                p.setGravity(Gravity.FILL);
                weekGrid.addView(cell, p);
            }
        }

        // 5) AFTER layout is measured, insert a spacer above hour labels
        //    whose height equals the header row. This moves "00:00" to A2.
        weekGrid.post(() -> {
            if (weekGrid.getChildCount() > 0) {
                int headerHeight = weekGrid.getChildAt(0).getHeight(); // any header view
                // put a spacer at index 0 so it scrolls with the labels
                View spacer = new View(this);
                spacer.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, headerHeight));
                hourLabelsContainer.addView(spacer, 0);
            }
        });
    }

    private void renderMultiHourTask(DocumentSnapshot doc, Calendar start, Calendar end, String color) {
        long durationMs = end.getTimeInMillis() - start.getTimeInMillis();
        if (durationMs <= 0) durationMs = 1000 * 60 * 60; // at least 1h

        int totalHours = (int) Math.ceil(durationMs / (1000.0 * 60 * 60));
        int startHour = start.get(Calendar.HOUR_OF_DAY);

        // Normalize day index: Monday = 0, Sunday = 6
        int startDay = start.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
        if (startDay < 0) startDay += 7;

        // Only span multiple days if duration > 24h
        int totalDays = (durationMs > 24 * 60 * 60 * 1000L) ?
                (int) Math.ceil(durationMs / (24.0 * 60 * 60 * 1000L)) : 1;

        // === Create overlay ===
        FrameLayout taskOverlay = new FrameLayout(this);
        taskOverlay.setBackgroundColor(Color.parseColor(color));
        taskOverlay.setAlpha(0.85f);
        taskOverlay.setPadding(4, 4, 4, 4);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.rowSpec = GridLayout.spec(startHour + 1, Math.max(1, totalHours));
        lp.columnSpec = GridLayout.spec(startDay, totalDays);
        lp.width = 280 * totalDays;
        lp.height = 100 * totalHours;
        lp.setGravity(Gravity.FILL);
        lp.setMargins(2, 2, 2, 2); // prevent visual overlap

        taskOverlay.setLayoutParams(lp);

        // === Add label ===
        TextView label = new TextView(this);
        label.setText(doc.getString("title"));
        label.setTextColor(Color.WHITE);
        label.setPadding(8, 8, 8, 8);
        label.setTextSize(12f);
        label.setGravity(Gravity.CENTER_VERTICAL);
        taskOverlay.addView(label);

        // === Add to grid ===
        weekGrid.addView(taskOverlay);

        // === Request layout refresh ===
        weekGrid.post(weekGrid::requestLayout);
    }

    private void loadWeeklyTasks() {
        Calendar weekStart = (Calendar) currentWeek.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 7);

        weekGrid.post(() -> {
            if (weekGrid.getChildCount() > 7)
                weekGrid.removeViews(7, weekGrid.getChildCount() - 7);
        });

        long startMs = weekStart.getTimeInMillis();
        long endMs   = weekEnd.getTimeInMillis() - 1; // include last ms of the week

        db.collection("tasks")
                .whereEqualTo("isRecurring", false)
                .whereGreaterThanOrEqualTo("startDate", startMs)
                .whereLessThanOrEqualTo("startDate", endMs)
                .get()
                .addOnSuccessListener(query -> {
                    Log.d(TAG, "🗓 One-time tasks found: " + query.size());
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Log.d(TAG, "▶ one-time: " + doc.getString("title") + " at " + new java.util.Date(doc.getLong("startDate")));
                        renderTask(doc);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to load one-time tasks", e));


        // === 2️⃣ Recurring tasks ===
        db.collection("tasks")
                .whereEqualTo("isRecurring", true)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Long recurStart = doc.getLong("recurrenceStart");
                        Long recurEnd = doc.getLong("recurrenceEnd");
                        Long interval = doc.getLong("recurrenceInterval");
                        String unit = doc.getString("recurrenceUnit");

                        if (recurStart == null || interval == null || unit == null) continue;

                        Calendar recur = Calendar.getInstance();
                        recur.setTimeInMillis(recurStart);

                        while (recur.getTimeInMillis() < weekEnd.getTimeInMillis()) {
                            if (recur.getTimeInMillis() >= weekStart.getTimeInMillis()
                                    && recur.getTimeInMillis() < weekEnd.getTimeInMillis()) {

                                Calendar end = (Calendar) recur.clone();
                                long durationMs = getDurationMs(doc);
                                end.setTimeInMillis(recur.getTimeInMillis() + durationMs);

                                renderWithColor(doc, recur, end);
                            }

                            // Move to next recurrence
                            switch (unit) {
                                case "DAY":   recur.add(Calendar.DAY_OF_YEAR, interval.intValue()); break;
                                case "WEEK":  recur.add(Calendar.WEEK_OF_YEAR, interval.intValue()); break;
                                case "MONTH": recur.add(Calendar.MONTH, interval.intValue()); break;
                            }

                            // ✅ also break if passed end-of-recurrence
                            if (recurEnd != null && recurEnd > 0 && recur.getTimeInMillis() > recurEnd) break;
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load recurring tasks", e));
    }

    private void renderTask(DocumentSnapshot doc) {
        Long startMs = doc.getLong("startDate");
        if (startMs == null) startMs = doc.getLong("executionTime");
        if (startMs == null) return;

        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(startMs);

        Calendar end = Calendar.getInstance();
        long durationMs = getDurationMs(doc);
        end.setTimeInMillis(startMs + durationMs);

        renderWithColor(doc, start, end);
    }

    private long getDurationMs(DocumentSnapshot doc) {
        Long durationInterval = doc.getLong("durationInterval");
        String durationUnit = doc.getString("durationUnit");
        long durationMs = 60 * 60 * 1000; // default 1h
        if (durationInterval != null && durationUnit != null) {
            switch (durationUnit) {
                case "HOUR":
                    durationMs = durationInterval * 60 * 60 * 1000L; break;
                case "DAY":
                    durationMs = durationInterval * 24 * 60 * 60 * 1000L; break;
                case "WEEK":
                    durationMs = durationInterval * 7 * 24 * 60 * 60 * 1000L; break;
            }
        }
        return durationMs;
    }

    private void renderWithColor(DocumentSnapshot doc, Calendar start, Calendar end) {
        String categoryId = doc.getString("categoryId");
        final String defaultColor = "#FF80AB";
        if (categoryId != null) {
            db.collection("categories").document(categoryId)
                    .get()
                    .addOnSuccessListener(cat -> {
                        String c = cat.getString("color");
                        String resolved = (c != null) ? c : defaultColor;
                        renderMultiHourTask(doc, start, end, resolved);
                    })
                    .addOnFailureListener(e -> renderMultiHourTask(doc, start, end, defaultColor));
        } else {
            renderMultiHourTask(doc, start, end, defaultColor);
        }
    }

    private Button makeStatusButton(String emoji, String status, String taskId) {
        Button btn = new Button(this);
        btn.setText(emoji);
        btn.setTextSize(18);
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setOnClickListener(v -> updateTaskStatus(taskId, status));
        return btn;
    }

    private void updateTaskStatus(String taskId, String newStatus) {
        db.collection("tasks").document(taskId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Task " + newStatus, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        drawEmptyCalendar(currentWeek);
        loadWeeklyTasks(); // refresh tasks in case new ones were added
    }
}
