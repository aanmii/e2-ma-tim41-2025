package com.example.maproject.ui.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private TextView activeDaysTextView, longestStreakTextView, specialMissionsTextView;
    private PieChart taskStatusPieChart;
    private BarChart categoryBarChart;
    private LineChart xpLineChart;
    private Button backButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        initViews();
        loadStatistics();
        setupBackButton();
    }

    private void initViews() {
        activeDaysTextView = findViewById(R.id.activeDaysTextView);
        longestStreakTextView = findViewById(R.id.longestStreakTextView);
        specialMissionsTextView = findViewById(R.id.specialMissionsTextView);
        taskStatusPieChart = findViewById(R.id.taskStatusPieChart);
        categoryBarChart = findViewById(R.id.categoryBarChart);
        xpLineChart = findViewById(R.id.xpLineChart);
        backButton = findViewById(R.id.backButton);
    }

    private void loadStatistics() {
        db.collection("statistics").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Učitaj brojke
                        Long activeDays = document.getLong("activeDays");
                        Long longestStreak = document.getLong("longestStreak");
                        Long missionsStarted = document.getLong("specialMissionsStarted");
                        Long missionsCompleted = document.getLong("specialMissionsCompleted");

                        activeDaysTextView.setText("Dana aktivnosti: " + (activeDays != null ? activeDays : 0));
                        longestStreakTextView.setText("Najduži niz: " + (longestStreak != null ? longestStreak : 0) + " dana");
                        specialMissionsTextView.setText("Misije: " + (missionsCompleted != null ? missionsCompleted : 0) + "/" + (missionsStarted != null ? missionsStarted : 0));

                        // Kreiraj grafikone
                        setupTaskStatusPieChart(document);
                        setupCategoryBarChart(document);
                        setupXPLineChart();
                    } else {
                        // Nema podataka - prikaži prazne grafikone
                        activeDaysTextView.setText("Dana aktivnosti: 0");
                        longestStreakTextView.setText("Najduži niz: 0 dana");
                        specialMissionsTextView.setText("Misije: 0/0");
                    }
                });
    }

    private void setupTaskStatusPieChart(com.google.firebase.firestore.DocumentSnapshot document) {
        Long completed = document.getLong("totalTasksCompleted");
        Long notDone = document.getLong("totalTasksNotDone");
        Long cancelled = document.getLong("totalTasksCancelled");

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(completed != null ? completed : 0, "Završeno"));
        entries.add(new PieEntry(notDone != null ? notDone : 0, "Neurađeno"));
        entries.add(new PieEntry(cancelled != null ? cancelled : 0, "Otkazano"));

        PieDataSet dataSet = new PieDataSet(entries, "Status zadataka");
        dataSet.setColors(new int[]{Color.rgb(76, 175, 80), Color.rgb(244, 67, 54), Color.rgb(255, 152, 0)});
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        taskStatusPieChart.setData(data);
        taskStatusPieChart.getDescription().setEnabled(false);
        taskStatusPieChart.setDrawEntryLabels(true);
        taskStatusPieChart.animateY(1000);
        taskStatusPieChart.invalidate();
    }

    private void setupCategoryBarChart(com.google.firebase.firestore.DocumentSnapshot document) {
        Map<String, Object> tasksPerCategory = (Map<String, Object>) document.get("tasksPerCategory");

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        if (tasksPerCategory != null && !tasksPerCategory.isEmpty()) {
            for (Map.Entry<String, Object> entry : tasksPerCategory.entrySet()) {
                labels.add(entry.getKey());
                entries.add(new BarEntry(index++, ((Long) entry.getValue()).floatValue()));
            }
        } else {
            // Dummy data ako nema zadataka
            labels.add("Nema podataka");
            entries.add(new BarEntry(0, 0));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Zadaci po kategoriji");
        dataSet.setColors(new int[]{
                Color.rgb(255, 107, 107),
                Color.rgb(78, 205, 196),
                Color.rgb(255, 217, 61),
                Color.rgb(149, 225, 211),
                Color.rgb(168, 230, 207)
        });
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        categoryBarChart.setData(data);
        categoryBarChart.getDescription().setEnabled(false);
        categoryBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        categoryBarChart.animateY(1000);
        categoryBarChart.invalidate();
    }

    private void setupXPLineChart() {
        // Za sada dummy data - kasnije ćeš učitavati iz baze
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 10));
        entries.add(new Entry(1, 25));
        entries.add(new Entry(2, 40));
        entries.add(new Entry(3, 35));
        entries.add(new Entry(4, 60));
        entries.add(new Entry(5, 80));
        entries.add(new Entry(6, 100));

        LineDataSet dataSet = new LineDataSet(entries, "XP u poslednjih 7 dana");
        dataSet.setColor(Color.rgb(33, 150, 243));
        dataSet.setCircleColor(Color.rgb(33, 150, 243));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);

        LineData data = new LineData(dataSet);
        xpLineChart.setData(data);
        xpLineChart.getDescription().setEnabled(false);
        xpLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        xpLineChart.animateX(1000);
        xpLineChart.invalidate();
    }

    private void setupBackButton() {
        backButton.setOnClickListener(v -> finish());
    }
}