package com.timscdr.eventease.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.timscdr.eventease.repository.OrganizerEventSummary;
import com.timscdr.eventease.repository.OrganizerRepository;
import com.timscdr.eventease.repository.OrganizerStats;
import java.util.List;

public final class OrganizerDashboardViewModel extends ViewModel {
    public final LiveData<List<OrganizerEventSummary>> events;
    public final LiveData<OrganizerStats> stats;
    private final OrganizerRepository repository;
    private final MutableLiveData<String> operationError = new MutableLiveData<>();
    private volatile boolean cleared;
    public OrganizerDashboardViewModel(OrganizerRepository repository) {
        this.repository = repository;
        events = repository.observeEvents(); stats = repository.observeStats();
    }
    public LiveData<String> operationError() { return operationError; }
    public void deleteEvent(long id) {
        operationError.setValue(null);
        repository.deleteEvent(id).whenComplete((unused, failure) -> {
            if (!cleared && failure != null) operationError.postValue("delete");
        });
    }
    @Override protected void onCleared() { cleared = true; }
}
