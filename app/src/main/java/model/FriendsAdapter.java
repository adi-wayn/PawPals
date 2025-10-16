package model;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pawpals.OtherUserProfileActivity;
import com.example.pawpals.R;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.VH> {

    public interface OnFriendClick {
        void onClick(User user);
    }

    private final Context ctx;
    private final List<User> data;
    private final OnFriendClick clickCb;

    public FriendsAdapter(@NonNull Context ctx, @NonNull List<User> data, @NonNull OnFriendClick clickCb) {
        this.ctx = ctx;
        this.data = data;
        this.clickCb = clickCb;
        setHasStableIds(false);
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar, chevron;
        TextView name, subtitle;
        VH(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.friend_avatar);
            name = itemView.findViewById(R.id.friend_name);
            subtitle = itemView.findViewById(R.id.friend_subtitle);
            chevron = itemView.findViewById(R.id.friend_chevron);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        User u = data.get(pos);
        String name = nn(u.getUserName());
        String sub = !isEmpty(u.getCommunityName()) ? u.getCommunityName()
                : nn(u.getContactDetails());

        h.name.setText(name.isEmpty() ? "(ללא שם)" : name);
        h.subtitle.setText(sub);

        View.OnClickListener nav = v -> clickCb.onClick(u);
        h.itemView.setOnClickListener(nav);
        if (h.chevron != null) h.chevron.setOnClickListener(nav);
    }

    @Override public int getItemCount() { return data != null ? data.size() : 0; }

    // ===== helpers =====
    private static String nn(String s) { return s == null ? "" : s; }
    private static boolean isEmpty(String s) { return s == null || s.isEmpty(); }

    // 🧭 ניווט ברירת-מחדל: מעבר למסך פרופיל של משתמש אחר עם ה-ID שלו
    public static OnFriendClick defaultNavigator(Context ctx) {
        return user -> {
            if (user == null) return;
            String uid = user.getUid();
            if (uid == null || uid.isEmpty()) return;

            Intent i = new Intent(ctx, OtherUserProfileActivity.class);
            i.putExtra(OtherUserProfileActivity.EXTRA_OTHER_USER_ID, uid);

            // אם ה-Context אינו Activity (למשל Application), צריך FLAG_ACTIVITY_NEW_TASK
            if (!(ctx instanceof android.app.Activity)) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            ctx.startActivity(i);
        };
    }
}
