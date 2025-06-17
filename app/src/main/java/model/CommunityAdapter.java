package model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pawpals.R;
import java.util.ArrayList;
import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    private List<User> users;

    public CommunityAdapter(List<User> users) {
        this.users = (users != null) ? users : new ArrayList<>();
    }

    public void updateData(List<User> newUsers) {
        users = (newUsers != null) ? newUsers : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public CommunityAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CommunityAdapter.ViewHolder holder, int position) {
        User user = users.get(position);
        holder.nameText.setText(user.getUserName());

        int dogCount = (user.getDogs() != null) ? user.getDogs().size() : 0;
        holder.infoText.setText("Dogs: " + dogCount);
    }

    @Override
    public int getItemCount() {
        return (users != null) ? users.size() : 0;
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
