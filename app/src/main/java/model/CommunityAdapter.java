package model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pawpals.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Pair;
import model.firebase.Firestore.UserRepository;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user, int position);
    }

    private final List<Pair<String, User>> rows = new ArrayList<>();
    @Nullable private OnUserClickListener clickListener;
    private final UserRepository userRepo = new UserRepository();

    // 🧠 Cache לתמונות (userId → imageUrl)
    private final Map<String, String> imageCache = new HashMap<>();

    public CommunityAdapter(@Nullable List<Pair<String, User>> rows) {
        if (rows != null) this.rows.addAll(rows);
    }

    public void setOnUserClickListener(@Nullable OnUserClickListener listener) {
        this.clickListener = listener;
    }

    public void updateData(@Nullable List<Pair<String, User>> newRows) {
        this.rows.clear();
        if (newRows != null) this.rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommunityAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommunityAdapter.ViewHolder holder, int position) {
        Pair<String, User> row = rows.get(position);
        String userId = row.first;
        User user = row.second;
        holder.bind(user, userId, userRepo, imageCache);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener == null) return;
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                clickListener.onUserClick(rows.get(pos).second, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView profilePicture;
        TextView nameText, infoText, communityText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            profilePicture = itemView.findViewById(R.id.profile_picture);
            nameText = itemView.findViewById(R.id.profile_name);
            infoText = itemView.findViewById(R.id.profile_info);
            communityText = itemView.findViewById(R.id.profile_community);
        }

        void bind(@Nullable User user,
                  @NonNull String userId,
                  @NonNull UserRepository repo,
                  @NonNull Map<String, String> imageCache) {

            if (user == null) return;
            nameText.setText(user.getUserName() != null ? user.getUserName() : "Unknown");

            // ✅ טעינת תמונת פרופיל עם cache
            String cachedUrl = imageCache.get(userId);
            if (cachedUrl != null) {
                Glide.with(profilePicture.getContext())
                        .load(cachedUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .centerCrop()
                        .into(profilePicture);
            } else {
                repo.getUserProfileImage(userId, new UserRepository.FirestoreStringCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            imageCache.put(userId, imageUrl); // 🧠 שומר במטמון
                            Glide.with(profilePicture.getContext())
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .centerCrop()
                                    .into(profilePicture);
                        } else {
                            profilePicture.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        profilePicture.setImageResource(R.drawable.ic_profile_placeholder);
                    }
                });
            }

            // 🐶 הצגת כמות כלבים
            repo.getDogsForUser(userId, new UserRepository.FirestoreDogsListCallback() {
                @Override
                public void onSuccess(List<Dog> dogs) {
                    int dogCount = (dogs != null) ? dogs.size() : 0;
                    infoText.setText("Dogs: " + dogCount);
                }

                @Override
                public void onFailure(Exception e) {
                    infoText.setText("Dogs: -");
                }
            });

            // 🌍 שם קהילה
            if (communityText != null) {
                communityText.setText(
                        user.getCommunityName() != null
                                ? "Community: " + user.getCommunityName()
                                : "Community: -"
                );
            }
        }
    }
}