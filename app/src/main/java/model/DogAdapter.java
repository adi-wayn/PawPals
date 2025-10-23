package model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pawpals.R;

import java.util.ArrayList;
import java.util.List;

public class DogAdapter extends RecyclerView.Adapter<DogAdapter.DogViewHolder> {

    public interface OnDogActionListener {
        void onDogClick(@NonNull Dog dog);      // קליק רגיל – פתח כרטיס כלב
        void onDeleteDog(@NonNull Dog dog);     // לחיצה ארוכה – מחיקה (אופציונלי)
    }

    private final LayoutInflater inflater;
    private final List<Dog> dogs;
    private final OnDogActionListener listener;

    public DogAdapter(@NonNull Context context, List<Dog> dogs, OnDogActionListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.dogs = (dogs != null) ? new ArrayList<>(dogs) : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<Dog> newDogs) {
        this.dogs.clear();
        if (newDogs != null) this.dogs.addAll(newDogs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_dog_card, parent, false);
        return new DogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DogViewHolder holder, int position) {
        final Dog dog = dogs.get(position);

        String name  = dog != null ? dog.getName()  : null;
        String breed = dog != null ? dog.getBreed() : null;
        Integer age  = dog != null ? dog.getAge()   : null;

        holder.dogName.setText(name != null ? name : "");
        holder.dogBreed.setText("Breed: " + (breed != null ? breed : ""));
        holder.dogAge.setText("Age: " + (age != null ? age : "—"));

        // ✅ טען תמונה עם Glide
        if (dog != null && dog.getPhotoUrl() != null && !dog.getPhotoUrl().isEmpty()) {
            Glide.with(holder.dogProfilePicture.getContext())
                    .load(dog.getPhotoUrl())
                    .placeholder(R.drawable.rex_image) // תמונה ברירת מחדל
                    .centerCrop()
                    .into(holder.dogProfilePicture);
        } else {
            holder.dogProfilePicture.setImageResource(R.drawable.rex_image);
        }

        // קליק רגיל -> פתיחת פרטים
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && dog != null) listener.onDogClick(dog);
        });

        // לחיצה ארוכה -> מחיקה
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null && dog != null) {
                listener.onDeleteDog(dog);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return dogs.size();
    }

    static class DogViewHolder extends RecyclerView.ViewHolder {
        ImageView dogProfilePicture;
        TextView dogName, dogBreed, dogAge;

        DogViewHolder(@NonNull View itemView) {
            super(itemView);
            dogProfilePicture = itemView.findViewById(R.id.dog_profile_picture);
            dogName  = itemView.findViewById(R.id.dog_name);
            // בלייאאוט שלך השדה נקרא dog_breed1; נוסיף fallback ל-dog_breed אם קיים לייאאוט אחר
            dogBreed = itemView.findViewById(R.id.dog_breed);
//            TextView breed1 = itemView.findViewById(R.id.dog_breed);
//            dogBreed = (breed1 != null) ? breed1 : itemView.findViewById(R.id.dog_breed);
            dogAge   = itemView.findViewById(R.id.dog_age);
        }
    }
}
