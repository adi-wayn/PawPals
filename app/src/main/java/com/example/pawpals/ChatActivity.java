package com.example.pawpals;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pawpals.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import model.Message;
import model.MessagesAdapter;

public class ChatActivity extends AppCompatActivity {

    // UI
    private RecyclerView recyclerView;
    private EditText editMessage;
    private ImageButton btnSend;

    // Data
    private final List<Message> messages = new ArrayList<>();
    private MessagesAdapter adapter;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration registration;

    // current user / chat
    private String currentUserId = "";
    private String currentUserName = "PawPal";
    private @Nullable String chatId = null; // אם יש לכם צ'אטים לפי קהילה, אפשר להעביר ב-Intent

    // נתיב ברירת מחדל; אם יש chatId נשתמש ב-chats/{chatId}/messages
    private static final String COLLECTION_MESSAGES = "messages";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_chat); // זה ה-XML של המסך ששלחת

        // 1) Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            if (auth.getCurrentUser().getDisplayName() != null &&
                    !auth.getCurrentUser().getDisplayName().isEmpty()) {
                currentUserName = auth.getCurrentUser().getDisplayName();
            }
        } else {
            Toast.makeText(this, "Not signed in.", Toast.LENGTH_SHORT).show();
        }

        // קבלת chatId אם הועבר
        if (getIntent() != null && getIntent().hasExtra("chatId")) {
            chatId = getIntent().getStringExtra("chatId");
        }

        // 2) Views
        recyclerView = findViewById(R.id.chat_recycler_view);
        editMessage = findViewById(R.id.edit_text_message);
        btnSend = findViewById(R.id.button_send);

        // 3) RecyclerView + Adapter
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true); // צ'אט מתחיל מלמטה
        recyclerView.setLayoutManager(lm);

        adapter = new MessagesAdapter(messages, currentUserId, this);
        recyclerView.setAdapter(adapter);

        // 4) מאזינים להודעות בזמן אמת
        startListening();

        // 5) שליחה
        btnSend.setOnClickListener(v -> sendMessage());
        editMessage.setOnEditorActionListener((TextView v, int actionId, android.view.KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void startListening() {
        // בוחרים את האוסף: אם יש chatId – עובדים תחת chats/{chatId}/messages, אחרת אוסף messages ראשי
        Query query = (chatId != null && !chatId.isEmpty())
                ? db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                : db.collection(COLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING);

        // מאזין ל-DocumentChange כדי להימנע מ-clear מלא
        registration = query.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;

            for (DocumentChange dc : snap.getDocumentChanges()) {
                Message m = dc.getDocument().toObject(Message.class);
                switch (dc.getType()) {
                    case ADDED:
                        messages.add(m);
                        adapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
                        break;
                    case MODIFIED:
                        // במקרה של עריכה (נדיר בצ'אט) – מעדכנים את הפריט
                        int idxM = dc.getNewIndex();
                        if (idxM >= 0 && idxM < messages.size()) {
                            messages.set(idxM, m);
                            adapter.notifyItemChanged(idxM);
                        }
                        break;
                    case REMOVED:
                        int idxR = dc.getOldIndex();
                        if (idxR >= 0 && idxR < messages.size()) {
                            messages.remove(idxR);
                            adapter.notifyItemRemoved(idxR);
                        }
                        break;
                }
            }
        });
    }

    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        Message msg = new Message(currentUserId, currentUserName, text);
        // timestamp עם @ServerTimestamp ימולא בצד השרת אוטומטית

        if (chatId != null && !chatId.isEmpty()) {
            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(msg)
                    .addOnSuccessListener(ref -> {
                        editMessage.setText("");
                        recyclerView.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
                    });
        } else {
            db.collection(COLLECTION_MESSAGES)
                    .add(msg)
                    .addOnSuccessListener(ref -> {
                        editMessage.setText("");
                        recyclerView.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
                    });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
