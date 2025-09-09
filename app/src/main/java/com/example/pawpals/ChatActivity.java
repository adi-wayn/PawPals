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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import model.Message;
import model.MessagesAdapter;
import model.User;
import model.firebase.firestore.CommunityRepository;

public class ChatActivity extends AppCompatActivity {

    // UI
    private RecyclerView recyclerView;
    private EditText editMessage;
    private ImageButton btnSend;

    // Data
    private final List<Message> messages = new ArrayList<>();
    private MessagesAdapter adapter;

    // Firebase/Repo
    private FirebaseAuth auth;
    private CommunityRepository repo;
    private ListenerRegistration registration;

    // current user / community
    private String currentUserId = "";
    private String currentUserName;
    private @Nullable String communityId = null;
    private @Nullable User currentUser = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_chat);

        // --- 1) קבלת המשתמש ושם הקהילה מה-Intent ---
        currentUser = getIntent().getParcelableExtra("currentUser");
        if (currentUser == null || TextUtils.isEmpty(currentUser.getCommunityName())) {
            Toast.makeText(this, "Missing user or community name", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserName = currentUser.getUserName();
        final String communityName = currentUser.getCommunityName();

        // --- 2) Firebase/Auth + Repo ---
        auth = FirebaseAuth.getInstance();
        repo = new CommunityRepository();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            if (!TextUtils.isEmpty(auth.getCurrentUser().getDisplayName())) {
                currentUserName = auth.getCurrentUser().getDisplayName();
            }
        }

        // --- 3) Views + RecyclerView ---
        recyclerView = findViewById(R.id.chat_recycler_view);
        editMessage = findViewById(R.id.edit_text_message);
        btnSend = findViewById(R.id.button_send);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);

        // כרגע עדיין אין לנו communityId, לכן נטען עם adapter זמני
        adapter = new MessagesAdapter(messages, currentUserId, this);
        recyclerView.setAdapter(adapter);

        // --- 4) נביא את ה-communityId ונעדכן את ה-adapter ---
        repo.getCommunityIdByName(communityName, new CommunityRepository.FirestoreIdCallback() {
            @Override
            public void onSuccess(String id) {
                communityId = id;

                // יצירת adapter חדש עם כל הנתונים (כולל isManager)
                adapter = new MessagesAdapter(
                        messages,
                        currentUserId,
                        ChatActivity.this,
                        communityId,
                        currentUser.isManager() // ✅ כאן המנהל מקבל הרשאה למחיקה
                );
                recyclerView.setAdapter(adapter);

                loadMessagesAndListen();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ChatActivity.this,
                        "Failed to find community id: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // --- 5) שליחה (כפתור או מקש Send במקלדת) ---
        btnSend.setOnClickListener(v -> sendMessageViaRepo());
        editMessage.setOnEditorActionListener((TextView v, int actionId, android.view.KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessageViaRepo();
                return true;
            }
            return false;
        });
    }

    private void loadMessagesAndListen() {
        if (TextUtils.isEmpty(communityId)) return;

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager lm = (LinearLayoutManager) layoutManager;
            lm.setReverseLayout(false);
            lm.setStackFromEnd(true);
        }

        if (registration != null) {
            registration.remove();
            registration = null;
        }

        final boolean[] firstBatch = {true};

        registration = repo.listenToMessagesStream(
                communityId,
                new CommunityRepository.FirestoreMessagesChangeCallback() {
                    @Override
                    public void onChanges(List<DocumentChange> changes) {
                        if (changes == null || changes.isEmpty()) return;

                        if (firstBatch[0]) {
                            messages.clear();
                            firstBatch[0] = false;
                        }

                        for (DocumentChange dc : changes) {
                            Message m = dc.getDocument().toObject(Message.class);
                            switch (dc.getType()) {
                                case ADDED:
                                    messages.add(m);
                                    adapter.notifyItemInserted(messages.size() - 1);
                                    recyclerView.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
                                    break;
                                case MODIFIED:
                                    int idxM = dc.getNewIndex();
                                    if (idxM >= 0 && idxM < messages.size()) {
                                        messages.set(idxM, m);
                                        adapter.notifyItemChanged(idxM);
                                    } else {
                                        messages.add(m);
                                        adapter.notifyItemInserted(messages.size() - 1);
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
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(ChatActivity.this,
                                "Stream error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void sendMessageViaRepo() {
        if (TextUtils.isEmpty(communityId)) return;

        String text = editMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        Message msg = new Message(communityId, currentUserId, currentUserName, text);

        repo.createMessage(communityId, msg, new CommunityRepository.FirestoreCallback() {
            @Override
            public void onSuccess(String documentId) {
                editMessage.setText("");
                recyclerView.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ChatActivity.this,
                        "Send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
