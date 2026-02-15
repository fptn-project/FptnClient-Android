package org.fptn.vpn.views.bypassmethod;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.R;
import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.database.entity.SniEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.utils.SharedPrefUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;

public class BypassMethodsViewModel extends AndroidViewModel {
    @Getter
    private final MutableLiveData<String> sniMutableLiveData;
    @Getter
    private final MutableLiveData<BypassCensorshipMethod> bypassCensorshipMethodMutableLiveData;
    @Getter
    private final MutableLiveData<Integer> sniCountLiveData = new MutableLiveData<>(0);

    private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public BypassMethodsViewModel(@NonNull Application application) {
        super(application);

        sniMutableLiveData = new MutableLiveData<>(SharedPrefUtils.getSniHostname(application));
        bypassCensorshipMethodMutableLiveData = new MutableLiveData<>(SharedPrefUtils.getBypassCensorshipMethod(application));

        refreshSniCount();
    }

    public String getCurrentSni() {
        return sniMutableLiveData.getValue();
    }

    public void setNewSni(String sni) {
        sniMutableLiveData.postValue(sni);
        SharedPrefUtils.saveSniHostname(getApplication(), sni);
    }

    public void resetToDefault() {
        setNewSni(getApplication().getString(R.string.default_sni));
    }

    public void setBypassMethod(BypassCensorshipMethod bypassMethod) {
        bypassCensorshipMethodMutableLiveData.postValue(bypassMethod);
    }

    public void saveBypassMethod() {
        BypassCensorshipMethod bypassCensorshipMethod = bypassCensorshipMethodMutableLiveData.getValue();
        if (bypassCensorshipMethod != null) {
            SharedPrefUtils.saveBypassCensorshipMethod(getApplication(), bypassCensorshipMethod);
        }
    }

    public void validateAndSetSni(String newSni) {
        String cleanedNewSni = newSni
                .replaceAll("^https?://", "")
                .replaceAll("^ftp://", "")
                .replaceAll("^www\\.", "")
                .replaceAll("[^a-zA-Zа-яА-Я0-9.-]", "")
                .replaceAll("/.*", "")
                .trim()
                .replaceAll("^\\.+|\\.+$", "")
                .toLowerCase();
        if (!cleanedNewSni.isBlank()) {
            setNewSni(newSni);
        }
    }

    public void refreshSniCount() {
        executorService.submit(() -> {
            int sniCount = appDatabase.sniDAO().count();
            sniCountLiveData.postValue(sniCount);
        });
    }

    public ListenableFuture<List<ServerEntity>> getAllServers() {
        return appDatabase.serverDAO().getServerListAsync(false);
    }

    public void deleteAllSni() {
        executorService.submit(() -> {
            appDatabase.sniDAO().deleteAll();
            int sniCount = appDatabase.sniDAO().count();
            sniCountLiveData.postValue(sniCount);
        });
    }

    public ListenableFuture<Void> insertAllSni(List<SniEntity> sniList) {
        return appDatabase.sniDAO().insertAll(sniList);
    }
}
