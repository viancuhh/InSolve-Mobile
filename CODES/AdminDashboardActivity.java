package com.angelfish.insolve;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

public class AdminDashboardActivity extends AppCompatActivity {

    DatabaseReference dbReference;
    List<IncidentReport> reportList;
    Button reportDetailsBtn;
    LinearLayout navHome, navStatus;
    ImageView iconHome, iconStatus;
    TextView tvHome, tvStatus, tvViewAll;
    RecyclerView recyclerView;
    ReportAdapter adapter;

    TextView tvCountTotal, tvCountResolved, tvCountPending, tvCountRejected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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

        navHome = findViewById(R.id.navHome);
        navStatus = findViewById(R.id.navStatus);
        iconHome = findViewById(R.id.iconHome);
        iconStatus = findViewById(R.id.iconStatus);
        tvHome = findViewById(R.id.tvHome);
        tvStatus = findViewById(R.id.tvStatus);
        recyclerView = findViewById(R.id.recyclerViewReports);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        tvCountTotal = findViewById(R.id.tvCountTotal);
        tvCountResolved = findViewById(R.id.tvCountResolved);
        tvCountPending = findViewById(R.id.tvCountPending);
        tvCountRejected = findViewById(R.id.tvCountRejected);

        reportList = new ArrayList<>();
        adapter = new ReportAdapter(this, reportList, true);
        recyclerView.setAdapter(adapter);

        dbReference = FirebaseDatabase.getInstance("https://insolveapp-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("reports");
        fetchReports();

        setSelected(navHome, iconHome, tvHome);
        setUnselected(navStatus, iconStatus, tvStatus);
        navHome.setOnClickListener(v -> {
            //ALREADY ON HOME
        });

        navStatus.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminCheckReportActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fast_fade_in, R.anim.hold);
        });

        tvViewAll = findViewById(R.id.tvViewAll);
        tvViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminCheckReportActivity.class);
            startActivity(intent);
        });

        ImageView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Logged out.", Toast.LENGTH_SHORT).show();
            finish();
        });


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

    private void fetchReports() {
        dbReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                reportList.clear();

                List<IncidentReport> tempAllReports = new ArrayList<>();

                int total = 0, resolved = 0, pending = 0, rejected = 0;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    IncidentReport report = dataSnapshot.getValue(IncidentReport.class);
                    if (report != null) {
                        tempAllReports.add(report);
                        total++;
                        if (report.status != null) {
                            String status = report.status.toLowerCase();
                            if (status.equals("resolved")) {
                                resolved++;
                            } else if (status.equals("rejected")) {
                                rejected++;
                            } else {
                                pending++;
                            }
                        }
                    }
                }

                tvCountTotal.setText(String.valueOf(total));
                tvCountResolved.setText(String.valueOf(resolved));
                tvCountPending.setText(String.valueOf(pending));
                tvCountRejected.setText(String.valueOf(rejected));

                Collections.reverse(tempAllReports);

                int limit = Math.min(5, tempAllReports.size());
                for (int i = 0; i < limit; i++) {
                    reportList.add(tempAllReports.get(i));
                }

                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminDashboardActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fast_fade_in, R.anim.hold);
    }
}