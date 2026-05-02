package com.angelfish.insolve;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import androidx.core.content.ContextCompat;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private Context context;
    private List<IncidentReport> reportList;
    private boolean isAdmin;

    public ReportAdapter(Context context, List<IncidentReport> reportList, boolean isAdmin) {
        this.context = context;
        this.reportList = reportList;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        IncidentReport report = reportList.get(position);

        holder.tvTitle.setText(report.incidentType);
        holder.tvLocation.setText(report.exactAddress);
        holder.tvStatus.setText(report.status);

        holder.tvReportId.setText("ID: " + report.reportId);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(report.timestamp)).toUpperCase());

        String status = report.status.toLowerCase();
        if (status.equals("resolved")) {
            holder.cardStatusBg.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_resolved_pill));
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_resolved_text));

        } else if (status.equals("rejected")) {
            holder.cardStatusBg.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_rejected_pill));
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_rejected_text));

        } else {
            holder.cardStatusBg.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_pending_pill));
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text));
        }

        holder.tvReviewDetailsBtn.setOnClickListener(v -> {
            Intent intent;
            if (isAdmin) {
                intent = new Intent(context, AdminReportDetailsActivity.class);
            } else {
                intent = new Intent(context, ReportDetailsActivity.class);
            }

            intent.putExtra("REPORT_ID", report.reportId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLocation, tvStatus, tvDate, tvReportId, tvReviewDetailsBtn;
        CardView cardStatusBg;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvReportTitle);
            tvLocation = itemView.findViewById(R.id.tvReportLocation);
            tvStatus = itemView.findViewById(R.id.tvReportStatus);
            tvDate = itemView.findViewById(R.id.tvReportDate);
            cardStatusBg = itemView.findViewById(R.id.cardStatusBg);

            tvReportId = itemView.findViewById(R.id.tvReportId);
            tvReviewDetailsBtn = itemView.findViewById(R.id.tvReviewDetailsBtn);
        }
    }
}