package model;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pawpals.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

import model.firebase.Firestore.CommunityRepository;

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

        // ✅ טעינת תמונת הפרופיל של השולח
        String senderId = post.getSenderId();
        Log.d("FeedAdapter", "onBindViewHolder: senderId=" + senderId + " | senderName=" + post.getSenderName());


        if (senderId != null && !senderId.isEmpty()) {
            model.firebase.Firestore.UserRepository userRepo = new model.firebase.Firestore.UserRepository();

            Log.d("FeedAdapter", "Fetching profile image for senderId=" + senderId);
            userRepo.getUserProfileImage(senderId, new model.firebase.Firestore.UserRepository.FirestoreStringCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    Log.d("FeedAdapter", "Profile image URL for " + senderId + ": " + imageUrl);

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(holder.itemView.getContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .centerCrop()
                                .into(holder.senderImage);
                    } else {
                        Log.w("FeedAdapter", "No image found for user " + senderId + ", using placeholder");
                        holder.senderImage.setImageResource(R.drawable.ic_profile_placeholder);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.e("FeedAdapter", "Failed to load image for " + senderId, e);
                    holder.senderImage.setImageResource(R.drawable.ic_profile_placeholder);
                }
            });
        } else {
            android.util.Log.w("FeedAdapter", "Missing senderId for post: " + post.getId());
            holder.senderImage.setImageResource(R.drawable.ic_profile_placeholder);
        }

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
        ShapeableImageView senderImage;
        RecyclerView imagesRv;
        ImagesAdapter imagesAdapter;
        MaterialButton buttonDelete;

        public PostViewHolder(View itemView, RecyclerView.RecycledViewPool sharedPool) {
            super(itemView);
            sender  = itemView.findViewById(R.id.text_post_sender);
            subject = itemView.findViewById(R.id.text_post_subject);
            message = itemView.findViewById(R.id.text_post_message);
            type    = itemView.findViewById(R.id.text_post_type);
            senderImage = itemView.findViewById(R.id.image_sender_profile);

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
