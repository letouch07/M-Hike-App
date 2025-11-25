package com.example.mhike.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mhike.R;
import com.example.mhike.model.Observation;

import java.util.List;

public class ObservationAdapter extends RecyclerView.Adapter<ObservationAdapter.ObservationViewHolder> {

    private Context context;
    private List<Observation> observationList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Observation observation);
        void onDeleteClick(Observation observation);
    }

    public ObservationAdapter(Context context, List<Observation> observationList, OnItemClickListener listener) {
        this.context = context;
        this.observationList = observationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ObservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_observation, parent, false);
        return new ObservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ObservationViewHolder holder, int position) {
        Observation obs = observationList.get(position);

        // 1. Set Text
        holder.textViewDetail.setText(obs.getObservationDetail());
        holder.textViewTime.setText(obs.getTimeOfObservation());

        // 2. Set Comment (Show only if exists)
        if (obs.getAdditionalComments() != null && !obs.getAdditionalComments().trim().isEmpty()) {
            holder.textViewComment.setText(obs.getAdditionalComments());
            holder.textViewComment.setVisibility(View.VISIBLE);
        } else {
            holder.textViewComment.setVisibility(View.GONE);
        }

        // 3. Set Image (CRITICAL PART: Load from file)
        if (obs.getImagePath() != null && !obs.getImagePath().isEmpty()) {
            Bitmap bitmap = BitmapFactory.decodeFile(obs.getImagePath());
            if (bitmap != null) {
                holder.imageViewThumbnail.setImageBitmap(bitmap);
                holder.imageViewThumbnail.setVisibility(View.VISIBLE);
            } else {
                holder.imageViewThumbnail.setVisibility(View.GONE);
            }
        } else {
            holder.imageViewThumbnail.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return observationList.size();
    }

    public class ObservationViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDetail, textViewTime, textViewComment;
        ImageView imageViewThumbnail;

        public ObservationViewHolder(@NonNull View itemView) {
            super(itemView);
            // Must match IDs in item_observation.xml
            textViewDetail = itemView.findViewById(R.id.textViewObsDetail);
            textViewTime = itemView.findViewById(R.id.textViewObsTime);
            textViewComment = itemView.findViewById(R.id.textViewObsComment);
            imageViewThumbnail = itemView.findViewById(R.id.imageViewThumbnail);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(observationList.get(getAdapterPosition()));
                }
            });
        }
    }
}