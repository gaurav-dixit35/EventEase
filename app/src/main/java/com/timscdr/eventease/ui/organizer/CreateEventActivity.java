package com.timscdr.eventease.ui.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import com.timscdr.eventease.util.DateTimeUtils;
import com.timscdr.eventease.util.EventBannerImages;
import com.timscdr.eventease.viewmodel.CreateEventViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import java.util.Calendar;

public final class CreateEventActivity extends AppCompatActivity {
    private static final String DATE_TIME = "event_date_time", BANNER_URI = "event_banner_uri";
    public static final String EVENT_ID = "organizer_event_id";
    private CreateEventViewModel model;
    private long eventDateTime;
    private long eventId;
    private int registeredCount;
    private Uri bannerUri;
    private String existingBannerImage;
    private final ActivityResultLauncher<String[]> imagePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (SecurityException ignored) { }
                bannerUri = uri;
                ((TextView) findViewById(R.id.banner_selection)).setText(R.string.event_image_selected);
                previewBanner((AutoCompleteTextView) findViewById(R.id.event_category_input));
            });
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_create_event);
        MaterialToolbar toolbar = findViewById(R.id.create_event_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        eventId = getIntent().getLongExtra(EVENT_ID, 0);
        boolean editing = eventId > 0;
        if (editing) {
            registeredCount = getIntent().getIntExtra("event_registered_count", 0);
            toolbar.setTitle(R.string.organizer_edit_event);
            ((MaterialButton) findViewById(R.id.submit_event)).setText(R.string.organizer_save_changes);
            if (registeredCount > 0) ((TextInputLayout) findViewById(R.id.event_capacity_layout)).setHelperText(
                    getResources().getQuantityString(R.plurals.organizer_capacity_minimum,
                            registeredCount, registeredCount));
            fillFromIntent();
        }
        model = new ViewModelProvider(this, new EventEaseViewModelFactory(this, 0)).get(CreateEventViewModel.class);
        AutoCompleteTextView category = findViewById(R.id.event_category_input);
        category.setAdapter(ArrayAdapter.createFromResource(this, R.array.event_categories,
                android.R.layout.simple_dropdown_item_1line));
        category.setOnClickListener(v -> category.showDropDown());
        category.setOnItemClickListener((parent, selected, position, id) -> previewBanner(category));
        if (state != null) {
            eventDateTime = state.getLong(DATE_TIME, eventDateTime);
            String savedUri = state.getString(BANNER_URI);
            if (savedUri != null) bannerUri = Uri.parse(savedUri);
        }
        previewBanner(category);
        updateDateLabel();
        findViewById(R.id.choose_event_datetime).setOnClickListener(v -> chooseDateTime());
        findViewById(R.id.choose_event_banner).setOnClickListener(v -> imagePicker.launch(new String[]{"image/*"}));
        findViewById(R.id.submit_event).setOnClickListener(v -> {
            String title = text(R.id.event_title_input), selectedCategory = category.getText().toString();
            String description = text(R.id.event_description_input), venue = text(R.id.event_venue_input);
            String capacity = text(R.id.event_capacity_input);
            if (editing && registeredCount > 0) {
                try {
                    if (Integer.parseInt(capacity.trim()) < registeredCount) {
                        TextView message = findViewById(R.id.create_event_message);
                        message.setText(getResources().getQuantityString(R.plurals.organizer_capacity_below_attendees,
                                registeredCount, registeredCount));
                        message.setVisibility(View.VISIBLE);
                        return;
                    }
                } catch (NumberFormatException ignored) { }
            }
            String image = bannerUri != null ? bannerUri.toString() : existingBannerImage;
            if (editing) model.update(eventId, title, selectedCategory, description, venue, eventDateTime, image, capacity);
            else model.create(title, selectedCategory, description, venue, eventDateTime, image, capacity);
        });
        model.state().observe(this, value -> {
            findViewById(R.id.event_saving).setVisibility(value.saving ? View.VISIBLE : View.GONE);
            findViewById(R.id.submit_event).setEnabled(!value.saving);
            TextView message = findViewById(R.id.create_event_message);
            message.setVisibility(value.error == null ? View.GONE : View.VISIBLE);
            if (value.error != null) message.setText(errorText(value.error));
            if (value.created) {
                Toast.makeText(this, value.updated ? R.string.event_updated : R.string.event_created, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private String text(int id) { return ((EditText) findViewById(id)).getText().toString(); }
    private void fillFromIntent() {
        ((EditText) findViewById(R.id.event_title_input)).setText(getIntent().getStringExtra("event_title"));
        AutoCompleteTextView category = findViewById(R.id.event_category_input);
        category.setText(getIntent().getStringExtra("event_category"), false);
        ((EditText) findViewById(R.id.event_description_input)).setText(getIntent().getStringExtra("event_description"));
        ((EditText) findViewById(R.id.event_venue_input)).setText(getIntent().getStringExtra("event_venue"));
        ((EditText) findViewById(R.id.event_capacity_input)).setText(String.valueOf(getIntent().getIntExtra("event_capacity", 1)));
        eventDateTime = getIntent().getLongExtra("event_date_time_value", 0);
        existingBannerImage = getIntent().getStringExtra("event_banner_url");
        if (existingBannerImage != null && !existingBannerImage.isEmpty())
            ((TextView) findViewById(R.id.banner_selection)).setText(R.string.event_image_already_selected);
    }
    public static Intent editIntent(android.content.Context context, Event event, int registeredCount) {
        return new Intent(context, CreateEventActivity.class).putExtra(EVENT_ID, event.id)
                .putExtra("event_title", event.title).putExtra("event_category", event.category)
                .putExtra("event_description", event.description).putExtra("event_venue", event.venue)
                .putExtra("event_capacity", event.capacity).putExtra("event_registered_count", registeredCount)
                .putExtra("event_date_time_value", event.dateTime)
                .putExtra("event_banner_url", event.bannerImageUrl);
    }
    private String errorText(String error) {
        if ("title".equals(error)) return getString(R.string.create_event_title_required);
        if ("category".equals(error)) return getString(R.string.create_event_category_required);
        if ("description".equals(error)) return getString(R.string.create_event_description_required);
        if ("venue".equals(error)) return getString(R.string.create_event_venue_required);
        if ("date".equals(error)) return getString(R.string.create_event_date_required);
        if ("capacity".equals(error)) return getString(R.string.create_event_capacity_required);
        if ("image".equals(error)) return getString(R.string.event_image_prepare_error);
        return getString(R.string.create_event_save_error);
    }
    private void chooseDateTime() {
        Calendar calendar = Calendar.getInstance();
        if (eventDateTime > 0) calendar.setTimeInMillis(eventDateTime);
        new DatePickerDialog(this, (picker, year, month, day) -> {
            calendar.set(Calendar.YEAR, year); calendar.set(Calendar.MONTH, month); calendar.set(Calendar.DAY_OF_MONTH, day);
            new TimePickerDialog(this, (timePicker, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0);
                eventDateTime = calendar.getTimeInMillis(); updateDateLabel();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void updateDateLabel() {
        ((MaterialButton) findViewById(R.id.choose_event_datetime)).setText(eventDateTime <= 0
                ? getString(R.string.choose_event_date) : DateTimeUtils.dateTime(this, eventDateTime));
    }
    private void previewBanner(AutoCompleteTextView category) {
        String value = category.getText().toString().trim();
        ImageView preview = findViewById(R.id.event_banner_preview);
        int fallback = EventBannerImages.forCategory(value);
        if (bannerUri != null) {
            Glide.with(this).load(bannerUri).centerCrop().placeholder(fallback).error(fallback).into(preview);
        } else {
            EventBannerImages.load(preview, existingBannerImage, value);
            ((TextView) findViewById(R.id.banner_selection)).setText(existingBannerImage == null
                    ? R.string.event_image_category_default : R.string.event_image_already_selected);
        }
    }
    @Override protected void onPostResume() {
        super.onPostResume();
        previewBanner((AutoCompleteTextView) findViewById(R.id.event_category_input));
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putLong(DATE_TIME, eventDateTime);
        if (bannerUri != null) out.putString(BANNER_URI, bannerUri.toString());
        super.onSaveInstanceState(out);
    }
}
