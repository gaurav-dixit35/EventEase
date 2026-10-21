package com.timscdr.eventease.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.timscdr.eventease.R;
import com.timscdr.eventease.ui.event.EventDetailActivity;
import com.timscdr.eventease.ui.profile.ProfileFragment;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.EventListViewModel;

public final class EventListFragment extends Fragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private EventListViewModel model;
    private final Runnable refresh = () -> { if (model != null) model.refresh(); };
    public EventListFragment() { super(R.layout.fragment_event_list); }
    public static EventListFragment create(long studentId) {
        EventListFragment fragment = new EventListFragment();
        Bundle args = new Bundle(); args.putLong(ProfileFragment.STUDENT_ID, studentId);
        fragment.setArguments(args); return fragment;
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        model = new ViewModelProvider(this, new EventEaseViewModelFactory(requireContext(),
                requireArguments().getLong(ProfileFragment.STUDENT_ID))).get(EventListViewModel.class);
        EventAdapter adapter = new EventAdapter(id -> startActivity(new Intent(requireContext(), EventDetailActivity.class)
                .putExtra(EventDetailActivity.EVENT_ID, id)));
        RecyclerView list = view.findViewById(R.id.event_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext())); list.setAdapter(adapter);
        SwipeRefreshLayout swipe = view.findViewById(R.id.swipe_refresh);
        swipe.setColorSchemeColors(getResources().getColor(R.color.accent, null));
        swipe.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.surface, null));
        swipe.setOnRefreshListener(model::refresh);
        CategoryFilterView categories = view.findViewById(R.id.category_filters);
        categories.select(model.category()); categories.onSelection(model::category);
        SearchView search = view.findViewById(R.id.event_search);
        search.setQuery(model.search(), false);
        search.setVisibility(model.searchOpen() ? View.VISIBLE : View.GONE);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { search.clearFocus(); return true; }
            @Override public boolean onQueryTextChange(String query) { model.search(query); return true; }
        });
        MaterialToolbar toolbar = view.findViewById(R.id.events_toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.action_search) return false;
            boolean open = !model.searchOpen(); model.setSearchOpen(open);
            search.setVisibility(open ? View.VISIBLE : View.GONE);
            if (open) search.requestFocus(); else { search.setQuery("", false); search.clearFocus(); }
            return true;
        });
        view.findViewById(R.id.load_samples).setOnClickListener(button -> model.loadSamples());
        if (!model.supportsSampleEvents()) view.findViewById(R.id.load_samples).setVisibility(View.GONE);
        view.findViewById(R.id.list_retry).setOnClickListener(button -> model.refresh());
        model.state().observe(getViewLifecycleOwner(), state -> {
            adapter.show(state); swipe.setRefreshing(state.loading);
            boolean empty = state.events.isEmpty() && !state.loading && !state.error;
            view.findViewById(R.id.list_feedback).setVisibility(empty || state.error ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.empty_list).setVisibility(empty ? View.VISIBLE : View.GONE);
            ((TextView) view.findViewById(R.id.empty_list)).setText(state.catalogEmpty
                    ? (model.supportsSampleEvents() ? R.string.catalog_empty : R.string.catalog_empty_cloud)
                    : R.string.no_matching_events);
            view.findViewById(R.id.list_error).setVisibility(state.error ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.list_retry).setVisibility(state.error ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.load_samples).setVisibility(empty && state.catalogEmpty
                    && model.supportsSampleEvents() ? View.VISIBLE : View.GONE);
            schedule();
        });
    }
    private void schedule() {
        handler.removeCallbacks(refresh);
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED))
            handler.postDelayed(refresh, model.nextRefreshDelay());
    }
    @Override public void onResume() { super.onResume(); model.refresh(); schedule(); }
    @Override public void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
    @Override public void onDestroyView() {
        handler.removeCallbacks(refresh);
        ((RecyclerView) requireView().findViewById(R.id.event_list)).setAdapter(null);
        super.onDestroyView();
    }
}
