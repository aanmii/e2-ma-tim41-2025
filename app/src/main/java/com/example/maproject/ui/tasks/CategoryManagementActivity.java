package com.example.maproject.ui.tasks;

import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.example.maproject.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryManagementActivity extends AppCompatActivity {

    private static final String TAG = "CategoryManagement";

    private LinearLayout containerCategories;
    private Button buttonAddCategory;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

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

        containerCategories = findViewById(R.id.container_categories);
        buttonAddCategory = findViewById(R.id.button_add_category);
        // Inline form views
        View categoryForm = findViewById(R.id.category_form);
        EditText inputCategoryName = findViewById(R.id.input_category_name);
        EditText inputCategoryColor = findViewById(R.id.input_category_color);
        Button buttonSaveCategory = findViewById(R.id.button_save_category);
        Button buttonCancelCategory = findViewById(R.id.button_cancel_category);

        // Show inline form when Add Category clicked
        if (buttonAddCategory != null) {
            buttonAddCategory.setOnClickListener(v -> {
                Log.d(TAG, "Add Category button clicked - showing inline form");
                // immediate visible feedback
                buttonAddCategory.setEnabled(false);
                buttonAddCategory.setText("Opening...");
                Toast.makeText(this, "Opening add category form...", Toast.LENGTH_SHORT).show();
                if (categoryForm != null) categoryForm.setVisibility(View.VISIBLE);
                if (inputCategoryName != null) inputCategoryName.setText("");
                if (inputCategoryColor != null) inputCategoryColor.setText("");
                // restore button state
                buttonAddCategory.postDelayed(() -> {
                    buttonAddCategory.setEnabled(true);
                    buttonAddCategory.setText("Add Category");
                }, 500);
            });
        }

        // Debug: confirm activity started and views initialized
        Log.i(TAG, "CategoryManagementActivity onCreate: buttonAddCategory=" + (buttonAddCategory != null) + ", categoryForm=" + (categoryForm != null));
        Toast.makeText(this, "Category management opened", Toast.LENGTH_SHORT).show();

        // Cancel button hides the form
        if (buttonCancelCategory != null) buttonCancelCategory.setOnClickListener(v -> {
            Log.d(TAG, "Inline category form cancelled");
            if (categoryForm != null) categoryForm.setVisibility(View.GONE);
            Toast.makeText(this, "Add category cancelled", Toast.LENGTH_SHORT).show();
        });

        // Save handler - reuse the same validation & Firestore logic as dialog
        if (buttonSaveCategory != null) buttonSaveCategory.setOnClickListener(v -> {
            String name = inputCategoryName != null ? inputCategoryName.getText().toString().trim() : "";
            String color = inputCategoryColor != null ? inputCategoryColor.getText().toString().trim() : "";
            Log.d(TAG, "Inline Save clicked - name='" + name + "' color='" + color + "'");
            // immediate UI feedback so user sees click registered
            buttonSaveCategory.setEnabled(false);
            buttonSaveCategory.setText("Saving...");
            Toast.makeText(this, "Saving category...", Toast.LENGTH_SHORT).show();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show();
                buttonSaveCategory.setEnabled(true);
                buttonSaveCategory.setText("Save");
                return;
            }
            if (TextUtils.isEmpty(color)) {
                Toast.makeText(this, "Color required", Toast.LENGTH_SHORT).show();
                buttonSaveCategory.setEnabled(true);
                buttonSaveCategory.setText("Save");
                return;
            }
            try {
                Color.parseColor(color);
            } catch (IllegalArgumentException ex) {
                Toast.makeText(this, "Invalid color", Toast.LENGTH_SHORT).show();
                buttonSaveCategory.setEnabled(true);
                buttonSaveCategory.setText("Save");
                return;
            }

            // enforce unique color and add
            boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            Log.d(TAG, "Checking uniqueness of color: " + color);
            db.collection("categories").whereEqualTo("color", color).get().addOnSuccessListener(q -> {
                if (!q.isEmpty()) {
                    Toast.makeText(this, "Color already used by another category", Toast.LENGTH_SHORT).show();
                    buttonSaveCategory.setEnabled(true);
                    buttonSaveCategory.setText("Save");
                    return;
                }
                Map<String, Object> data = new HashMap<>();
                data.put("name", name);
                data.put("color", color);
                Log.d(TAG, "Adding category to Firestore (inline form): " + data);
                db.collection("categories").add(data).addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Category added (inline): " + docRef.getId());
                    Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show();
                    if (categoryForm != null) categoryForm.setVisibility(View.GONE);
                    loadCategories();
                    // restore Save button state
                    buttonSaveCategory.setEnabled(true);
                    buttonSaveCategory.setText("Save");
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add category (inline)", e);
                    if (isDebuggable) {
                        String debugId = "debug-" + java.util.UUID.randomUUID().toString();
                        Log.w(TAG, "Firestore add failed, adding category locally (debug) id=" + debugId, e);
                        Toast.makeText(this, "[DEBUG] Added category locally (Firestore write failed)", Toast.LENGTH_LONG).show();
                        addCategoryRow(debugId, name, color);
                        if (categoryForm != null) categoryForm.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, "Failed to add category: " + (e.getMessage() != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show();
                    }
                    // restore Save button state
                    buttonSaveCategory.setEnabled(true);
                    buttonSaveCategory.setText("Save");
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to check category uniqueness (inline)", e);
                if (isDebuggable) {
                    String debugId = "debug-" + java.util.UUID.randomUUID().toString();
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("color", color);
                    Log.w(TAG, "Uniqueness check failed - adding category locally (debug): " + data, e);
                    addCategoryRow(debugId, name, color);
                    Toast.makeText(this, "[DEBUG] Added category locally (uniqueness check failed)", Toast.LENGTH_LONG).show();
                    if (categoryForm != null) categoryForm.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this, "Failed to add category (uniqueness check failed)", Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadCategories();
    }

    private void loadCategories() {
        containerCategories.removeAllViews();
        Log.d(TAG, "Loading categories from Firestore");
        db.collection("categories").get().addOnSuccessListener(querySnapshot -> {
            List<DocumentSnapshot> docs = querySnapshot.getDocuments();
            for (DocumentSnapshot doc : docs) {
                String name = doc.getString("name");
                String color = doc.getString("color");
                addCategoryRow(doc.getId(), name, color);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load categories", e);
            Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show();
        });
    }

    private void addCategoryRow(String docId, String name, String colorHex) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(8, 16, 8, 16);
        row.setGravity(Gravity.CENTER_VERTICAL);

        View colorSwatch = new View(this);
        LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(64, 64);
        swatchParams.setMarginEnd(16);
        colorSwatch.setLayoutParams(swatchParams);
        try {
            if (!TextUtils.isEmpty(colorHex)) colorSwatch.setBackgroundColor(Color.parseColor(colorHex));
        } catch (IllegalArgumentException ignored) {}

        TextView title = new TextView(this);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        title.setText(name != null ? name : "Unnamed");

        Button deleteBtn = new Button(this);
        deleteBtn.setText("Delete");
        deleteBtn.setOnClickListener(v -> attemptDeleteCategory(docId, name));

        row.addView(colorSwatch);
        row.addView(title);
        row.addView(deleteBtn);

        containerCategories.addView(row);
    }

    private void showAddCategoryDialog() {
        Log.d(TAG, "showAddCategoryDialog called");
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Add Category");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        final EditText inputName = new EditText(this);
        inputName.setHint("Category name");
        layout.addView(inputName);

        final EditText inputColor = new EditText(this);
        inputColor.setHint("Color hex (e.g. #FF4081)");
        layout.addView(inputColor);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            String color = inputColor.getText().toString().trim();
            Log.d(TAG, "Add dialog Add clicked - name='" + name + "' color='" + color + "'");
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(color)) {
                Toast.makeText(this, "Color required", Toast.LENGTH_SHORT).show();
                return;
            }
            // validate color
            try {
                Color.parseColor(color);
            } catch (IllegalArgumentException ex) {
                Toast.makeText(this, "Invalid color", Toast.LENGTH_SHORT).show();
                return;
            }

            // enforce unique color
            Log.d(TAG, "Checking uniqueness of color: " + color);
            db.collection("categories").whereEqualTo("color", color).get().addOnSuccessListener(q -> {
                if (!q.isEmpty()) {
                    Toast.makeText(this, "Color already used by another category", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("name", name);
                data.put("color", color);

                Log.d(TAG, "Adding category to Firestore: " + data);
                db.collection("categories").add(data).addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Category added: " + docRef.getId());
                    Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show();
                    loadCategories();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add category", e);
                    boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
                    if (isDebuggable) {
                        // fallback: add locally so tester sees immediate result
                        String debugId = "debug-" + java.util.UUID.randomUUID().toString();
                        Log.w(TAG, "Firestore add failed, adding category locally (debug) id=" + debugId, e);
                        Toast.makeText(this, "[DEBUG] Added category locally (Firestore write failed)", Toast.LENGTH_LONG).show();
                        addCategoryRow(debugId, name, color);
                    } else {
                        Toast.makeText(this, "Failed to add category: " + (e.getMessage() != null ? e.getMessage() : ""), Toast.LENGTH_LONG).show();
                    }
                });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to check category uniqueness", e);
                boolean isDebuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
                Log.w(TAG, "Uniqueness check failed", e);
                if (isDebuggable) {
                    // fallback: add locally
                    String debugId = "debug-" + java.util.UUID.randomUUID().toString();
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("color", color);
                    Log.w(TAG, "Uniqueness check failed - adding category locally (debug): " + data, e);
                    addCategoryRow(debugId, name, color);
                    Toast.makeText(this, "[DEBUG] Added category locally (uniqueness check failed)", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to add category (uniqueness check failed)", Toast.LENGTH_SHORT).show();
                }
             });

        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void attemptDeleteCategory(String docId, String categoryName) {
        if (categoryName == null) categoryName = "";
        // Check for active tasks with this category
        db.collection("tasks").whereEqualTo("categoryName", categoryName).whereEqualTo("status", Task.Status.ACTIVE.name()).get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) {
                        Toast.makeText(this, "Cannot delete category with active tasks", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // safe to delete
                    db.collection("categories").document(docId).delete().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show();
                        loadCategories();
                    }).addOnFailureListener(e -> Toast.makeText(this, "Failed to delete category", Toast.LENGTH_SHORT).show());
                }).addOnFailureListener(e -> Toast.makeText(this, "Failed to check tasks", Toast.LENGTH_SHORT).show());
    }
}
