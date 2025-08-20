package model; // או החבילה אצלך

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pawpals.R;

import java.util.ArrayList;
import java.util.List;

public class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.VH> {

    public interface OnRemove { void onRemove(int position); }

    private final List<Uri> data = new ArrayList<>();
    private final OnRemove onRemove;

    public SelectedImagesAdapter(List<Uri> seed, OnRemove onRemove) {
        if (seed != null) data.addAll(seed);
        this.onRemove = onRemove;
        setHasStableIds(true);
    }

    public void submit(List<Uri> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @Override public long getItemId(int position) {
        return data.get(position).hashCode();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_image, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Uri uri = data.get(pos);

        Glide.with(h.img.getContext())
                .load(uri)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_error)
                .into(h.img);

        h.btnRemove.setOnClickListener(v -> {
            if (onRemove != null) onRemove.onRemove(h.getBindingAdapterPosition());
        });
    }

    @Override public int getItemCount() { return data.size(); }

    // חשוב שיהיה public static וגם שהשדות יהיו final
    public static class VH extends RecyclerView.ViewHolder {
        final ImageView img;
        final View btnRemove;

        public VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}