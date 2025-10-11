package com.example.maproject.ui.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
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

    private TextView activeDaysTextView, longestStreakTextView, specialMissionsTextView, avgDifficultyTextView;
    private PieChart taskStatusPieChart;
    private BarChart categoryBarChart;
    private LineChart xpLineChart, difficultyLineChart;
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

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();

            initViews();
            loadStatistics();
            setupBackButton();
        } else {
            Toast.makeText(this, "Greška: Korisnik nije prijavljen.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        activeDaysTextView = findViewById(R.id.activeDaysTextView);
        longestStreakTextView = findViewById(R.id.longestStreakTextView);
        specialMissionsTextView = findViewById(R.id.specialMissionsTextView);
        avgDifficultyTextView = findViewById(R.id.avgDifficultyTextView);
        taskStatusPieChart = findViewById(R.id.taskStatusPieChart);
        categoryBarChart = findViewById(R.id.categoryBarChart);
        xpLineChart = findViewById(R.id.xpLineChart);
        difficultyLineChart = findViewById(R.id.difficultyLineChart);
        backButton = findViewById(R.id.backButton);
    }

    private void loadStatistics() {
        db.collection("statistics").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Long activeDays = document.getLong("activeDays");
                        Long longestStreak = document.getLong("longestStreak");
                        Long missionsStarted = document.getLong("specialMissionsStarted");
                        Long missionsCompleted = document.getLong("specialMissionsCompleted");

                        activeDaysTextView.setText(String.valueOf(activeDays != null ? activeDays : 0));
                        longestStreakTextView.setText(String.valueOf(longestStreak != null ? longestStreak : 0));
                        specialMissionsTextView.setText((missionsCompleted != null ? missionsCompleted : 0) + "/" + (missionsStarted != null ? missionsStarted : 0));
                        avgDifficultyTextView.setText("1.5");

                        setupTaskStatusPieChart(document);
                        setupCategoryBarChart(document);
                        setupDifficultyLineChart();
                        setupXPLineChart();
                    } else {
                        activeDaysTextView.setText("0");
                        longestStreakTextView.setText("0");
                        specialMissionsTextView.setText("0/0");
                        avgDifficultyTextView.setText("0.0");
                        setupEmptyCharts();
                    }
                });
    }

    private void setupTaskStatusPieChart(com.google.firebase.firestore.DocumentSnapshot document) {
        Long completed = document != null ? document.getLong("totalTasksCompleted") : 0L;
        Long notDone = document != null ? document.getLong("totalTasksNotDone") : 0L;
        Long cancelled = document != null ? document.getLong("totalTasksCancelled") : 0L;

        List<PieEntry> entries = new ArrayList<>();
        if (completed != null && completed > 0) entries.add(new PieEntry(completed, "Završeno"));
        if (notDone != null && notDone > 0) entries.add(new PieEntry(notDone, "Neurađeno"));
        if (cancelled != null && cancelled > 0) entries.add(new PieEntry(cancelled, "Otkazano"));

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "Nema podataka"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        dataSet.setColors(new int[]{
                Color.parseColor("#B5EAD7"),
                Color.parseColor("#FF9AA2"),
                Color.parseColor("#FFD4A3")
        });

        dataSet.setValueTextSize(16f);
        dataSet.setValueTextColor(Color.parseColor("#2C3E50"));
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        taskStatusPieChart.setData(data);
        taskStatusPieChart.setHoleRadius(58f);
        taskStatusPieChart.setTransparentCircleRadius(61f);
        taskStatusPieChart.setDrawEntryLabels(false);
        taskStatusPieChart.getDescription().setEnabled(false);

        Legend legend = taskStatusPieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(14f);

        taskStatusPieChart.animateY(1200);
        taskStatusPieChart.invalidate();
    }

    private void setupCategoryBarChart(com.google.firebase.firestore.DocumentSnapshot document) {
        Map<String, Object> tasksPerCategory = document != null ? (Map<String, Object>) document.get("tasksPerCategory") : null;

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        if (tasksPerCategory != null && !tasksPerCategory.isEmpty()) {
            for (Map.Entry<String, Object> entry : tasksPerCategory.entrySet()) {
                labels.add(entry.getKey());
                entries.add(new BarEntry(index++, ((Long) entry.getValue()).floatValue()));
            }
        } else {
            labels.add("");
            entries.add(new BarEntry(0, 5));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");

        dataSet.setColors(new int[]{
                Color.parseColor("#A8DAFF"),
                Color.parseColor("#FFB3D9"),
                Color.parseColor("#D4BAFF"),
                Color.parseColor("#B3FFD9"),
                Color.parseColor("#FFD4BA")
        });

        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.parseColor("#2C3E50"));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.8f);

        categoryBarChart.setData(data);
        categoryBarChart.getDescription().setEnabled(false);
        categoryBarChart.setFitBars(true);

        XAxis xAxis = categoryBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        categoryBarChart.getAxisLeft().setDrawGridLines(false);
        categoryBarChart.getAxisRight().setEnabled(false);
        categoryBarChart.getLegend().setEnabled(false);
        categoryBarChart.animateY(1200);
        categoryBarChart.invalidate();
    }

    private void setupDifficultyLineChart() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 1.2f));
        entries.add(new Entry(1, 1.8f));
        entries.add(new Entry(2, 2.1f));
        entries.add(new Entry(3, 1.5f));
        entries.add(new Entry(4, 2.3f));
        entries.add(new Entry(5, 2.0f));
        entries.add(new Entry(6, 1.9f));

        LineDataSet dataSet = new LineDataSet(entries, "Prosečna težina");

        dataSet.setColor(Color.parseColor("#D4BAFF"));
        dataSet.setCircleColor(Color.parseColor("#D4BAFF"));
        dataSet.setCircleHoleColor(Color.parseColor("#D4BAFF"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setCircleHoleRadius(3f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#2C3E50"));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E8BAFF"));
        dataSet.setFillAlpha(80);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(dataSet);
        difficultyLineChart.setData(data);
        difficultyLineChart.getDescription().setEnabled(false);

        XAxis xAxis = difficultyLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        difficultyLineChart.getAxisLeft().setDrawGridLines(false);
        difficultyLineChart.getAxisRight().setEnabled(false);
        difficultyLineChart.getLegend().setEnabled(false);
        difficultyLineChart.animateX(1200);
        difficultyLineChart.invalidate();
    }

    private void setupXPLineChart() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 15));
        entries.add(new Entry(1, 30));
        entries.add(new Entry(2, 45));
        entries.add(new Entry(3, 40));
        entries.add(new Entry(4, 65));
        entries.add(new Entry(5, 85));
        entries.add(new Entry(6, 110));

        LineDataSet dataSet = new LineDataSet(entries, "XP po danu");

        dataSet.setColor(Color.parseColor("#A8DAFF"));
        dataSet.setCircleColor(Color.parseColor("#A8DAFF"));
        dataSet.setCircleHoleColor(Color.parseColor("#A8DAFF"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setCircleHoleRadius(3f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#2C3E50"));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#A8DAFF"));
        dataSet.setFillAlpha(80);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(dataSet);
        xpLineChart.setData(data);
        xpLineChart.getDescription().setEnabled(false);

        XAxis xAxis = xpLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        xpLineChart.getAxisLeft().setDrawGridLines(false);
        xpLineChart.getAxisRight().setEnabled(false);
        xpLineChart.getLegend().setEnabled(false);
        xpLineChart.animateX(1200);
        xpLineChart.invalidate();
    }

    private void setupEmptyCharts() {
        setupTaskStatusPieChart(null);
        setupCategoryBarChart(null);
        setupDifficultyLineChart();
        setupXPLineChart();
    }

    private void setupBackButton() {
        backButton.setOnClickListener(v -> finish());
    }
}