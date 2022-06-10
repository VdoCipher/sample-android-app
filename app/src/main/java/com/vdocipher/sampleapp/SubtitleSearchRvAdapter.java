package com.vdocipher.sampleapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.vdocipher.aegis.player.internal.subtitle.SubtitleCue;

import java.util.ArrayList;
import java.util.List;

public class SubtitleSearchRvAdapter extends RecyclerView.Adapter<SubtitleSearchRvAdapter.ViewHolder> {

    private final LayoutInflater layoutInflater;
    private final List<SubtitleCue> subtitleCues = new ArrayList<>();
    private final Callbacks callbacks;

    public SubtitleSearchRvAdapter(Context context, Callbacks callbacks) {
        super();
        layoutInflater = LayoutInflater.from(context);
        this.callbacks = callbacks;
    }

    public void setSubtitleCues(List<SubtitleCue> subtitleCues) {
        this.subtitleCues.clear();
        this.subtitleCues.addAll(subtitleCues);
        notifyDataSetChanged();
    }

    public void clearResults() {
        this.subtitleCues.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.subtile_search_result_view, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubtitleCue subtitleCue = subtitleCues.get(position);

        long milliseconds = subtitleCue.getStartTime();
        long seconds = (milliseconds / 1000) % 60;
        long minutes = ((milliseconds / (1000 * 60)) % 60);
        long hours = ((milliseconds / (1000 * 60 * 60)) % 24);

        holder.tvSubtitle.setText(String.format("%s", subtitleCue.getText()));
        if (hours > 0) {
            holder.tvStartTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        } else {
            holder.tvStartTime.setText(String.format("%02d:%02d", minutes, seconds));
        }
        holder.rootLayout.setOnClickListener(v -> callbacks.onSubtitleSelected(subtitleCue));
    }

    @Override
    public int getItemCount() {
        return subtitleCues.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatTextView tvStartTime, tvSubtitle;
        private final LinearLayout rootLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStartTime = itemView.findViewById(R.id.tv_start_time);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            rootLayout = itemView.findViewById(R.id.ll_root);
        }
    }

    public interface Callbacks {
        void onSubtitleSelected(SubtitleCue subtitleCue);
    }
}
