package model;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pawpals.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import model.firebase.firestore.CommunityRepository;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.PostViewHolder> {

    private final List<Report> posts;
    private final RecyclerView.RecycledViewPool imagesPool = new RecyclerView.RecycledViewPool();
    private final CommunityRepository repo = new CommunityRepository();
    private String communityId;
    private boolean isManager;

    // --- בנאי בסיסי ---
    public FeedAdapter(List<Report> posts) {
        this.posts = posts != null ? posts : new ArrayList<>();
        setHasStableIds(true);
    }

    // --- קבלת communityId + הרשאת מנהל מה־Activity ---
    public void setCommunityData(String communityId, boolean isManager) {
        this.communityId = communityId;
        this.isManager = isManager;
    }

    @Override
    public long getItemId(int position) {
        Report r = posts.get(position);
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

        // טיפול בתמונות (בודדת או רשימה)
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

        // --- כפתור מחיקה --- (רק למנהל ובתוך קהילה אמיתית)
        if (isManager && communityId != null) {
            holder.buttonDelete.setVisibility(View.VISIBLE);
            holder.buttonDelete.setOnClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION) return;

                String postId = post.getId();
                if (postId == null || postId.isEmpty()) {
                    Toast.makeText(holder.itemView.getContext(),
                            "Cannot delete: missing post id", Toast.LENGTH_SHORT).show();
                    return;
                }

                repo.deleteFeedPost(communityId, postId, new CommunityRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess(String ignored) {
                        Toast.makeText(holder.itemView.getContext(),
                                "Post deleted", Toast.LENGTH_SHORT).show();
                        posts.remove(adapterPos);
                        notifyItemRemoved(adapterPos);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(holder.itemView.getContext(),
                                "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        } else {
            holder.buttonDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    // --- עדכון רשימת פוסטים ---
    public void updateData(List<Report> newPosts) {
        posts.clear();
        if (newPosts != null) posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    // --- ViewHolder ---
    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView sender, subject, message, type;
        RecyclerView imagesRv;
        ImagesAdapter imagesAdapter;
        MaterialButton buttonDelete;

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

            imagesAdapter = new ImagesAdapter(4);
            imagesRv.setAdapter(imagesAdapter);

            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
