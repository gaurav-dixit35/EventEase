package com.timscdr.eventease.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.timscdr.eventease.R;
import android.content.Intent;
import com.timscdr.eventease.viewmodel.AuthViewModel;
import com.timscdr.eventease.viewmodel.EventEaseViewModelFactory;
import com.timscdr.eventease.viewmodel.ProfileViewModel;
import java.util.Locale;

public final class ProfileFragment extends Fragment {
    public static final String STUDENT_ID = "student_id";
    public ProfileFragment() { super(R.layout.fragment_profile); }

    public static ProfileFragment create(long studentId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putLong(STUDENT_ID, studentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long studentId = requireArguments().getLong(STUDENT_ID);
        EventEaseViewModelFactory factory = new EventEaseViewModelFactory(requireContext(), studentId);
        ProfileViewModel model = new ViewModelProvider(this, factory).get(ProfileViewModel.class);
        AuthViewModel auth = new ViewModelProvider(requireActivity(), factory).get(AuthViewModel.class);
        model.profile.observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) { auth.restoreSession(); return; }
            ((TextView) view.findViewById(R.id.student_name)).setText(profile.name);
            ((TextView) view.findViewById(R.id.student_roll)).setText(getString(R.string.profile_roll, profile.rollNo));
            ((TextView) view.findViewById(R.id.student_course)).setText(getString(R.string.course_year, profile.course, profile.year));
            ((TextView) view.findViewById(R.id.student_email)).setText(profile.email);
            ((TextView) view.findViewById(R.id.student_college)).setText(getString(R.string.profile_college, profile.collegeName));
            String name = profile.name.trim();
            String initial = name.isEmpty() ? "?" : name.substring(0, name.offsetByCodePoints(0, 1));
            ((TextView) view.findViewById(R.id.initial)).setText(initial.toUpperCase(Locale.ROOT));
        });
        model.registrations.observe(getViewLifecycleOwner(), count ->
                ((TextView) view.findViewById(R.id.registration_count)).setText(getString(R.string.registration_count, count == null ? 0 : count)));
        model.attended.observe(getViewLifecycleOwner(), count ->
                ((TextView) view.findViewById(R.id.attendance_count)).setText(getString(R.string.attendance_count, count == null ? 0 : count)));
        view.findViewById(R.id.logout).setOnClickListener(button -> auth.logout());
        view.findViewById(R.id.open_settings).setOnClickListener(button -> startActivity(
                new Intent(requireContext(), SettingsActivity.class).putExtra(SettingsActivity.STUDENT_ID, studentId)));
    }
}
