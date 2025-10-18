package com.example.maproject.ui.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.maproject.R;
import com.example.maproject.service.StatisticsManagerService;
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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private static final String TAG = "StatisticsActivity";

    private TextView activeDaysTextView, longestStreakTextView, specialMissionsTextView, avgDifficultyTextView;
    private PieChart taskStatusPieChart;
    private BarChart categoryBarChart;
    private LineChart xpLineChart, difficultyLineChart;
    private Button backButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private StatisticsManagerService statisticsManager;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        statisticsManager = new StatisticsManagerService();

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();

            initViews();
            loadStatistics();
            setupBackButton();
        } else {
            Toast.makeText(this, "Error: user is not logged in.", Toast.LENGTH_LONG).show();
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
                        displayStatistics(document);
                    } else {
                        displayEmptyStatistics();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading statistics", e);
                    Toast.makeText(this, "Greška pri učitavanju statistike", Toast.LENGTH_SHORT).show();
                    displayEmptyStatistics();
                });
    }

    private void displayStatistics(DocumentSnapshot document) {

        Long activeDays = document.getLong("activeDays");
        int activeDaysValue = activeDays != null ? activeDays.intValue() : 0;
        activeDaysTextView.setText(String.valueOf(activeDaysValue));


        Long longestStreak = document.getLong("longestStreak");
        int longestStreakValue = longestStreak != null ? longestStreak.intValue() : 0;
        longestStreakTextView.setText(String.valueOf(longestStreakValue));


        Long missionsStarted = document.getLong("specialMissionsStarted");
        Long missionsCompleted = document.getLong("specialMissionsCompleted");
        specialMissionsTextView.setText(
                (missionsCompleted != null ? missionsCompleted : 0) + "/" +
                        (missionsStarted != null ? missionsStarted : 0)
        );


        Long totalDifficultySum = document.getLong("totalCompletedTaskDifficultySum");
        Long totalCompleted = document.getLong("totalTasksCompleted");
        double avgDifficulty = 0.0;

        if (totalCompleted != null && totalCompleted > 0 && totalDifficultySum != null) {
            avgDifficulty = (double) totalDifficultySum / totalCompleted;
        }

        avgDifficultyTextView.setText(String.format("%.1f", avgDifficulty));


        setupTaskStatusPieChart(document);


        setupCategoryBarChart(document);


        setupXPLineChart(document);


        setupDifficultyLineChart(document);
    }

    private void displayEmptyStatistics() {
        activeDaysTextView.setText("0");
        longestStreakTextView.setText("0");
        specialMissionsTextView.setText("0/0");
        avgDifficultyTextView.setText("0.0");
        setupEmptyCharts();
    }

    private void setupTaskStatusPieChart(DocumentSnapshot document) {
        Long completed = document.getLong("totalTasksCompleted");
        Long notDone = document.getLong("totalTasksNotDone");
        Long cancelled = document.getLong("totalTasksCancelled");

        List<PieEntry> entries = new ArrayList<>();

        if (completed != null && completed > 0) {
            entries.add(new PieEntry(completed, "Completed"));
        }
        if (notDone != null && notDone > 0) {
            entries.add(new PieEntry(notDone, "Not done"));
        }
        if (cancelled != null && cancelled > 0) {
            entries.add(new PieEntry(cancelled, "Cancelled"));
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No data"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{
                Color.parseColor("#B5EAD7"),  // Completed - zelena
                Color.parseColor("#FF9AA2"),  // Not done - crvena
                Color.parseColor("#FFD4A3")   // Cancelled - narandžasta
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

    private void setupCategoryBarChart(DocumentSnapshot document) {
        Map<String, Object> tasksPerCategory = (Map<String, Object>) document.get("tasksPerCategory");

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        if (tasksPerCategory != null && !tasksPerCategory.isEmpty()) {
            for (Map.Entry<String, Object> entry : tasksPerCategory.entrySet()) {
                labels.add(entry.getKey());
                long count = entry.getValue() instanceof Long ? (Long) entry.getValue() : 0L;
                entries.add(new BarEntry(index++, count));
            }
        } else {
            labels.add("No data");
            entries.add(new BarEntry(0, 0));
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
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        categoryBarChart.getAxisLeft().setDrawGridLines(false);
        categoryBarChart.getAxisRight().setEnabled(false);
        categoryBarChart.getLegend().setEnabled(false);
        categoryBarChart.animateY(1200);
        categoryBarChart.invalidate();
    }

    private void setupXPLineChart(DocumentSnapshot document) {
        Map<String, Object> dailyXP = (Map<String, Object>) document.get("dailyXP");
        List<String> last7Days = statisticsManager.getLastNDays(7);

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < last7Days.size(); i++) {
            String date = last7Days.get(i);
            long xp = 0;

            if (dailyXP != null && dailyXP.containsKey(date)) {
                Object xpValue = dailyXP.get(date);
                xp = xpValue instanceof Long ? (Long) xpValue : 0L;
            }

            entries.add(new Entry(i, xp));
            // Prikaži samo MM-DD format
            labels.add(date.substring(5)); // Npr. "01-15"
        }

        LineDataSet dataSet = new LineDataSet(entries, "XP per day");
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
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        xpLineChart.getAxisLeft().setDrawGridLines(false);
        xpLineChart.getAxisRight().setEnabled(false);
        xpLineChart.getLegend().setEnabled(false);
        xpLineChart.animateX(1200);
        xpLineChart.invalidate();
    }

    private void setupDifficultyLineChart(DocumentSnapshot document) {
        Map<String, Object> dailyDifficultySum = (Map<String, Object>) document.get("dailyDifficultySum");
        Map<String, Object> dailyCompletedCount = (Map<String, Object>) document.get("dailyCompletedCount");
        List<String> last7Days = statisticsManager.getLastNDays(7);

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < last7Days.size(); i++) {
            String date = last7Days.get(i);
            float avgDifficulty = 0f;

            if (dailyDifficultySum != null && dailyCompletedCount != null) {
                Object diffSum = dailyDifficultySum.get(date);
                Object completed = dailyCompletedCount.get(date);

                long diffSumValue = diffSum instanceof Long ? (Long) diffSum : 0L;
                long completedValue = completed instanceof Long ? (Long) completed : 0L;

                if (completedValue > 0) {
                    avgDifficulty = (float) diffSumValue / completedValue;
                }
            }

            entries.add(new Entry(i, avgDifficulty));
            labels.add(date.substring(5)); // MM-DD format
        }

        LineDataSet dataSet = new LineDataSet(entries, "Average difficulty");
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
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        difficultyLineChart.getAxisLeft().setDrawGridLines(false);
        difficultyLineChart.getAxisRight().setEnabled(false);
        difficultyLineChart.getLegend().setEnabled(false);
        difficultyLineChart.animateX(1200);
        difficultyLineChart.invalidate();
    }

    private void setupEmptyCharts() {

        List<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(1, "No data"));
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColor(Color.parseColor("#E0E0E0"));
        taskStatusPieChart.setData(new PieData(pieDataSet));
        taskStatusPieChart.invalidate();

        List<BarEntry> barEntries = new ArrayList<>();
        barEntries.add(new BarEntry(0, 0));
        BarDataSet barDataSet = new BarDataSet(barEntries, "");
        barDataSet.setColor(Color.parseColor("#E0E0E0"));
        categoryBarChart.setData(new BarData(barDataSet));
        categoryBarChart.invalidate();


        List<Entry> lineEntries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            lineEntries.add(new Entry(i, 0));
        }

        LineDataSet xpDataSet = new LineDataSet(lineEntries, "No data");
        xpDataSet.setColor(Color.parseColor("#E0E0E0"));
        xpLineChart.setData(new LineData(xpDataSet));
        xpLineChart.invalidate();

        LineDataSet diffDataSet = new LineDataSet(lineEntries, "No data");
        diffDataSet.setColor(Color.parseColor("#E0E0E0"));
        difficultyLineChart.setData(new LineData(diffDataSet));
        difficultyLineChart.invalidate();
    }

    private void setupBackButton() {
        backButton.setOnClickListener(v -> finish());
    }
}