package model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pawpals.R;

import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.PostViewHolder> {

    private List<Report> posts;

    public FeedAdapter(List<Report> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post_card, parent, false);
        return new PostViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Report post = posts.get(position);
        holder.sender.setText(post.getSenderName());
        holder.subject.setText(post.getSubject());
        holder.message.setText(post.getText());
        holder.type.setText(post.getType());
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView sender, subject, message, type;

        public PostViewHolder(View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.text_post_sender);
            subject = itemView.findViewById(R.id.text_post_subject);
            message = itemView.findViewById(R.id.text_post_message);
            type = itemView.findViewById(R.id.text_post_type);
        }
    }
}
