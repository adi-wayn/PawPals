package com.example.pawpals.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Pair;

//import androidx.core.util.Pair;

import model.User;
import model.firebase.Firestore.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class CommunityManagerUtils {

    public interface TransferCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface TransferFlowCallback {
        void onManagerTransferred(String newAdminUid);
        void onCancelled();
    }

    public static void transferManager(Context context, String community, String newAdminUid, TransferCallback callback) {
        // your existing transferManager implementation
    }

    public static void startManagerTransferFlow(
            Context context,
            String currentUserId,
            String currentCommunity,
            TransferFlowCallback callback
    ) {
        UserRepository repo = new UserRepository();
        repo.getUsersByCommunityWithIds(currentCommunity, new UserRepository.FirestoreUsersWithIdsCallback() {
            @Override
            public void onSuccess(List<Pair<String, User>> rows) {
                List<String> memberNames = new ArrayList<>();
                List<String> memberIds = new ArrayList<>();

                for (Pair<String, User> p : rows) {
                    if (!p.first.equals(currentUserId)) { // skip self
                        memberIds.add(p.first);
                        memberNames.add(p.second.getUserName());
                    }
                }

                if (memberNames.isEmpty()) {
                    Toast.makeText(context, "No members available to assign as new manager.", Toast.LENGTH_SHORT).show();
                    callback.onCancelled();
                    return;
                }

                // build spinner + dialog
                Spinner spinner = new Spinner(context);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_spinner_item,
                        memberNames
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Select New Manager");
                builder.setView(spinner);

                builder.setPositiveButton("Confirm", (dialog, which) -> {
                    int pos = spinner.getSelectedItemPosition();
                    if (pos == Spinner.INVALID_POSITION) {
                        callback.onCancelled();
                        return;
                    }

                    String selectedUid = memberIds.get(pos);
                    transferManager(context, currentCommunity, selectedUid, new CommunityManagerUtils.TransferCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onManagerTransferred(selectedUid);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(context, "Failed to transfer manager.", Toast.LENGTH_SHORT).show();
                            callback.onCancelled();
                        }
                    });
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> callback.onCancelled());
                builder.show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Failed to load community members.", Toast.LENGTH_SHORT).show();
                callback.onCancelled();
            }
        });
    }

}
