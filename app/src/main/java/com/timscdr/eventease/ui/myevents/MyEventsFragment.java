package com.timscdr.eventease.ui.myevents;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.timscdr.eventease.R;
import com.timscdr.eventease.ui.profile.ProfileFragment;
import com.timscdr.eventease.ui.registration.TicketQRActivity;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.MyEventsState;
import com.timscdr.eventease.viewmodel.MyEventsViewModel;

public final class MyEventsFragment extends Fragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MyEventsViewModel model;
    private MyEventsState latest;
    private RecyclerView list;
    private MyEventsAdapter adapter;
    private final Runnable refresh = () -> { if (model != null) model.refreshClock(); };
    public MyEventsFragment() { super(R.layout.fragment_my_events_pending); }

    public static MyEventsFragment create(long studentId) {
        MyEventsFragment fragment = new MyEventsFragment();
        Bundle args = new Bundle(); args.putLong(ProfileFragment.STUDENT_ID, studentId);
        fragment.setArguments(args); return fragment;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long studentId = requireArguments().getLong(ProfileFragment.STUDENT_ID);
        model = new ViewModelProvider(this, new EventEaseViewModelFactory(requireContext(), studentId))
                .get(MyEventsViewModel.class);
        list = view.findViewById(R.id.my_events_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MyEventsAdapter(eventId -> {
            if (latest == null) return;
            Long registrationId = latest.registrationIds.get(eventId);
            if (registrationId != null) startActivity(TicketQRActivity.intent(requireContext(), eventId,
                    studentId, registrationId));
        });
        list.setAdapter(adapter);
        model.state().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(MyEventsState state) {
        latest = state;
        adapter.show(state);
        boolean empty = !state.loading && state.upcoming.isEmpty() && state.past.isEmpty();
        requireView().findViewById(R.id.my_events_empty).setVisibility(empty ? View.VISIBLE : View.GONE);
        requireView().findViewById(R.id.my_events_progress).setVisibility(state.loading ? View.VISIBLE : View.GONE);
        list.setVisibility(state.loading ? View.INVISIBLE : View.VISIBLE);
        schedule();
    }

    private void schedule() {
        handler.removeCallbacks(refresh);
        if (getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
            handler.postDelayed(refresh, model.nextRefreshDelay());
    }
    @Override public void onResume() { super.onResume(); if (model != null) model.refreshClock(); schedule(); }
    @Override public void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
    @Override public void onDestroyView() {
        handler.removeCallbacks(refresh);
        if (list != null) list.setAdapter(null);
        list = null; adapter = null;
        super.onDestroyView();
    }
}
