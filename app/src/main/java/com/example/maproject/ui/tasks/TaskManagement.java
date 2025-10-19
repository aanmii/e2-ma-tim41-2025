package com.example.maproject.ui.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;

public class TaskManagement extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_management);

        Button createBtn = findViewById(R.id.button_create_task);
        Button listBtn = findViewById(R.id.button_view_tasks);
        Button categoriesBtn = findViewById(R.id.button_manage_categories);

        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TaskManagement.this, TaskCreateActivity.class));
            }
        });

        listBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TaskManagement.this, TaskListActivity.class));
            }
        });

        categoriesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TaskManagement.this, CategoryManagementActivity.class));
            }
        });
    }
}
