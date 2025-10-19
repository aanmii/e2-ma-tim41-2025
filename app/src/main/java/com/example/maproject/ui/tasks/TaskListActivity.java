package com.example.maproject.ui.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskListActivity extends AppCompatActivity {

    private Spinner filterSpinner;
    private LinearLayout containerTasks;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SimpleDateFormat displayDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        filterSpinner = findViewById(R.id.spinner_filter);
        containerTasks = findViewById(R.id.container_tasks);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"All", "One-time", "Recurring"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadTasks();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        loadTasks();
    }

    private void loadTasks() {
        containerTasks.removeAllViews();
        String filter = (String) filterSpinner.getSelectedItem();

        db.collection("tasks").get().addOnSuccessListener(querySnapshot -> {
            List<DocumentSnapshot> docs = querySnapshot.getDocuments();
            for (DocumentSnapshot doc : docs) {
                addTaskRow(doc);
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load tasks", Toast.LENGTH_SHORT).show());
    }

    private void addTaskRow(DocumentSnapshot doc) {
        String id = doc.getId();
        String title = doc.getString("title");
        Long execTime = doc.getLong("executionTime");
        String status = doc.getString("status");
        Boolean isRecurring = doc.getBoolean("isRecurring");

        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(8, 16, 8, 16);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(this);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        titleView.setText(title != null ? title : "(no title)");

        TextView timeView = new TextView(this);
        timeView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        timeView.setText(execTime != null ? displayDate.format(new Date(execTime)) : "");
        timeView.setPadding(8, 0, 8, 0);

        TextView statusView = new TextView(this);
        statusView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        statusView.setText(status != null ? status : "");
        statusView.setPadding(8, 0, 8, 0);

        Button openBtn = new Button(this);
        openBtn.setText("Open");
        openBtn.setOnClickListener(v -> {
            Intent i = new Intent(TaskListActivity.this, com.example.maproject.ui.tasks.TaskDetailActivity.class);
            i.putExtra("taskId", id);
            startActivity(i);
        });

        row.addView(titleView);
        row.addView(timeView);
        row.addView(statusView);
        row.addView(openBtn);

        containerTasks.addView(row);
    }
}
