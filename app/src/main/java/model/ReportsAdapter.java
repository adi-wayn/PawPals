package model;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

import java.util.ArrayList;
import java.util.List;

import model.firebase.Firestore.CommunityRepository;

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
        holder.textPostMessage.setText(report.getText());
        holder.textPostMessageFull.setText(report.getText());

        // ✅ הצגת preview קצר (20 תווים לדוגמה)
        String full = report.getText();
        if (full != null && full.length() > 20) {
            holder.textPostMessage.setText(full.substring(0, 20) + "...");
        } else {
            holder.textPostMessage.setText(full);
        }

        // ✅ טיפול בתמונות
        List<String> all = new ArrayList<>();
        if (report.getImageUrls() != null && !report.getImageUrls().isEmpty()) {
            all.addAll(report.getImageUrls());
        } else if (report.getImageUrl() != null && !report.getImageUrl().isEmpty()) {
            all.add(report.getImageUrl());
        }

        if (all.isEmpty()) {
            holder.postImagesRv.setVisibility(View.GONE);
            holder.imagesAdapter.submit(null);
        } else {
            holder.postImagesRv.setVisibility(holder.expanded ? View.VISIBLE : View.GONE);
            holder.imagesAdapter.submit(all);
        }

        // ✅ הצגת / הסתרת תוכן מלא בלחיצה
        holder.itemView.setOnClickListener(v -> {
            holder.expanded = !holder.expanded;
            holder.textPostMessageFull.setVisibility(holder.expanded ? View.VISIBLE : View.GONE);
            holder.textPostMessage.setVisibility(holder.expanded ? View.GONE : View.VISIBLE);
            holder.postImagesRv.setVisibility(holder.expanded && !all.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // ✅ Change button text based on report type
        String type = report.getType() != null ? report.getType().toLowerCase() : "";
        switch (type) {
            case "complaint":
                holder.buttonApprove.setText("Mark as Handled");
                holder.buttonApprove.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E"))); // Gray

                holder.buttonApprove.setTextColor(Color.WHITE);
                break;
            case "assistance":
                holder.buttonApprove.setText("Offer Help");
                holder.buttonApprove.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9800"))); // Orange

                holder.buttonApprove.setTextColor(Color.WHITE);
                break;
            case "manager application":
                holder.buttonApprove.setText("Transfer Manager");
                holder.buttonApprove.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2196F3"))); // Blue

                holder.buttonApprove.setTextColor(Color.WHITE);
                break;
            default:
                holder.buttonApprove.setText("Approve");
                holder.buttonApprove.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Green

                holder.buttonApprove.setTextColor(Color.WHITE);
                break;
        }

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
            }

            // 🔴 תלונה או בקשת סיוע
            else if (type.equals("complaint") || type.equals("assistance")) {
                repo.deleteReport(communityId, report.getId(), new CommunityRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess(String ignored) {
                        Toast.makeText(context, "Report handled and removed.", Toast.LENGTH_SHORT).show();
                        removeAt(holder.getAdapterPosition());
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(context, "Failed to remove report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            else {
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

        // ✅ כפתור מחיקה
        holder.buttonDelete.setOnClickListener(v -> {
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
        holder.actionButtonsLayout.setVisibility(isManager ? View.VISIBLE : View.GONE);
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
        TextView textPostSender, textPostType, textPostSubject, textPostMessage, textPostMessageFull;
        Button buttonApprove, buttonDelete;
        LinearLayout actionButtonsLayout;
        RecyclerView postImagesRv;
        ImagesAdapter imagesAdapter;

        boolean expanded = false;

        ReportViewHolder(View itemView) {
            super(itemView);
            textPostSender = itemView.findViewById(R.id.text_post_sender);
            textPostType = itemView.findViewById(R.id.text_post_type);
            textPostSubject = itemView.findViewById(R.id.text_post_subject);
            textPostMessage = itemView.findViewById(R.id.text_post_message);
            textPostMessageFull = itemView.findViewById(R.id.text_post_message_full);
            buttonApprove = itemView.findViewById(R.id.buttonApprove);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            actionButtonsLayout = itemView.findViewById(R.id.actionButtonsLayout);

            postImagesRv = itemView.findViewById(R.id.postImagesRv);
            postImagesRv.setLayoutManager(
                    new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false)
            );
            postImagesRv.setItemAnimator(null);
            postImagesRv.setNestedScrollingEnabled(false);
            imagesAdapter = new ImagesAdapter(4);
            postImagesRv.setAdapter(imagesAdapter);
        }
    }
}
