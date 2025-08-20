package model;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pawpals.R;

import java.util.ArrayList;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.PostViewHolder> {

    private final List<Report> posts;

    public FeedAdapter(List<Report> posts) {
        this.posts = posts != null ? posts : new ArrayList<>();
        setHasStableIds(true); // אם יש id לדוקומנט זה ייתן מיחזור יציב יותר
    }

    @Override public long getItemId(int position) {
        Report r = posts.get(position);
        // אם setId() נקבע כשמשכת מה־Firestore (מומלץ), נשתמש בו
        if (r.getId() != null) return r.getId().hashCode();
        return RecyclerView.NO_ID;
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

        // ----- תמונות -----
        List<String> urls = post.getImageUrls();
        String single = post.getImageUrl();

        String firstToShow = null;
        int extraCount = 0;

        if (urls != null && !urls.isEmpty()) {
            firstToShow = urls.get(0);
            extraCount = urls.size() - 1;
        } else if (!TextUtils.isEmpty(single)) {
            firstToShow = single;
            extraCount = 0;
        }

        if (!TextUtils.isEmpty(firstToShow)) {
            holder.image.setVisibility(View.VISIBLE);
            Glide.with(holder.image.getContext())
                    .load(firstToShow)
                    .placeholder(R.drawable.image_placeholder) // אופציונלי
                    .error(R.drawable.image_error)             // אופציונלי
                    .into(holder.image);

            if (extraCount > 0) {
                holder.imageCountBadge.setVisibility(View.VISIBLE);
                holder.imageCountBadge.setText("+" + extraCount);
            } else {
                holder.imageCountBadge.setVisibility(View.GONE);
            }

            // (אופציונלי) פתיחת גלריה במסך מלא כשנוגעים בתמונה
            // holder.image.setOnClickListener(v -> openGallery(v.getContext(), urlsOrSingle));

        } else {
            holder.image.setVisibility(View.GONE);
            holder.imageCountBadge.setVisibility(View.GONE);
            // חשוב לנקות Glide כשאין תמונה כדי שלא יופיע reuse שגוי
            Glide.with(holder.image.getContext()).clear(holder.image);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void updateData(List<Report> newPosts) {
        posts.clear();
        if (newPosts != null) posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView sender, subject, message, type;
        ImageView image;
        TextView imageCountBadge;

        public PostViewHolder(View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.text_post_sender);
            subject = itemView.findViewById(R.id.text_post_subject);
            message = itemView.findViewById(R.id.text_post_message);
            type    = itemView.findViewById(R.id.text_post_type);
            image   = itemView.findViewById(R.id.image_post);
            imageCountBadge = itemView.findViewById(R.id.image_count_badge);
        }
    }
}