package com.example.maproject.model;

import java.util.ArrayList;
import java.util.List;

public class Category {
    private String categoryId;
    private String name;
    private List<String> taskIds;

    public Category() {
        this.taskIds = new ArrayList<>();
    }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getTaskIds() { return taskIds; }
    public void setTaskIds(List<String> taskIds) { this.taskIds = taskIds; }
}

