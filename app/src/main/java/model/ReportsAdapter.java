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
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import model.firebase.Firestore.CommunityRepository;
import model.firebase.Storage.StorageRepository;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ReportViewHolder> {

    public interface OnReportRemovedListener {
        void onReportRemoved(Report report);
    }

    private final List<Report> reportList; // filtered list
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
        boolean isPost = report.isPost();
        boolean isManagerApp = report.isManagerApplication();

        // כפתורי פעולה מוצגים רק כשמורחב ורק אם יש מה לאשר/לדחות
        boolean showActions = isExpanded && (isPost || isManagerApp);
        holder.textPostMessageFull.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.actionButtonsLayout.setVisibility(showActions ? View.VISIBLE : View.GONE);

        // פתיחה/סגירה של הכרטיס (expand/collapse)
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            int oldExpanded = expandedPosition;
            expandedPosition = (position == expandedPosition) ? -1 : pos;

            if (oldExpanded != -1) notifyItemChanged(oldExpanded);
            if (expandedPosition != -1) notifyItemChanged(expandedPosition);
        });

        // טקסט מלא בלחיצה על המקוצר
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

            if (isManagerApp) {
                // אישור מועמדות לניהול → transferManager
                String oldManagerUid = FirebaseAuth.getInstance().getUid();
                String newManagerUid = report.getApplicantUserId();

                if (communityId == null || oldManagerUid == null || newManagerUid == null || newManagerUid.isEmpty()) {
                    Toast.makeText(context, "Missing data for manager transfer.", Toast.LENGTH_LONG).show();
                    return;
                }

                repo.transferManager(communityId, oldManagerUid, newManagerUid, new CommunityRepository.FirestoreCallback() {
                    @Override public void onSuccess(String id) {
                        // מחיקת הדיווח אחרי ההעברה
                        repo.deleteReport(communityId, reportId, new CommunityRepository.FirestoreCallback() {
                            @Override public void onSuccess(String ignored) {
                                Toast.makeText(context, "Manager transferred successfully.", Toast.LENGTH_SHORT).show();
                                removeAt(adapterPos, report);
                            }
                            @Override public void onFailure(Exception e) {
                                // גם אם המחיקה נכשלה – נסיר מקומית כדי לא לתקוע את ה-UI
                                removeAt(adapterPos, report);
                            }
                        });
                    }
                    @Override public void onFailure(Exception e) {
                        Toast.makeText(context, "Failed to transfer manager: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

            } else if (isPost) {
                // פוסט מאושר → עובר ל-feed ונמחק מהתור
                repo.createFeedPost(communityId, report, new CommunityRepository.FirestoreCallback() {
                    @Override public void onSuccess(String feedId) {
                        repo.deleteReport(communityId, reportId, new CommunityRepository.FirestoreCallback() {
                            @Override public void onSuccess(String ignored) {
                                Toast.makeText(context, "Post approved & moved to bulletin.", Toast.LENGTH_SHORT).show();
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
                // סוג אחר – רק מחיקה
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
                    // מחיקה מה־Storage (best-effort)
                    StorageRepository s = new StorageRepository();
                    java.util.List<String> urls = new java.util.ArrayList<>();
                    if (report.getImageUrl() != null) urls.add(report.getImageUrl());
                    if (report.getImageUrls() != null) urls.addAll(report.getImageUrls());
                    for (String u : urls) {
                        s.deleteByUrl(u, new StorageRepository.SimpleCallback() {
                            @Override public void onSuccess() { /* no-op */ }
                            @Override public void onFailure(@NonNull Exception e) { /* swallow */ }
                        });
                    }

                    Toast.makeText(context, "Report rejected and deleted.", Toast.LENGTH_SHORT).show();
                    removeAt(adapterPos, report);
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
        if (expandedPosition == position) expandedPosition = -1;
        else if (expandedPosition > position) expandedPosition--;
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