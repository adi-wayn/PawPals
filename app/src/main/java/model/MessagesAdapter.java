package model;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pawpals.R;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import model.firebase.Firestore.CommunityRepository;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private final String currentUserId;
    private final Context context;
    private final CommunityRepository repo;
    private String communityId;
    private boolean isManager;

    public MessagesAdapter(List<Message> messageList,
                           String currentUserId,
                           Context context,
                           String communityId,
                           boolean isManager) {
        this.messageList = (messageList != null) ? messageList : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.context = context;
        this.communityId = communityId;
        this.isManager = isManager;
        this.repo = new CommunityRepository();
    }

    public MessagesAdapter(List<Message> messageList,
                           String currentUserId,
                           Context context) {
        this(messageList, currentUserId, context, null, false);
    }

    public void setCommunityData(String communityId, boolean isManager) {
        this.communityId = communityId;
        this.isManager = isManager;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_chat, parent, false);
        return new MessageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message msg = messageList.get(position);
        boolean isOutgoing = msg.getSenderId() != null && msg.getSenderId().equals(currentUserId);

        // שם השולח
        holder.textSenderName.setText(isOutgoing ? "Me" : msg.getSenderName());

        // טקסט ההודעה
        holder.textMessageBody.setText(msg.getText());

        // הצגת השעה (אם קיימת)
        if (msg.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.textMessageTime.setText(sdf.format(msg.getTimestamp()));
        } else {
            holder.textMessageTime.setText("");
        }

        // כיווניות
        holder.rootItem.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        holder.messageContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        // יישור צדדים – שלך לשמאל, אחרים לימין
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageContainer.getLayoutParams();
        if (isOutgoing) {
            params.gravity = Gravity.END;
            holder.messageContainer.setGravity(Gravity.START);
            holder.bubble.setBackgroundResource(R.drawable.bubble_outgoing);
            holder.buttonDelete.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        } else {
            params.gravity = Gravity.START;
            holder.messageContainer.setGravity(Gravity.END);
            holder.bubble.setBackgroundResource(R.drawable.bubble_incoming);
            holder.buttonDelete.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
        holder.messageContainer.setLayoutParams(params);

        // צבע בועה
        int bubbleColor = ContextCompat.getColor(context,
                isOutgoing ? R.color.bubble_outgoing : R.color.bubble_incoming);
        holder.bubble.setCardBackgroundColor(bubbleColor);

        // צבע טקסט
        int textColor = ContextCompat.getColor(context,
                isOutgoing ? android.R.color.white : android.R.color.black);
        holder.textMessageBody.setTextColor(textColor);

        // צבע שעה
        int timeColor = ContextCompat.getColor(context,
                isOutgoing ? R.color.bubble_time_light : android.R.color.darker_gray);
        holder.textMessageTime.setTextColor(timeColor);

        // כפתור מחיקה
        if ((isManager || isOutgoing) && communityId != null) {
            holder.buttonDelete.setVisibility(View.VISIBLE);
            holder.buttonDelete.setOnClickListener(v -> {
                if (msg.getId() == null || msg.getId().isEmpty()) {
                    Toast.makeText(context, "Cannot delete: missing message id", Toast.LENGTH_SHORT).show();
                    return;
                }
                repo.deleteMessage(communityId, msg.getId(), new CommunityRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess(String ignored) {
                        Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        } else {
            holder.buttonDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void setMessages(List<Message> newList) {
        this.messageList = (newList != null) ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addMessage(Message m) {
        if (m == null) return;
        this.messageList.add(m);
        notifyItemInserted(messageList.size() - 1);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout rootItem;
        LinearLayout messageContainer;
        TextView textSenderName;
        MaterialCardView bubble;
        TextView textMessageBody;
        TextView textMessageTime;
        ImageButton buttonDelete;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            rootItem = itemView.findViewById(R.id.root_message_item);
            messageContainer = itemView.findViewById(R.id.message_container);
            textSenderName = itemView.findViewById(R.id.text_sender_name);
            bubble = itemView.findViewById(R.id.bubble);
            textMessageBody = itemView.findViewById(R.id.text_message_body);
            textMessageTime = itemView.findViewById(R.id.text_message_time);
            buttonDelete = itemView.findViewById(R.id.button_delete_message);
        }
    }
}