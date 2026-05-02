package com.angelfish.insolve;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportDetailsActivity extends BaseChildActivity {

    LinearLayout llAdminRemarks;
    TextView tvUserAdminRemarks, tvAdminRemarksLabel;
    View vAdminRemarksLine;

    TextView tvStatusValue, tvStatusId, tvStatusDescription;
    TextView tvDetailTitle, tvDetailLocation, tvDetailDescription;
    TextView tvStep1Date, tvEvidenceLabel;
    ImageView ivEvidencePhoto;
    CardView cardStatusContainer, cardStatusIdBg;

    DatabaseReference dbReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_details);
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

        tvStatusValue = findViewById(R.id.tvStatusValue);
        tvStatusId = findViewById(R.id.tvStatusId);
        tvStatusDescription = findViewById(R.id.tvStatusDescription);

        llAdminRemarks = findViewById(R.id.llAdminRemarks);
        tvUserAdminRemarks = findViewById(R.id.tvUserAdminRemarks);
        tvAdminRemarksLabel = findViewById(R.id.tvAdminRemarksLabel);
        vAdminRemarksLine = findViewById(R.id.vAdminRemarksLine);

        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvEvidenceLabel = findViewById(R.id.tvEvidenceLabel);

        ivEvidencePhoto = findViewById(R.id.ivEvidencePhoto);

        cardStatusContainer = (CardView) tvStatusValue.getParent().getParent().getParent();
        cardStatusIdBg = (CardView) tvStatusId.getParent();

        String reportId = getIntent().getStringExtra("REPORT_ID");

        if (reportId == null || reportId.isEmpty()) {
            Toast.makeText(this, "Error: No Report ID found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbReference = FirebaseDatabase.getInstance("https://insolveapp-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("reports").child(reportId);

        loadReportData();


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
                    Toast.makeText(ReportDetailsActivity.this, "Report no longer exists.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ReportDetailsActivity.this, "Failed to load details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFullScreenImage(Bitmap bitmap) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout rootWrapper = new FrameLayout(this);

        int padding = 20;
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

    private void populateUI(IncidentReport report) {
        tvDetailTitle.setText(report.incidentType);
        tvDetailLocation.setText(report.exactAddress);
        tvDetailDescription.setText(report.description);
        tvStatusId.setText("ID: " + report.reportId);
        tvStatusValue.setText(report.status.toUpperCase());


        if (report.adminRemarks != null && !report.adminRemarks.trim().isEmpty()) {
            llAdminRemarks.setVisibility(View.VISIBLE);
            tvUserAdminRemarks.setText("\"" + report.adminRemarks + "\"");
        } else {
            llAdminRemarks.setVisibility(View.GONE);
        }

        if (report.imageUrl != null && !report.imageUrl.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(report.imageUrl, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivEvidencePhoto.setImageBitmap(decodedByte);
                ivEvidencePhoto.setOnClickListener(v -> showFullScreenImage(decodedByte));
            } catch (Exception e) {
                e.printStackTrace();
                hideImage();
            }
        } else {
            hideImage();
        }

        styleStatusCard(report.status.toLowerCase());
    }

    private void hideImage() {
        ivEvidencePhoto.setVisibility(View.GONE);
        tvEvidenceLabel.setVisibility(View.GONE);
        ((View) ivEvidencePhoto.getParent()).setVisibility(View.GONE);
    }

    private void styleStatusCard(String status) {
        if (status.equals("resolved")) {
            cardStatusContainer.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_resolved_bg));
            cardStatusIdBg.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_resolved_pill));

            int mainGreen = ContextCompat.getColor(this, R.color.status_resolved_text);
            int darkGreen = ContextCompat.getColor(this, R.color.status_resolved_text_dark);

            tvStatusValue.setTextColor(mainGreen);
            tvStatusId.setTextColor(mainGreen);
            tvAdminRemarksLabel.setTextColor(mainGreen);
            vAdminRemarksLine.setBackgroundColor(mainGreen);

            tvUserAdminRemarks.setTextColor(darkGreen);
            tvStatusDescription.setText("This incident has been resolved by our dispatch team. Thank you for making the community safer!");

        } else if (status.equals("rejected")) {
            cardStatusContainer.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_rejected_bg));
            cardStatusIdBg.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_rejected_pill));

            int mainRed = ContextCompat.getColor(this, R.color.status_rejected_text);
            int darkRed = ContextCompat.getColor(this, R.color.status_rejected_text_dark);

            tvStatusValue.setTextColor(mainRed);
            tvStatusId.setTextColor(mainRed);
            tvAdminRemarksLabel.setTextColor(mainRed);
            vAdminRemarksLine.setBackgroundColor(mainRed);

            tvUserAdminRemarks.setTextColor(darkRed);
            tvStatusDescription.setText("This report was rejected. It may be a duplicate or lack sufficient details to dispatch a team.");

        } else {
            cardStatusContainer.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_pending_bg));
            cardStatusIdBg.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_pending_pill));

            int mainOrange = ContextCompat.getColor(this, R.color.status_pending_text);
            int darkOrange = ContextCompat.getColor(this, R.color.status_pending_text_dark);

            tvStatusValue.setTextColor(mainOrange);
            tvStatusId.setTextColor(mainOrange);
            tvAdminRemarksLabel.setTextColor(mainOrange);
            vAdminRemarksLine.setBackgroundColor(mainOrange);

            tvUserAdminRemarks.setTextColor(darkOrange);

            if (status.equals("in progress")) {
                tvStatusDescription.setText("A dispatch team has been assigned and is currently working on this report.");
            } else {
                tvStatusDescription.setText("Your report is currently pending review by the infrastructure department.");
            }
        }
    }
}