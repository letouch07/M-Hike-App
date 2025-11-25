package com.example.mhike.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mhike.R;
import com.example.mhike.model.Hike;

import java.util.List;

public class HikeAdapter extends RecyclerView.Adapter<HikeAdapter.HikeViewHolder> {

    private List<Hike> hikeList;
    private Context context;
    private OnItemClickListener listener; // Interface for click handling

    // Interface for handling click events on list items
    public interface OnItemClickListener {
        void onItemClick(Hike hike);
    }

    public HikeAdapter(Context context, List<Hike> hikeList, OnItemClickListener listener) {
        this.context = context;
        this.hikeList = hikeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HikeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single list item
        View view = LayoutInflater.from(context).inflate(R.layout.item_hike, parent, false);
        return new HikeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HikeViewHolder holder, int position) {
        // Get the Hike object at the current position
        Hike currentHike = hikeList.get(position);

        // Bind data to the TextViews
        holder.nameTextView.setText(currentHike.getName());
        holder.locationTextView.setText(currentHike.getLocation());
        holder.dateTextView.setText("Date: " + currentHike.getDate());

        // Set the click listener on the entire item view
        holder.itemView.setOnClickListener(v -> listener.onItemClick(currentHike));
    }

    @Override
    public int getItemCount() {
        return hikeList.size();
    }

    // Update the list data and notify the adapter
    public void setHikeList(List<Hike> newList) {
        this.hikeList = newList;
        notifyDataSetChanged();
    }

    // ViewHolder class holds references to the views in each item
    public static class HikeViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView locationTextView;
        public TextView dateTextView;

        public HikeViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textViewHikeName);
            locationTextView = itemView.findViewById(R.id.textViewHikeLocation);
            dateTextView = itemView.findViewById(R.id.textViewHikeDate);
        }
    }
}