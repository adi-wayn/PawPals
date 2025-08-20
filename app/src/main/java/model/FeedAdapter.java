package model;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pawpals.R;

import java.util.ArrayList;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.PostViewHolder> {

    private final List<Report> posts;
    private final RecyclerView.RecycledViewPool imagesPool = new RecyclerView.RecycledViewPool();

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
        return new PostViewHolder(itemView, imagesPool);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Report post = posts.get(position);

        holder.sender.setText(post.getSenderName());
        holder.subject.setText(post.getSubject());
        holder.message.setText(post.getText());
        holder.type.setText(post.getType());

        // איסוף כל ה־URLs (רשימה או שדה יחיד)
        List<String> all = new ArrayList<>();
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            all.addAll(post.getImageUrls());
        } else if (!TextUtils.isEmpty(post.getImageUrl())) {
            all.add(post.getImageUrl());
        }

        if (all.isEmpty()) {
            holder.imagesRv.setVisibility(View.GONE);
            holder.imagesAdapter.submit(null);
        } else {
            holder.imagesRv.setVisibility(View.VISIBLE);
            holder.imagesAdapter.submit(all);
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
        RecyclerView imagesRv;
        ImagesAdapter imagesAdapter;

        public PostViewHolder(View itemView, RecyclerView.RecycledViewPool sharedPool) {
            super(itemView);
            sender  = itemView.findViewById(R.id.text_post_sender);
            subject = itemView.findViewById(R.id.text_post_subject);
            message = itemView.findViewById(R.id.text_post_message);
            type    = itemView.findViewById(R.id.text_post_type);

            imagesRv = itemView.findViewById(R.id.postImagesRv);
            imagesRv.setLayoutManager(
                    new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false)
            );
            imagesRv.setRecycledViewPool(sharedPool);
            imagesRv.setItemAnimator(null);
            imagesRv.setNestedScrollingEnabled(false);

            imagesAdapter = new ImagesAdapter(/*maxToShow*/4); // מציג עד 4 ומראה +N על האחרונה
            imagesRv.setAdapter(imagesAdapter);
        }
    }
}