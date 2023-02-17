package com.vdocipher.sampleapp.tvapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import java.util.List;
import java.util.Random;

public class MainFragment extends BrowseSupportFragment {

    private BackgroundManager mBackgroundManager;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();
    }

    private void loadRows() {
        List<Video> list = VideoList.getList();

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        CardPresenter cardPresenter = new CardPresenter();
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
        HeaderItem header = new HeaderItem(0, VideoList.VIDEO_CATEGORY[0]);
        listRowAdapter.addAll(0, list);
        rowsAdapter.add(new ListRow(header, listRowAdapter));

        CardPresenter cardPresenter1 = new CardPresenter();
        ArrayObjectAdapter listRowAdapter1 = new ArrayObjectAdapter(cardPresenter1);
        HeaderItem header1 = new HeaderItem(1, VideoList.VIDEO_CATEGORY[1]);
        listRowAdapter1.addAll(0, list);
        rowsAdapter.add(new ListRow(header1, listRowAdapter1));

        setAdapter(rowsAdapter);
    }

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(requireActivity());
        mBackgroundManager.attach(requireActivity().getWindow());

        DisplayMetrics mMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        setTitle(getString(R.string.browse_title));
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(ContextCompat.getColor(requireActivity(), R.color.fastlane_background));
        setSearchAffordanceColor(ContextCompat.getColor(requireActivity(), R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(view -> Toast.makeText(getActivity(), "Implement your own in-app search", Toast.LENGTH_LONG).show());

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void updateBackground() {
        Random rnd = new Random();
        int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        mBackgroundManager.setColor(color);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;
                Intent intent;
                if (row.getHeaderItem().getId() == 0) {
                    intent = new Intent(getActivity(), TvPlayerUIActivity.class);
                } else {
                    intent = new Intent(getActivity(), TvPlayerActivity.class);
                }
                intent.putExtra(TvPlayerUIActivity.VIDEO, video);
                requireActivity().startActivity(intent);
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Video) {
                updateBackground();
            }
        }
    }

}