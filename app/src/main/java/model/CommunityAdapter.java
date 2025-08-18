package model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pawpals.R;

import java.util.ArrayList;
import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private List<User> users;
    @Nullable private OnUserClickListener clickListener;

    public CommunityAdapter(List<User> users) {
        this.users = (users != null) ? users : new ArrayList<>();
    }

    public CommunityAdapter(List<User> users, @Nullable OnUserClickListener listener) {
        this.users = (users != null) ? users : new ArrayList<>();
        this.clickListener = listener;
    }

    public void setOnUserClickListener(@Nullable OnUserClickListener listener) {
        this.clickListener = listener;
    }

    public void updateData(List<User> newUsers) {
        this.users = (newUsers != null) ? newUsers : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public CommunityAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CommunityAdapter.ViewHolder holder, int position) {
        final User user = users.get(position);
        holder.bind(user);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null && user != null) {
                clickListener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return (users != null) ? users.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, infoText, communityText;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.profile_name);
            infoText = itemView.findViewById(R.id.profile_info);
            communityText = itemView.findViewById(R.id.profile_community); // נוסיף ל־XML
        }

        void bind(User user) {
            if (user == null) return;

            // שם
            nameText.setText(user.getUserName() != null ? user.getUserName() : "Unknown");

            // מידע כללי – כמות כלבים
            int dogCount = (user.getDogs() != null) ? user.getDogs().size() : 0;
            infoText.setText("Dogs: " + dogCount);

            // קהילה
            communityText.setText(user.getCommunityName() != null
                    ? "Community: " + user.getCommunityName()
                    : "Community: -");
        }
    }
}
