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

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    // מאזין שמעביר גם את המיקום ברשימה
    public interface OnUserClickListener {
        void onUserClick(User user, int position);
    }

    private final List<User> users = new ArrayList<>();
    @Nullable private OnUserClickListener clickListener;

    public CommunityAdapter(@Nullable List<User> users) {
        if (users != null) this.users.addAll(users);
    }

    public void setOnUserClickListener(@Nullable OnUserClickListener listener) {
        this.clickListener = listener;
    }

    public void updateData(@Nullable List<User> newUsers) {
        this.users.clear();
        if (newUsers != null) this.users.addAll(newUsers);
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
        final User user = users.get(position);
        holder.bind(user);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener == null) return;

            int pos = holder.getAdapterPosition(); // עובד בכל הגרסאות
            if (pos != RecyclerView.NO_POSITION) {
                clickListener.onUserClick(users.get(pos), pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, infoText, communityText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText      = itemView.findViewById(R.id.profile_name);
            infoText      = itemView.findViewById(R.id.profile_info);
            // שים לב: אם ב־item_profile_card אין TextView עם id כזה – או להוסיף אותו, או להוריד את השורה הבאה
            communityText = itemView.findViewById(R.id.profile_community);
        }

        void bind(@Nullable User user) {
            if (user == null) return;

            // שם
            nameText.setText(user.getUserName() != null ? user.getUserName() : "Unknown");

            // מידע כללי – כמות כלבים
            int dogCount = (user.getDogs() != null) ? user.getDogs().size() : 0;
            infoText.setText("Dogs: " + dogCount);

            // קהילה (בדיקה למקרה שה־View לא קיים ב־XML)
            if (communityText != null) {
                communityText.setText(
                        user.getCommunityName() != null ? "Community: " + user.getCommunityName() : "Community: -"
                );
            }
        }
    }
}
