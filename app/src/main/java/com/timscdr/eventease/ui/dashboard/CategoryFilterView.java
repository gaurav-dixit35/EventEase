package com.timscdr.eventease.ui.dashboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.HorizontalScrollView;
import com.google.android.material.chip.ChipGroup;
import com.timscdr.eventease.R;
import java.util.function.Consumer;

/** All controls are inflated from XML; this class only maps selection to a category. */
public final class CategoryFilterView extends HorizontalScrollView {
    private final ChipGroup group;
    private final int[] ids = {R.id.category_all, R.id.category_technical, R.id.category_cultural,
            R.id.category_sports, R.id.category_workshop, R.id.category_seminar};
    private final String[] values = {null, "Technical", "Cultural", "Sports", "Workshop", "Seminar"};
    public CategoryFilterView(Context context, AttributeSet attributes) {
        super(context, attributes);
        setHorizontalScrollBarEnabled(false);
        LayoutInflater.from(context).inflate(R.layout.view_category_filter, this, true);
        group = findViewById(R.id.category_group);
    }
    public void select(String category) {
        for (int i = 0; i < ids.length; i++) if (java.util.Objects.equals(values[i], category)) group.check(ids[i]);
    }
    public void onSelection(Consumer<String> listener) {
        group.setOnCheckedStateChangeListener((chips, checked) -> {
            if (checked.isEmpty()) return;
            for (int i = 0; i < ids.length; i++) if (checked.get(0) == ids[i]) listener.accept(values[i]);
        });
    }
}
