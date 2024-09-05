package com.vdocipher.sampleapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.vdocipher.sampleapp.databinding.MediaItemBinding;
import com.vdocipher.sampleapp.models.MediaItem;
import com.vdocipher.sampleapp.utils.MediaItemDiffCallback;
import com.vdocipher.sampleapp.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface MediaItemSelected {
        void onMediaSelected(int position);
    }

    private final MediaItemSelected mediaItemSelected;

    private List<MediaItem> items;
    private static final String TAG = "VideoAdapter";

    public VideoAdapter(MediaItemSelected mediaItemSelected) {
        this.mediaItemSelected = mediaItemSelected;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MediaItemBinding binding = MediaItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VideoHolder(binding, mediaItemSelected);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof VideoHolder) {
            ((VideoHolder) holder).bind(items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<MediaItem> newMediaItems) {
        if (items == null) {
            items = new ArrayList<>(newMediaItems);
            notifyItemRangeInserted(0, newMediaItems.size());
        } else {
            DiffUtil.Callback diffCallback = new MediaItemDiffCallback(items, newMediaItems);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

            items.clear();
            items.addAll(newMediaItems);

            diffResult.dispatchUpdatesTo(this);
        }
    }

    static class VideoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final MediaItemBinding binding;
        private final MediaItemSelected mediaItemSelected;

        public VideoHolder(MediaItemBinding binding, MediaItemSelected mediaItemSelected) {
            super(binding.getRoot());
            this.binding = binding;
            this.mediaItemSelected = mediaItemSelected;
            this.binding.playButton.setOnClickListener(this);
        }

        public void bind(MediaItem mediaItem) {
            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.cast_album_art_placeholder)
                    .error(R.drawable.cast_album_art_placeholder);

            Glide.with(itemView.getContext())
                    .applyDefaultRequestOptions(requestOptions)
                    .load(mediaItem.poster)
                    .into((android.widget.ImageView) binding.ivMediaPoster);
            binding.mediaTitle.setText(mediaItem.title);
            binding.mediaDuration.setText(String.format("Duration : %s", Utils.convertSecondsToHMS(mediaItem.duration)));
            if(mediaItem.isPlaying){
                binding.playButton.setImageDrawable(ResourcesCompat.getDrawable(binding.playButton.getResources(), R.drawable.ic_playing_media_24px, null));
            }
            else{
                binding.playButton.setImageDrawable(ResourcesCompat.getDrawable(binding.playButton.getResources(), R.drawable.ic_play_arrow_24px, null));
            }
        }

        @Override
        public void onClick(View view) {
            int position = getBindingAdapterPosition();
            if (view == binding.playButton) {
                mediaItemSelected.onMediaSelected(position);
            }
        }
    }
}