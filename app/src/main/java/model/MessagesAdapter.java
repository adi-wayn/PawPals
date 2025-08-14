package model;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pawpals.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import model.Message;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private List<Message> messageList;
    private final String currentUserId;
    private final Context context;

    public MessagesAdapter(List<Message> messageList, String currentUserId, Context context) {
        // כדי להימנע מ-NullPointer ולתמוך בהוספות דינמיות
        this.messageList = (messageList != null) ? messageList : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.context = context;
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

        // שם שולח + גוף הודעה
        holder.textSenderName.setText(isOutgoing ? "Me" : msg.getSenderName());
        holder.textMessageBody.setText(msg.getText());

        // יישור לפי סוג ההודעה (ימין = יוצאת, שמאל = נכנסת)
        holder.rootItem.setGravity(isOutgoing ? Gravity.END : Gravity.START);

        // צבע רקע של הבועה + צבע טקסט
        int bubbleColor = ContextCompat.getColor(context,
                isOutgoing ? R.color.bubble_outgoing : R.color.bubble_incoming);
        holder.bubble.setCardBackgroundColor(bubbleColor);

        int textColor = ContextCompat.getColor(context,
                isOutgoing ? android.R.color.white : android.R.color.black);
        holder.textMessageBody.setTextColor(textColor);

        // (אופציונלי) לחיצה ארוכה להעתקה
        holder.itemView.setOnLongClickListener(v -> {
            // אפשר להוסיף כאן העתקה ללוח/תפריט
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ----- עוזרים נוחים לניהול הרשימה -----

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
        LinearLayout rootItem;              // root_message_item
        TextView textSenderName;            // text_sender_name
        MaterialCardView bubble;            // bubble
        TextView textMessageBody;           // text_message_body

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            rootItem = itemView.findViewById(R.id.root_message_item);
            textSenderName = itemView.findViewById(R.id.text_sender_name);
            bubble = itemView.findViewById(R.id.bubble);
            textMessageBody = itemView.findViewById(R.id.text_message_body);
        }
    }
}
