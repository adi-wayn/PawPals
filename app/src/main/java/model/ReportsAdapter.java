// model/ReportsAdapter.java
package model;

import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.pawpals.R;

import java.util.List;

import model.firebase.firestore.CommunityRepository;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ReportViewHolder> {

    public interface OnReportRemovedListener {
        void onReportRemoved(Report report);
    }

    private final List<Report> reportList; // this is your filtered list
    private final String communityId;
    private final Context context;
    private final CommunityRepository repo;
    private int expandedPosition = -1;

    private OnReportRemovedListener removedListener;

    public ReportsAdapter(List<Report> reportList, String communityId, Context context) {
        this.reportList = reportList;
        this.communityId = communityId;
        this.context = context;
        this.repo = new CommunityRepository();
    }

    public void setOnReportRemovedListener(OnReportRemovedListener l) {
        this.removedListener = l;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bulletin_post, parent, false);
        return new ReportViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);

        holder.textPostSender.setText(report.getSenderName());
        holder.textPostType.setText(report.getType());
        holder.textPostSubject.setText(report.getSubject());

        String text = report.getText() != null ? report.getText() : "";
        String shortText = text.length() > 80 ? text.substring(0, 80) + "..." : text;

        holder.textPostMessage.setText(shortText);
        holder.textPostMessageFull.setText(text);

        boolean isExpanded = position == expandedPosition;
        holder.textPostMessageFull.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.actionButtonsLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // פתיחה/סגירה של הכרטיס (expand/collapse)
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            int oldExpanded = expandedPosition;
            expandedPosition = (position == expandedPosition) ? -1 : pos;

            // רענון מינימלי: סגור הישן ופתח החדש
            if (oldExpanded != -1) notifyItemChanged(oldExpanded);
            if (expandedPosition != -1) notifyItemChanged(expandedPosition);
        });

        // הצגת הטקסט המלא/מקוצר
        holder.textPostMessage.setOnClickListener(v -> {
            boolean currentlyVisible = holder.textPostMessageFull.getVisibility() == View.VISIBLE;
            holder.textPostMessageFull.setVisibility(currentlyVisible ? View.GONE : View.VISIBLE);
        });

        // APPROVE
        holder.buttonApprove.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            String reportId = report.getId();
            if (reportId == null || reportId.isEmpty()) {
                Toast.makeText(context, "Missing report id. Cannot approve.", Toast.LENGTH_LONG).show();
                return;
            }

            if ("post".equalsIgnoreCase(report.getType())) {
                // 1) הוסף ל-feed  2) מחק מה-reports
                repo.createFeedPost(communityId, report, new CommunityRepository.FirestoreCallback() {
                    @Override public void onSuccess(String feedId) {
                        repo.deleteReport(communityId, reportId, new CommunityRepository.FirestoreCallback() {
                            @Override public void onSuccess(String ignored) {
                                Toast.makeText(context, "Post approved, moved to bulletin, and removed from queue.", Toast.LENGTH_SHORT).show();
                                removeAt(adapterPos, report);
                            }
                            @Override public void onFailure(Exception e) {
                                Toast.makeText(context, "Post added but failed to remove from queue: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    @Override public void onFailure(Exception e) {
                        Toast.makeText(context, "Failed to approve post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // לא פוסט – רק מחיקה מהתור
                repo.deleteReport(communityId, reportId, new CommunityRepository.FirestoreCallback() {
                    @Override public void onSuccess(String ignored) {
                        Toast.makeText(context, "Report approved and removed.", Toast.LENGTH_SHORT).show();
                        removeAt(adapterPos, report);
                    }
                    @Override public void onFailure(Exception e) {
                        Toast.makeText(context, "Failed to delete report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        // REJECT
        holder.buttonReject.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            String reportId = report.getId();
            if (reportId == null || reportId.isEmpty()) {
                Toast.makeText(context, "Missing report id. Cannot delete.", Toast.LENGTH_LONG).show();
                return;
            }

            repo.deleteReport(communityId, reportId, new CommunityRepository.FirestoreCallback() {
                @Override public void onSuccess(String ignored) {
                    Toast.makeText(context, "Report rejected and deleted.", Toast.LENGTH_SHORT).show();
                    removeAt(adapterPos, report); // מסיר מה־RecyclerView ומעדכן את הרשימות
                }
                @Override public void onFailure(Exception e) {
                    Toast.makeText(context, "Failed to delete report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void removeAt(int position, Report report) {
        if (position < 0 || position >= reportList.size()) return;
        reportList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, reportList.size() - position);
        if (removedListener != null) {
            removedListener.onReportRemoved(report);
        }
        // collapse if needed
        if (expandedPosition == position) expandedPosition = -1;
        else if (expandedPosition > position) expandedPosition--; // keep expansion index consistent
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView textPostSender, textPostType, textPostSubject, textPostMessage, textPostMessageFull;
        LinearLayout actionButtonsLayout;
        Button buttonApprove, buttonReject;

        public ReportViewHolder(View itemView) {
            super(itemView);
            textPostSender = itemView.findViewById(R.id.text_post_sender);
            textPostType = itemView.findViewById(R.id.text_post_type);
            textPostSubject = itemView.findViewById(R.id.text_post_subject);
            textPostMessage = itemView.findViewById(R.id.text_post_message);
            textPostMessageFull = itemView.findViewById(R.id.text_post_message_full);
            actionButtonsLayout = itemView.findViewById(R.id.actionButtonsLayout);
            buttonApprove = itemView.findViewById(R.id.buttonApprove);
            buttonReject = itemView.findViewById(R.id.buttonReject);
        }
    }
}
