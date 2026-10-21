package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.data.entity.StudentProfile;
import com.timscdr.eventease.repository.ProfileRepository;
import com.timscdr.eventease.repository.RegistrationRepository;

public final class ProfileViewModel extends ViewModel {
    public final LiveData<StudentProfile> profile;
    public final LiveData<Integer> registrations;
    public final LiveData<Integer> attended;

    public ProfileViewModel(ProfileRepository profiles, RegistrationRepository tickets, long studentId) {
        profile = profiles.observeById(studentId);
        registrations = tickets.observeRegistrationCount(studentId);
        attended = tickets.observeAttendanceCount(studentId);
    }
}
