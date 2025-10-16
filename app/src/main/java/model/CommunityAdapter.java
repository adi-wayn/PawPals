package model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pawpals.R;

import java.util.ArrayList;
import java.util.List;

import android.util.Pair;
import model.firebase.Firestore.UserRepository;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user, int position);
    }

    private final List<Pair<String, User>> rows = new ArrayList<>();
    @Nullable private OnUserClickListener clickListener;

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
        holder.bind(user, userId);

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
        TextView nameText, infoText, communityText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText      = itemView.findViewById(R.id.profile_name);
            infoText      = itemView.findViewById(R.id.profile_info);
            communityText = itemView.findViewById(R.id.profile_community);
        }

        void bind(@Nullable User user, @NonNull String userId) {
            if (user == null) return;

            nameText.setText(user.getUserName() != null ? user.getUserName() : "Unknown");

            // שליפה לפי userId
            UserRepository userRepo = new UserRepository();
            userRepo.getDogsForUser(userId, new UserRepository.FirestoreDogsListCallback() {
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