package com.timscdr.eventease.ui.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.timscdr.eventease.R;
import com.timscdr.eventease.repository.AttendeeRecord;
import com.timscdr.eventease.util.DateTimeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.Holder> {
    private List<AttendeeRecord> visible = new ArrayList<>();
    private List<AttendeeRecord> all = new ArrayList<>();
    private String query = "";
    void submit(List<AttendeeRecord> rows) { all = rows == null ? new ArrayList<>() : new ArrayList<>(rows); filter(query); }
    void filter(String value) {
        query = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        List<AttendeeRecord> next = new ArrayList<>();
        for (AttendeeRecord row : all) {
            String haystack = (row.name + " " + row.email + " " + row.rollNo + " " + row.collegeName).toLowerCase(Locale.ROOT);
            if (haystack.contains(query)) next.add(row);
        }
        DiffUtil.DiffResult changes = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return visible.size(); }
            @Override public int getNewListSize() { return next.size(); }
            @Override public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return Objects.equals(visible.get(oldPosition).registrationCode,
                        next.get(newPosition).registrationCode);
            }
            @Override public boolean areContentsTheSame(int oldPosition, int newPosition) {
                AttendeeRecord oldRow = visible.get(oldPosition), newRow = next.get(newPosition);
                return Objects.equals(oldRow.name, newRow.name) && Objects.equals(oldRow.rollNo, newRow.rollNo)
                        && Objects.equals(oldRow.email, newRow.email)
                        && Objects.equals(oldRow.collegeName, newRow.collegeName)
                        && oldRow.registeredAt == newRow.registeredAt && oldRow.checkedIn == newRow.checkedIn;
            }
        });
        visible = next;
        changes.dispatchUpdatesTo(this);
    }
    List<AttendeeRecord> all() { return new ArrayList<>(all); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendee, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        AttendeeRecord row = visible.get(position);
        holder.name.setText(row.name);
        holder.identity.setText(holder.itemView.getContext().getString(R.string.attendee_details, row.rollNo, row.email));
        holder.college.setText(holder.itemView.getContext().getString(R.string.attendee_college, row.collegeName));
        holder.status.setText(row.checkedIn ? R.string.attendee_status_checked_in : R.string.attendee_status_registered);
        holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                row.checkedIn ? R.color.success : R.color.text_secondary));
        holder.registeredAt.setText(DateTimeUtils.dateTime(holder.itemView.getContext(), row.registeredAt));
    }
    @Override public int getItemCount() { return visible.size(); }
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name, identity, college, status, registeredAt;
        Holder(View item) {
            super(item); name = item.findViewById(R.id.attendee_name); identity = item.findViewById(R.id.attendee_identity);
            college = item.findViewById(R.id.attendee_college); status = item.findViewById(R.id.attendee_status);
            registeredAt = item.findViewById(R.id.attendee_registration_time);
        }
    }
}
