package com.timscdr.eventease.util;

import android.widget.ImageView;
import android.util.Base64;
import com.bumptech.glide.Glide;
import com.timscdr.eventease.R;
import com.timscdr.eventease.data.entity.Event;
import java.util.Locale;

/** Provides bundled category artwork as the fallback for events without a custom photo. */
public final class EventBannerImages {
    private EventBannerImages() { }
    public static int forEvent(Event event) {
        return forCategory(event == null ? null : event.category);
    }

    public static void load(ImageView view, Event event) {
        load(view, event == null ? null : event.bannerImageUrl, event == null ? null : event.category);
    }

    public static void load(ImageView view, String image, String category) {
        int fallback = forCategory(category);
        Glide.with(view).clear(view);
        if (image == null || image.trim().isEmpty()) {
            view.setImageResource(fallback);
            return;
        }
        if (image.startsWith(EventImageEncoder.DATA_URI_PREFIX)) {
            try {
                byte[] bytes = Base64.decode(image.substring(EventImageEncoder.DATA_URI_PREFIX.length()), Base64.DEFAULT);
                Glide.with(view).load(bytes).centerCrop().placeholder(fallback).fallback(fallback).error(fallback).into(view);
            } catch (IllegalArgumentException invalidImage) {
                view.setImageResource(fallback);
            }
            return;
        }
        Glide.with(view).load(image).centerCrop().placeholder(fallback).fallback(fallback).error(fallback).into(view);
    }

    public static int forCategory(String value) {
        String category = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        switch (category) {
            case "technical": return R.drawable.event_photo_technical;
            case "cultural": return R.drawable.event_photo_cultural;
            case "sports": return R.drawable.event_photo_sports;
            case "workshop": return R.drawable.event_photo_workshop;
            case "seminar": return R.drawable.event_photo_seminar;
            default: return R.drawable.event_banner_placeholder;
        }
    }
}
