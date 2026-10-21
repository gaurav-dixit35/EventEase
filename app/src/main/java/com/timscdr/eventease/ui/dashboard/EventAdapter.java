package com.timscdr.eventease.ui.dashboard;

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
import com.timscdr.eventease.viewmodel.EventListState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;

public final class EventAdapter extends ListAdapter<EventAdapter.Row, EventAdapter.Holder> {
    private final LongConsumer open;
    public EventAdapter(LongConsumer open) {
        super(new DiffUtil.ItemCallback<Row>() {
            @Override public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) { return a.event.id == b.event.id; }
            @Override public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                return a.registered == b.registered && a.event.dateTime == b.event.dateTime
                        && a.event.title.equals(b.event.title) && a.event.category.equals(b.event.category)
                        && a.event.venue.equals(b.event.venue) && Objects.equals(a.event.bannerImageUrl, b.event.bannerImageUrl);
            }
        });
        this.open = open;
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }
    public void show(EventListState state) {
        List<Row> rows = new ArrayList<>();
        for (Event event : state.events) rows.add(new Row(event, state.registeredIds.contains(event.id)));
        submitList(rows);
    }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Row row = getItem(position);
        Event event = row.event;
        holder.title.setText(event.title);
        holder.category.setText(event.category);
        holder.date.setText(DateTimeUtils.dateTime(holder.itemView.getContext(), event.dateTime));
        holder.venue.setText(event.venue);
        holder.badge.setVisibility(row.registered ? View.VISIBLE : View.GONE);
        holder.button.setVisibility(row.registered ? View.GONE : View.VISIBLE);
        holder.button.setOnClickListener(view -> open.accept(event.id));
        holder.itemView.setOnClickListener(view -> open.accept(event.id));
        EventBannerImages.load(holder.banner, event);
    }
    @Override public void onViewRecycled(@NonNull Holder holder) {
        // RecyclerView can recycle rows while the owning Activity is being destroyed.
        Glide.with(holder.itemView.getContext().getApplicationContext()).clear(holder.banner);
        super.onViewRecycled(holder);
    }
    static final class Row {
        final Event event;
        final boolean registered;
        Row(Event event, boolean registered) { this.event = event; this.registered = registered; }
    }
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title, category, date, venue, badge;
        final ImageView banner;
        final MaterialButton button;
        Holder(View view) {
            super(view);
            title = view.findViewById(R.id.event_title); category = view.findViewById(R.id.event_category);
            date = view.findViewById(R.id.event_date); venue = view.findViewById(R.id.event_venue);
            badge = view.findViewById(R.id.registered_badge); banner = view.findViewById(R.id.event_banner);
            button = view.findViewById(R.id.quick_register);
        }
    }
}
