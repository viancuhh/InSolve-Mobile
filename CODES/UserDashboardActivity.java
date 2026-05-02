package com.angelfish.insolve;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.graphics.Color;
import androidx.cardview.widget.CardView;
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


public class UserDashboardActivity extends AppCompatActivity {

    androidx.cardview.widget.CardView cvReport;
    LinearLayout navHome, navStatus;
    ImageView iconHome, iconStatus;
    TextView tvHome, tvStatus, tvViewAll;
    RecyclerView recyclerView;
    ReportAdapter adapter;
    List<IncidentReport> reportList;
    DatabaseReference dbReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_dashboard);
        ImageView btnToggleTheme = findViewById(R.id.btnToggleTheme);
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);

        boolean isDark = themePrefs.getBoolean("isDark", false);
        btnToggleTheme.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);

        btnToggleTheme.setOnClickListener(v -> {
            // 1. Get the current state
            boolean currentDarkState = themePrefs.getBoolean("isDark", false);

            // 2. Save the opposite state
            themePrefs.edit().putBoolean("isDark", !currentDarkState).apply();

            // 3. Command Android to redraw the screen!
            if (!currentDarkState) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        //INITIALIZE VIEWS
        navHome = findViewById(R.id.navHome);
        navStatus = findViewById(R.id.navStatus);
        iconHome = findViewById(R.id.iconHome);
        iconStatus = findViewById(R.id.iconStatus);
        tvHome = findViewById(R.id.tvHome);
        tvStatus = findViewById(R.id.tvStatus);


        //NAV BAR STUFF
        setSelected(navHome, iconHome, tvHome);
        setUnselected(navStatus, iconStatus, tvStatus);

        navHome.setOnClickListener(v -> {
            //ALREADY ON HOME
        });

        navStatus.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboardActivity.this, CheckReportActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fast_fade_in, R.anim.hold);
        });


        //LOGOUT
        ImageView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboardActivity.this, MainActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Logged out.", Toast.LENGTH_SHORT).show();
            finish();
        });

        cvReport = findViewById(R.id.cvReport);
        cvReport.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboardActivity.this, ReportIncidentActivity.class);
            startActivity(intent);
        });

        tvViewAll = findViewById(R.id.tvViewAll);
        tvViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboardActivity.this, CheckReportActivity.class);
            startActivity(intent);
        });

        recyclerView = findViewById(R.id.recyclerViewRecentReports);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        reportList = new ArrayList<>();
        adapter = new ReportAdapter(this, reportList, false);
        recyclerView.setAdapter(adapter);

        dbReference = FirebaseDatabase.getInstance("https://insolveapp-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("reports");
        fetchRecentReports();
    }

    private void fetchRecentReports() {
        dbReference.limitToLast(5).addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                reportList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    IncidentReport report = dataSnapshot.getValue(IncidentReport.class);
                    if (report != null) {
                        reportList.add(0, report);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(UserDashboardActivity.this, "Failed to load recent reports.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setSelected(LinearLayout layout, ImageView icon, TextView text) {
        layout.setBackgroundResource(R.drawable.bg_blue_tint);
        icon.setColorFilter(Color.WHITE);
        text.setTextColor(Color.WHITE);
    }

    private void setUnselected(LinearLayout layout, ImageView icon, TextView text) {
        layout.setBackgroundResource(android.R.color.transparent);
        int unselectedColor = ContextCompat.getColor(this, R.color.text_secondary);
        icon.setColorFilter(unselectedColor);
        text.setTextColor(unselectedColor);
    }

}