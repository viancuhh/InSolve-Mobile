package com.angelfish.insolve;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.text.Editable;
import android.text.TextWatcher;

public class AdminCheckReportActivity extends AppCompatActivity {

    TextView reviewDetailsBtn;
    LinearLayout navHome, navStatus;
    ImageView iconHome, iconStatus;
    TextView tvHome, tvStatus;

    EditText etReportId;
    Button btnSearchReport;
    RecyclerView recyclerView;
    ReportAdapter adapter;

    List<IncidentReport> allReportsList;
    List<IncidentReport> displayList;
    DatabaseReference dbReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_check_report);
        ImageView btnToggleTheme = findViewById(R.id.btnToggleTheme);
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);

        boolean isDark = themePrefs.getBoolean("isDark", false);
        btnToggleTheme.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);

        btnToggleTheme.setOnClickListener(v -> {
            boolean currentDarkState = themePrefs.getBoolean("isDark", false);
            themePrefs.edit().putBoolean("isDark", !currentDarkState).apply();
            if (!currentDarkState) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        dbReference = FirebaseDatabase.getInstance("https://insolveapp-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("reports");
        fetchReports();

        //INITIALIZE VIEWS
        navHome = findViewById(R.id.navHome);
        navStatus = findViewById(R.id.navStatus);
        iconHome = findViewById(R.id.iconHome);
        iconStatus = findViewById(R.id.iconStatus);
        tvHome = findViewById(R.id.tvHome);
        tvStatus = findViewById(R.id.tvStatus);

        etReportId = findViewById(R.id.etReportId);
        btnSearchReport = findViewById(R.id.btnSearchReport);
        recyclerView = findViewById(R.id.recyclerViewSearch);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        allReportsList = new ArrayList<>();
        displayList = new ArrayList<>();

        adapter = new ReportAdapter(this, displayList, true);
        recyclerView.setAdapter(adapter);

        setSelected(navStatus, iconStatus, tvStatus);
        setUnselected(navHome, iconHome, tvHome);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(AdminCheckReportActivity.this, AdminDashboardActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fast_fade_in, R.anim.hold);
        });

        navStatus.setOnClickListener(v -> {
            //ALREADY ON STATUS
        });

        etReportId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterReports(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSearchReport.setOnClickListener(v -> {
            filterReports(etReportId.getText().toString());
            if (displayList.isEmpty()) {
                Toast.makeText(this, "No matching report found.", Toast.LENGTH_SHORT).show();
            }
        });

        //LOGOUT
        ImageView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(AdminCheckReportActivity.this, MainActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Logged out.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    private void fetchReports() {
        dbReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                allReportsList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    IncidentReport report = dataSnapshot.getValue(IncidentReport.class);
                    if (report != null) {
                        allReportsList.add(report);
                    }
                }
                Collections.reverse(allReportsList);
                filterReports(etReportId.getText().toString());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminCheckReportActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterReports(String query) {
        displayList.clear();
        String cleanQuery = query.toLowerCase().replace("#", "").trim();

        if (cleanQuery.isEmpty()) {
            int maxItems = Math.min(allReportsList.size(), 10);
            for (int i = 0; i < maxItems; i++) {
                displayList.add(allReportsList.get(i));
            }
        } else {
            for (IncidentReport report : allReportsList) {
                if (report.reportId != null) {
                    String cleanReportId = report.reportId.toLowerCase().replace("#", "");
                    if (cleanReportId.contains(cleanQuery)) {
                        displayList.add(report);
                    }
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void setSelected(LinearLayout layout, ImageView icon, TextView text) {
        layout.setBackgroundResource(R.drawable.bg_blue_tint);
        icon.setColorFilter(Color.WHITE);
        text.setTextColor(Color.WHITE);
    }

    private void setUnselected(LinearLayout layout, ImageView icon, TextView text) {
        layout.setBackgroundResource(android.R.color.transparent);
        int gray = Color.parseColor("#94A3B8");
        icon.setColorFilter(gray);
        text.setTextColor(gray);
    }
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fast_fade_in, R.anim.hold);
    }
}
