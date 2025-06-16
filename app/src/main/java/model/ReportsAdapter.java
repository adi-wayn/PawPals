package model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pawpals.R;

import java.util.List;

import model.Report;
import model.firebase.CommunityRepository;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ReportViewHolder> {

    private List<Report> reportList;
    private int expandedPosition = -1;
    private String communityId;
    private Context context;
    private CommunityRepository repo;

    public ReportsAdapter(List<Report> reportList, String communityId, Context context) {
        this.reportList = reportList;
        this.communityId = communityId;
        this.context = context;
        this.repo = new CommunityRepository();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bulletin_post, parent, false); // ודא ששם הקובץ נכון
        return new ReportViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);

        holder.textPostSender.setText(report.getSenderName());
        holder.textPostType.setText(report.getType());
        holder.textPostSubject.setText(report.getSubject());

        // תוכן מקוצר = שורה ראשונה או שתיים בלבד (או כל הגבלה אחרת)
        String shortText = report.getText().length() > 80
                ? report.getText().substring(0, 80) + "..."
                : report.getText();

        holder.textPostMessage.setText(shortText);
        holder.textPostMessageFull.setText(report.getText());

        // האם פתוח?
        boolean isExpanded = position == expandedPosition;
        holder.textPostMessageFull.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.actionButtonsLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // לחיצה על כלל האייטם – פותחת את הכפתורים
        holder.itemView.setOnClickListener(v -> {
            expandedPosition = (isExpanded ? -1 : position);
            notifyDataSetChanged();
        });

        // לחיצה על הטקסט המקוצר – פותחת או סוגרת את המלא
        holder.textPostMessage.setOnClickListener(v -> {
            boolean currentlyVisible = holder.textPostMessageFull.getVisibility() == View.VISIBLE;
            holder.textPostMessageFull.setVisibility(currentlyVisible ? View.GONE : View.VISIBLE);
        });

        // כפתור אישור (לוגיקת דוגמה)
        holder.buttonApprove.setOnClickListener(v -> {
            if (report.getType().equalsIgnoreCase("post")) {
                repo.createFeedPost(communityId, report, new CommunityRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess(String documentId) {
                        Toast.makeText(context, "Post approved and added to bulletin.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(context, "Failed to add to bulletin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(context, "Report approved (not a post).", Toast.LENGTH_SHORT).show();
            }
        });

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
