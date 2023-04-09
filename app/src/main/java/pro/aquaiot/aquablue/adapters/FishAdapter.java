package pro.aquaiot.aquablue.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import pro.aquaiot.aquablue.R;
import pro.aquaiot.aquablue.data.model.Fish;

public class FishAdapter extends RecyclerView.Adapter<FishAdapter.FishViewHolder> {
    private List<Fish> fishes;
    public FishAdapter(List<Fish> fishes) {
        this.fishes = fishes;
    }

    @NonNull
    @Override
    public FishViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.fish_choosing_layout_item, parent, false);
        return new FishViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FishViewHolder holder, int position) {
        holder.textView.setText(fishes.get(position).getFishName());
        Glide.with(holder.itemView.getContext())
                .load(fishes.get(position).getPictureUrl())
                .centerCrop()
                .placeholder(R.drawable.img_placeholder)
                .into(holder.imageView);
        holder.checkBox.setChecked(false);
    }

    @Override
    public int getItemCount() {
        return fishes.size();
    }

    class FishViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        ImageView imageView;
        CheckBox checkBox;
        public FishViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.item_fish_name);
            imageView = itemView.findViewById(R.id.item_fish_image);
            checkBox = itemView.findViewById(R.id.item_fish_cb);
        }

    }
}
