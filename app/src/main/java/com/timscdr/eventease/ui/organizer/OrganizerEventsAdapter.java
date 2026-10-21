package com.timscdr.eventease.ui.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.timscdr.eventease.R;
import com.timscdr.eventease.repository.OrganizerEventSummary;
import com.timscdr.eventease.util.DateTimeUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class OrganizerEventsAdapter extends ListAdapter<OrganizerEventSummary, OrganizerEventsAdapter.Holder> {
    interface Actions {
        void attendees(OrganizerEventSummary event);
        void edit(OrganizerEventSummary event);
        void delete(OrganizerEventSummary event);
    }

    private final Actions actions;

    OrganizerEventsAdapter(Actions actions) {
        super(new DiffUtil.ItemCallback<OrganizerEventSummary>() {
            @Override public boolean areItemsTheSame(@NonNull OrganizerEventSummary a,
                    @NonNull OrganizerEventSummary b) {
                return a.event.id == b.event.id;
            }

            @Override public boolean areContentsTheSame(@NonNull OrganizerEventSummary a,
                    @NonNull OrganizerEventSummary b) {
                return a.registeredCount == b.registeredCount && a.checkedInCount == b.checkedInCount
                        && a.event.title.equals(b.event.title) && a.event.category.equals(b.event.category)
                        && a.event.description.equals(b.event.description) && a.event.venue.equals(b.event.venue)
                        && a.event.dateTime == b.event.dateTime && a.event.capacity == b.event.capacity
                        && a.event.organizerName.equals(b.event.organizerName)
                        && Objects.equals(a.event.bannerImageUrl, b.event.bannerImageUrl);
            }
        });
        this.actions = actions;
    }

    void submit(List<OrganizerEventSummary> values) {
        submitList(values == null ? Collections.emptyList() : new ArrayList<>(values));
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_event, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        OrganizerEventSummary row = getItem(position);
        holder.title.setText(row.event.title);
        android.content.Context context = holder.itemView.getContext();
        holder.meta.setText(context.getString(R.string.organizer_event_meta, row.event.category,
                DateTimeUtils.dateTime(context, row.event.dateTime), row.event.venue));
        holder.registrations.setText(context.getResources().getQuantityString(R.plurals.organizer_event_capacity,
                row.registeredCount, row.registeredCount, row.event.capacity));
        holder.checkIns.setText(context.getResources().getQuantityString(R.plurals.organizer_event_checkins,
                row.checkedInCount, row.checkedInCount));
        holder.attendees.setOnClickListener(v -> actions.attendees(row));
        holder.edit.setOnClickListener(v -> actions.edit(row));
        holder.delete.setOnClickListener(v -> actions.delete(row));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title, meta, registrations, checkIns;
        final MaterialButton attendees, edit, delete;
        Holder(View item) {
            super(item);
            title = item.findViewById(R.id.organizer_event_name);
            meta = item.findViewById(R.id.organizer_event_meta);
            registrations = item.findViewById(R.id.organizer_event_registrations);
            checkIns = item.findViewById(R.id.organizer_event_checkins);
            attendees = item.findViewById(R.id.manage_attendees);
            edit = item.findViewById(R.id.edit_event);
            delete = item.findViewById(R.id.delete_event);
        }
    }
}
