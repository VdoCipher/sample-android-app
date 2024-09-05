package com.vdocipher.sampleapp.utils;

import androidx.recyclerview.widget.DiffUtil;

import com.vdocipher.sampleapp.models.MediaItem;

import java.util.List;

public class MediaItemDiffCallback extends DiffUtil.Callback {
    private final List<MediaItem> oldList;
    private final List<MediaItem> newList;

    public MediaItemDiffCallback(List<MediaItem> oldList, List<MediaItem> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).hashCode() == (newList.get(newItemPosition).hashCode());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}

