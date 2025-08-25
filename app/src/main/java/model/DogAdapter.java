package model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pawpals.R;

import java.util.List;

public class DogAdapter extends RecyclerView.Adapter<DogAdapter.DogViewHolder> {

    private Context context;
    private List<Dog> dogs;
    private OnDogActionListener listener;

    // ממשק לפעולות (מחיקה, עריכה וכו')
    public interface OnDogActionListener {
        void onDeleteDog(Dog dog);
    }

    public DogAdapter(Context context, List<Dog> dogs, OnDogActionListener listener) {
        this.context = context;
        this.dogs = dogs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dog_card, parent, false);
        return new DogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DogViewHolder holder, int position) {
        Dog dog = dogs.get(position);

        holder.dogName.setText(dog.getName());
        holder.dogBreed.setText(dog.getBreed());
        holder.dogAge.setText("גיל: " + dog.getAge());

        // כפתור התפריט (שלוש נקודות)
        holder.menuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.menuButton);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_dog_item, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete_dog) {
                    if (listener != null) {
                        listener.onDeleteDog(dog);
                    } else {
                        Toast.makeText(context, "מחק כלב: " + dog.getName(), Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return dogs.size();
    }

    public static class DogViewHolder extends RecyclerView.ViewHolder {
        ImageView dogProfilePicture;
        TextView dogName, dogBreed, dogAge;
        ImageButton menuButton;

        public DogViewHolder(@NonNull View itemView) {
            super(itemView);
            dogProfilePicture = itemView.findViewById(R.id.dog_profile_picture);
            dogName = itemView.findViewById(R.id.dog_name);
            dogBreed = itemView.findViewById(R.id.dog_breed1);
            dogAge = itemView.findViewById(R.id.dog_age);
            menuButton = itemView.findViewById(R.id.btn_dog_menu);
        }
    }
}
