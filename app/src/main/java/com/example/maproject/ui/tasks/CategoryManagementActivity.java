package com.example.maproject.ui.tasks;

import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.maproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.maproject.model.Task;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import yuku.ambilwarna.AmbilWarnaDialog;

public class CategoryManagementActivity extends AppCompatActivity {

    private static final String TAG = "CategoryManagement";
    private LinearLayout containerCategories;
    private ImageButton buttonAddCategory;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth;
    private int inlineSelectedColor = Color.parseColor("#FF4081");
    private boolean isEditing = false;
    private String editingDocId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebuggable) {
                auth.signInAnonymously().addOnSuccessListener(r -> {
                    if (auth.getCurrentUser() != null)
                        Toast.makeText(this, "[DEBUG] Signed in anonymously", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> Toast.makeText(this, "[DEBUG] Sign-in failed", Toast.LENGTH_SHORT).show());
            }
        }

        containerCategories = findViewById(R.id.container_categories);
        buttonAddCategory = findViewById(R.id.button_add_category);
        View categoryForm = findViewById(R.id.category_form);
        EditText inputCategoryName = findViewById(R.id.input_category_name);
        Button inputCategoryColor = findViewById(R.id.input_category_color);
        Button buttonSaveCategory = findViewById(R.id.button_save_category);
        Button buttonCancelCategory = findViewById(R.id.button_cancel_category);

        buttonAddCategory.setOnClickListener(v -> {
            buttonAddCategory.setEnabled(false);
            categoryForm.setVisibility(View.VISIBLE);
            inputCategoryName.setText("");
            inlineSelectedColor = Color.parseColor("#FF4081");
            inputCategoryColor.setBackgroundColor(inlineSelectedColor);
            isEditing = false;
            editingDocId = null;
            buttonSaveCategory.setText("Save");
            buttonAddCategory.postDelayed(() -> buttonAddCategory.setEnabled(true), 300);
        });

        inputCategoryColor.setOnClickListener(v -> {
            AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, inlineSelectedColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override public void onOk(AmbilWarnaDialog dialog, int color) {
                    inlineSelectedColor = color;
                    inputCategoryColor.setBackgroundColor(color);
                }
                @Override public void onCancel(AmbilWarnaDialog dialog) {}
            });
            dialog.show();
        });

        buttonSaveCategory.setOnClickListener(v -> {
            String name = inputCategoryName.getText().toString().trim();
            String color = String.format("#%06X", (0xFFFFFF & inlineSelectedColor));
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isEditing && editingDocId != null) updateCategory(editingDocId, name, color);
            else addCategory(name, color);
        });

        buttonCancelCategory.setOnClickListener(v -> resetForm());

        loadCategories();
    }

    private void addCategory(String name, String color) {
        db.collection("categories").whereEqualTo("color", color).get().addOnSuccessListener(q -> {
            if (!q.isEmpty()) {
                Toast.makeText(this, "Color already used", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("color", color);
            db.collection("categories").add(data).addOnSuccessListener(doc -> {
                Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show();
                resetForm();
                loadCategories();
            }).addOnFailureListener(e -> Toast.makeText(this, "Add failed", Toast.LENGTH_SHORT).show());
        });
    }

    private void updateCategory(String docId, String name, String color) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("color", color);
        db.collection("categories").document(docId).update(data).addOnSuccessListener(a -> {
            Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show();
            resetForm();
            loadCategories();
        }).addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    private void resetForm() {
        View categoryForm = findViewById(R.id.category_form);
        EditText inputCategoryName = findViewById(R.id.input_category_name);
        Button buttonSaveCategory = findViewById(R.id.button_save_category);
        isEditing = false;
        editingDocId = null;
        buttonSaveCategory.setText("Save");
        inputCategoryName.setText("");
        categoryForm.setVisibility(View.GONE);
    }

    private void loadCategories() {
        containerCategories.removeAllViews();
        db.collection("categories").get().addOnSuccessListener(querySnapshot -> {
            List<DocumentSnapshot> docs = querySnapshot.getDocuments();
            for (DocumentSnapshot doc : docs) {
                String name = doc.getString("name");
                String color = doc.getString("color");
                addCategoryRow(doc.getId(), name, color);
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show());
    }

    private void addCategoryRow(String docId, String name, String colorHex) {
        View rowView = getLayoutInflater().inflate(R.layout.item_category_card, containerCategories, false);
        TextView title = rowView.findViewById(R.id.text_category_name);
        View colorBox = rowView.findViewById(R.id.category_color_box);
        ImageButton editBtn = rowView.findViewById(R.id.button_edit_category);
        ImageButton deleteBtn = rowView.findViewById(R.id.button_delete_category);
        title.setText(name);
        try {
            colorBox.setBackgroundColor(Color.parseColor(colorHex));
        } catch (Exception e) {
            colorBox.setBackgroundColor(Color.GRAY);
        }
        editBtn.setOnClickListener(v -> {
            View categoryForm = findViewById(R.id.category_form);
            EditText inputCategoryName = findViewById(R.id.input_category_name);
            Button inputCategoryColor = findViewById(R.id.input_category_color);
            Button buttonSaveCategory = findViewById(R.id.button_save_category);
            categoryForm.setVisibility(View.VISIBLE);
            inputCategoryName.setText(name);
            inlineSelectedColor = Color.parseColor(colorHex);
            inputCategoryColor.setBackgroundColor(inlineSelectedColor);
            isEditing = true;
            editingDocId = docId;
            buttonSaveCategory.setText("Update");
        });
        deleteBtn.setOnClickListener(v -> attemptDeleteCategory(docId, name));
        containerCategories.addView(rowView);
    }

    private void attemptDeleteCategory(String docId, String categoryName) {
        if (categoryName == null) categoryName = "";
        db.collection("tasks").whereEqualTo("categoryId", docId)
                .whereEqualTo("status", Task.Status.ACTIVE.name()).get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) {
                        Toast.makeText(this, "Tasks reference this category; cannot delete.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("categories").document(docId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show();
                                loadCategories();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
                }).addOnFailureListener(e -> Toast.makeText(this, "Check failed", Toast.LENGTH_SHORT).show());
    }
}
