package com.timscdr.eventease.ui.myevents;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.util.EventBannerImages;
import com.timscdr.eventease.viewmodel.MyEventsState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;

final class MyEventsAdapter extends ListAdapter<MyEventsAdapter.Row, RecyclerView.ViewHolder> {
    private static final int SECTION = 0, EVENT = 1;
    private final LongConsumer openTicket;
    MyEventsAdapter(LongConsumer openTicket) {
        super(new DiffUtil.ItemCallback<Row>() {
            @Override public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
                if (a.type != b.type) return false;
                return a.type == SECTION ? a.section == b.section : a.event.id == b.event.id;
            }
            @Override public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                if (a.type == SECTION) return a.section == b.section;
                return a.event.title.equals(b.event.title) && a.event.category.equals(b.event.category)
                        && a.event.dateTime == b.event.dateTime && a.event.venue.equals(b.event.venue)
                        && Objects.equals(a.event.bannerImageUrl, b.event.bannerImageUrl);
            }
        });
        this.openTicket = openTicket;
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    void show(MyEventsState state) {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.section(R.string.upcoming_my_events));
        for (Event event : state.upcoming) rows.add(Row.event(event));
        rows.add(Row.section(R.string.past_events));
        for (Event event : state.past) rows.add(Row.event(event));
        submitList(rows);
    }

    @Override public int getItemViewType(int position) { return getItem(position).type; }
    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        if (type == SECTION) return new SectionHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_myevents_section, parent, false));
        return new EventHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = getItem(position);
        if (holder instanceof SectionHolder) {
            ((SectionHolder) holder).title.setText(row.section);
            return;
        }
        EventHolder item = (EventHolder) holder;
        Event event = row.event;
        item.title.setText(event.title);
        item.category.setText(event.category);
        item.date.setText(DateTimeUtils.dateTime(item.itemView.getContext(), event.dateTime));
        item.venue.setText(event.venue);
        item.badge.setText(R.string.open_qr_ticket);
        item.badge.setVisibility(View.VISIBLE);
        item.button.setVisibility(View.GONE);
        item.itemView.setContentDescription(item.itemView.getContext().getString(R.string.open_ticket_for, event.title));
        item.itemView.setOnClickListener(view -> openTicket.accept(event.id));
        EventBannerImages.load(item.banner, event);
    }
    @Override public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof EventHolder) {
            EventHolder event = (EventHolder) holder;
            Glide.with(event.itemView.getContext().getApplicationContext()).clear(event.banner);
        }
        super.onViewRecycled(holder);
    }

    static final class Row {
        final int type, section; final Event event;
        private Row(int type, int section, Event event) { this.type = type; this.section = section; this.event = event; }
        static Row section(int title) { return new Row(SECTION, title, null); }
        static Row event(Event value) { return new Row(EVENT, 0, value); }
    }
    private static final class SectionHolder extends RecyclerView.ViewHolder {
        final TextView title;
        SectionHolder(View view) { super(view); title = view.findViewById(R.id.section_title); }
    }
    private static final class EventHolder extends RecyclerView.ViewHolder {
        final TextView title, category, date, venue, badge; final ImageView banner; final MaterialButton button;
        EventHolder(View view) {
            super(view); title = view.findViewById(R.id.event_title); category = view.findViewById(R.id.event_category);
            date = view.findViewById(R.id.event_date); venue = view.findViewById(R.id.event_venue);
            badge = view.findViewById(R.id.registered_badge); banner = view.findViewById(R.id.event_banner);
            button = view.findViewById(R.id.quick_register);
        }
    }
}
