package model;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pawpals.R;

import java.util.ArrayList;
import java.util.List;

class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageVH> {
    private final List<String> urls = new ArrayList<>();
    private final int maxToShow;

    ImagesAdapter() { this(Integer.MAX_VALUE); }
    ImagesAdapter(int maxToShow) { this.maxToShow = maxToShow; }

    void submit(@Nullable List<String> data) {
        urls.clear();
        if (data != null) urls.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post_image, parent, false);
        return new ImageVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageVH h, int pos) {
        String url = urls.get(pos);
        Glide.with(h.img.getContext())
                .load(url)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_error)
                .into(h.img);

        int extra = urls.size() - maxToShow;
        if (maxToShow != Integer.MAX_VALUE && pos == maxToShow - 1 && extra > 0) {
            h.overlayMore.setVisibility(View.VISIBLE);
            h.overlayMore.setText("+" + extra);
        } else {
            h.overlayMore.setVisibility(View.GONE);
        }

        // אופציונלי: קליק לפתיחת גלריה במסך מלא
        // h.itemView.setOnClickListener(v -> openGallery(v.getContext(), urls, pos));
    }

    @Override public int getItemCount() {
        return Math.min(urls.size(), maxToShow);
    }

    static class ImageVH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView overlayMore;
        ImageVH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img);
            overlayMore = itemView.findViewById(R.id.overlay_more);
        }
    }
}
