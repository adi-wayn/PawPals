package com.example.pawpals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    private List<UserProfile> users;

    public CommunityAdapter(List<UserProfile> users) {
        this.users = users;
    }

    public void updateData(List<UserProfile> newUsers) {
        users = newUsers;
        notifyDataSetChanged();
    }

    @Override
    public CommunityAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CommunityAdapter.ViewHolder holder, int position) {
        UserProfile user = users.get(position);
        holder.nameText.setText(user.getName());
        holder.infoText.setText("Dogs: " + user.getDogCount() + (user.isFriend() ? " | Friend" : ""));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, infoText;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.profile_name);
            infoText = itemView.findViewById(R.id.profile_info);
        }
    }
}
