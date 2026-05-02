package com.angelfish.insolve;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AdminReportDetailsActivity extends BaseChildActivity {

    TextView tvStatusValue, tvDetailTitle, tvDetailLocation;
    TextView tvReporterName, tvReporterContact, tvDetailDescription;
    ImageView ivEvidencePhoto;
    CardView cardStatusContainer;
    Spinner spinnerAdminStatus;
    Button btnUpdateCase;

    DatabaseReference dbReference;
    String reportId;
    EditText etAdminRemarks;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report_details);
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
        etAdminRemarks = findViewById(R.id.etAdminRemarks);

        tvStatusValue = findViewById(R.id.tvStatusValue);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvReporterName = findViewById(R.id.tvReporterName);
        tvReporterContact = findViewById(R.id.tvReporterContact);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        ivEvidencePhoto = findViewById(R.id.ivEvidencePhoto);

        spinnerAdminStatus = findViewById(R.id.spinnerAdminStatus);
        btnUpdateCase = findViewById(R.id.btnUpdateCase);

        cardStatusContainer = (CardView) tvStatusValue.getParent().getParent();

        reportId = getIntent().getStringExtra("REPORT_ID");
        if (reportId == null || reportId.isEmpty()) {
            Toast.makeText(this, "Error: No Report ID found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        dbReference = FirebaseDatabase.getInstance("https://insolveapp-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("reports").child(reportId);
        loadReportData();
        btnUpdateCase.setOnClickListener(v -> updateReportStatus());
    }

    private void loadReportData() {
        dbReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    IncidentReport report = snapshot.getValue(IncidentReport.class);
                    if (report != null) {
                        populateUI(report);
                    }
                } else {
                    Toast.makeText(AdminReportDetailsActivity.this, "Report not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminReportDetailsActivity.this, "Failed to load details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(IncidentReport report) {
        tvDetailTitle.setText(report.incidentType);
        tvDetailLocation.setText(report.exactAddress);
        tvReporterName.setText(report.fullName);
        tvReporterContact.setText(report.contactNumber);
        tvDetailDescription.setText(report.description);
        tvStatusValue.setText(report.status.toUpperCase());


        if (report.adminRemarks != null) {
            etAdminRemarks.setText(report.adminRemarks);
        }

        String currentStatus = report.status;
        for (int i = 0; i < spinnerAdminStatus.getCount(); i++) {
            if (spinnerAdminStatus.getItemAtPosition(i).toString().equalsIgnoreCase(currentStatus)) {
                spinnerAdminStatus.setSelection(i);
                break;
            }
        }

        if (report.imageUrl != null && !report.imageUrl.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(report.imageUrl, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivEvidencePhoto.setImageBitmap(decodedByte);
                ivEvidencePhoto.setOnClickListener(v -> showFullScreenImage(decodedByte));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        styleStatusCard(report.status.toLowerCase());
    }

    private void updateReportStatus() {
        String newStatus = spinnerAdminStatus.getSelectedItem().toString().toLowerCase();
        String remarks = etAdminRemarks.getText().toString().trim();

        btnUpdateCase.setEnabled(false);
        btnUpdateCase.setText("Updating...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("adminRemarks", remarks);

        dbReference.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Case Updated Successfully!", Toast.LENGTH_SHORT).show();
                tvStatusValue.setText(newStatus.toUpperCase());
                styleStatusCard(newStatus);
            } else {
                Toast.makeText(this, "Failed to update case.", Toast.LENGTH_SHORT).show();
            }
            btnUpdateCase.setEnabled(true);
            btnUpdateCase.setText("Update Case");
        });
    }

    // Dynamic Coloring based on current status
    private void styleStatusCard(String status) {
        if (status.equals("resolved")) {
            cardStatusContainer.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_resolved_bg));
            tvStatusValue.setTextColor(ContextCompat.getColor(this, R.color.status_resolved_text));

        } else if (status.equals("rejected")) {
            cardStatusContainer.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_rejected_bg));
            tvStatusValue.setTextColor(ContextCompat.getColor(this, R.color.status_rejected_text));

        } else if (status.equals("in progress")) {
            cardStatusContainer.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_inprogress_bg));
            tvStatusValue.setTextColor(ContextCompat.getColor(this, R.color.status_inprogress_text));

        } else {
            cardStatusContainer.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_pending_bg));
            tvStatusValue.setTextColor(ContextCompat.getColor(this, R.color.status_pending_text));
        }
    }
    private void showFullScreenImage(Bitmap bitmap) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout rootWrapper = new FrameLayout(this);
        int padding = 16;
        rootWrapper.setPadding(padding, padding, padding, padding);

        CardView floatingCard = new CardView(this);
        floatingCard.setRadius(24f);
        floatingCard.setCardElevation(30f);
        floatingCard.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        ImageView fullScreenImageView = new ImageView(this);
        fullScreenImageView.setImageBitmap(bitmap);
        fullScreenImageView.setAdjustViewBounds(true);
        fullScreenImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        floatingCard.addView(fullScreenImageView);
        rootWrapper.addView(floatingCard);
        dialog.setContentView(rootWrapper);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        fullScreenImageView.setOnClickListener(v -> dialog.dismiss());
        rootWrapper.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}