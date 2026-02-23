package org.fptn.vpn.views.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.entity.ServerEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;

public class SettingsViewModel extends AndroidViewModel {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());

    @Getter
    private final MutableLiveData<List<ServerEntity>> serverDtoListLiveData = new MutableLiveData<>(List.of());

    public SettingsViewModel(@NonNull Application application) {
        super(application);
    }

    public void deleteAllServers() {
        executorService.submit(() -> appDatabase.serverDAO().deleteAll());
    }

    public void loadServersList() {
        executorService.submit(() -> serverDtoListLiveData.postValue(appDatabase.serverDAO().getServerList()));
    }
}
