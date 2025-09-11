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

import model.firebase.firestore.CommunityRepository;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ReportViewHolder> {

    public interface OnReportRemovedListener {
        void onReportRemoved(Report report);
    }

    private final List<Report> reportList;
    private final String communityId;
    private final Context context;
    private final CommunityRepository repo;
    private final boolean isManager;
    private OnReportRemovedListener removedListener;

    public ReportsAdapter(List<Report> reportList, String communityId, Context context, boolean isManager) {
        this.reportList = reportList;
        this.communityId = communityId;
        this.context = context;
        this.repo = new CommunityRepository();
        this.isManager = isManager;
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

        boolean isManagerApp = report.isManagerApplication();

        // ✅ כפתור אישור
        holder.buttonApprove.setOnClickListener(v -> {
            if (isManagerApp) {
                String oldManagerUid = FirebaseAuth.getInstance().getUid();
                String newManagerUid = report.getApplicantUserId();
                repo.transferManager(communityId, oldManagerUid, newManagerUid, new CommunityRepository.FirestoreCallback() {
                    @Override public void onSuccess(String id) {
                        repo.deleteReport(communityId, report.getId(), new CommunityRepository.FirestoreCallback() {
                            @Override public void onSuccess(String ignored) {
                                Toast.makeText(context, "Manager transferred successfully.", Toast.LENGTH_SHORT).show();
                                removeAt(holder.getAdapterPosition());
                            }
                            @Override public void onFailure(Exception e) { removeAt(holder.getAdapterPosition()); }
                        });
                    }
                    @Override public void onFailure(Exception e) {
                        Toast.makeText(context, "Failed to transfer manager: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                repo.createFeedPost(communityId, report, new CommunityRepository.FirestoreCallback() {
                    @Override public void onSuccess(String feedId) {
                        repo.deleteReport(communityId, report.getId(), new CommunityRepository.FirestoreCallback() {
                            @Override public void onSuccess(String ignored) {
                                Toast.makeText(context, "Post approved & moved to bulletin.", Toast.LENGTH_SHORT).show();
                                removeAt(holder.getAdapterPosition());
                            }
                            @Override public void onFailure(Exception e) { }
                        });
                    }
                    @Override public void onFailure(Exception e) { }
                });
            }
        });

        // ✅ כפתור דחייה/מחיקה
        holder.buttonReject.setOnClickListener(v -> {
            repo.deleteReport(communityId, report.getId(), new CommunityRepository.FirestoreCallback() {
                @Override public void onSuccess(String ignored) {
                    Toast.makeText(context, "Report deleted.", Toast.LENGTH_SHORT).show();
                    removeAt(holder.getAdapterPosition());
                }
                @Override public void onFailure(Exception e) {
                    Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        // ✅ הצגת כפתורים רק למנהל
        if (isManager) {
            holder.actionButtonsLayout.setVisibility(View.VISIBLE);
        } else {
            holder.actionButtonsLayout.setVisibility(View.GONE);
        }
    }

    private void removeAt(int position) {
        if (position < 0 || position >= reportList.size()) return;
        Report removed = reportList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, reportList.size());

        if (removedListener != null) {
            removedListener.onReportRemoved(removed);
        }
    }

    @Override
    public int getItemCount() { return reportList.size(); }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView textPostSender, textPostType, textPostSubject;
        Button buttonApprove, buttonReject;
        LinearLayout actionButtonsLayout;

        ReportViewHolder(View itemView) {
            super(itemView);
            textPostSender = itemView.findViewById(R.id.text_post_sender);
            textPostType = itemView.findViewById(R.id.text_post_type);
            textPostSubject = itemView.findViewById(R.id.text_post_subject);
            buttonApprove = itemView.findViewById(R.id.buttonApprove);
            buttonReject = itemView.findViewById(R.id.buttonReject);
            actionButtonsLayout = itemView.findViewById(R.id.actionButtonsLayout);
        }
    }
}
