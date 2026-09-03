/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.views.bypassmethod;

import android.app.Application;
import android.net.Uri;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.elvishew.xlog.XLog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.R;
import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.database.entity.SniEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.ConnectionStrategy;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.services.snichecker.SniCheckerService;
import org.fptn.vpn.views.home.HomeActivityViewModel;
import org.fptn.vpn.services.snichecker.SniCheckerServiceState;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BypassMethodsViewModel extends AndroidViewModel {
    private final String TAG = this.getClass().getSimpleName();

    private final MutableLiveData<String> sniMutableLiveData;
    private final MutableLiveData<BypassCensorshipMethod> bypassCensorshipMethodMutableLiveData;
    private final MutableLiveData<Integer> sniCountLiveData = new MutableLiveData<>(0);

    private final MutableLiveData<SniCheckerServiceState> serviceState = new MutableLiveData<>(SniCheckerServiceState.INACTIVE);

    private final MutableLiveData<String> currentCheckingSniInfo = new MutableLiveData<>("");

    private final MutableLiveData<Pair<Integer, Integer>> currentProgress = new MutableLiveData<>(Pair.create(0, 1));

    private final MutableLiveData<ServerEntity> selectedServer = new MutableLiveData<>(ServerEntity.AUTO);

    private final MutableLiveData<SniSpoofingMode> sniSpoofingModeMutableLiveData;

    private final MutableLiveData<ConnectionStrategy> connectionStrategyMutableLiveData;

    private final MutableLiveData<String> foundedSniEvent = new MutableLiveData<>();

    private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public BypassMethodsViewModel(@NonNull Application application) {
        super(application);

        sniMutableLiveData = new MutableLiveData<>(SharedPrefUtils.getSniHostname(application));
        bypassCensorshipMethodMutableLiveData = new MutableLiveData<>(SharedPrefUtils.getBypassCensorshipMethod(application));
        sniSpoofingModeMutableLiveData = new MutableLiveData<>(SharedPrefUtils.getSniSpoofingMode(application));
        connectionStrategyMutableLiveData = new MutableLiveData<>(SharedPrefUtils.getConnectionStrategy(application));

        refreshSniCount();
    }

    // Written out explicitly (instead of Lombok's @Getter) because Kotlin's Java-interop
    // stub generation runs before the Lombok annotation processor, so Kotlin/Compose call
    // sites can't see a Lombok-generated getter here.
    public MutableLiveData<String> getSniMutableLiveData() {
        return sniMutableLiveData;
    }

    public MutableLiveData<BypassCensorshipMethod> getBypassCensorshipMethodMutableLiveData() {
        return bypassCensorshipMethodMutableLiveData;
    }

    public MutableLiveData<Integer> getSniCountLiveData() {
        return sniCountLiveData;
    }

    public MutableLiveData<SniCheckerServiceState> getServiceState() {
        return serviceState;
    }

    public MutableLiveData<String> getCurrentCheckingSniInfo() {
        return currentCheckingSniInfo;
    }

    public MutableLiveData<Pair<Integer, Integer>> getCurrentProgress() {
        return currentProgress;
    }

    public MutableLiveData<ServerEntity> getSelectedServer() {
        return selectedServer;
    }

    public MutableLiveData<SniSpoofingMode> getSniSpoofingModeMutableLiveData() {
        return sniSpoofingModeMutableLiveData;
    }

    public MutableLiveData<ConnectionStrategy> getConnectionStrategyMutableLiveData() {
        return connectionStrategyMutableLiveData;
    }

    public MutableLiveData<String> getFoundedSniEvent() {
        return foundedSniEvent;
    }

    public String getCurrentSni() {
        return sniMutableLiveData.getValue();
    }

    public void refreshCurrentSni() {
        sniMutableLiveData.postValue(SharedPrefUtils.getSniHostname(getApplication()));
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
        SharedPrefUtils.saveBypassCensorshipMethod(getApplication(), bypassMethod);
    }

    public void setSniSpoofingMode(SniSpoofingMode sniSpoofingMode) {
        sniSpoofingModeMutableLiveData.postValue(sniSpoofingMode);
        SharedPrefUtils.saveSniSpoofingMode(getApplication(), sniSpoofingMode);
    }

    public void setConnectionStrategy(ConnectionStrategy connectionStrategy) {
        connectionStrategyMutableLiveData.postValue(connectionStrategy);
        SharedPrefUtils.saveConnectionStrategy(getApplication(), connectionStrategy);
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
        List<ServerEntity> cached = HomeActivityViewModel.lastPingedServers;
        if (cached != null && !cached.isEmpty()) {
            return Futures.immediateFuture(cached);
        }
        return appDatabase.serverDAO().getServerListAsync(false);
    }

    public void deleteAllSni() {
        executorService.submit(() -> {
            appDatabase.sniDAO().deleteAll();
            int sniCount = appDatabase.sniDAO().count();
            sniCountLiveData.postValue(sniCount);
        });
    }

    public void readFileContent(Uri uri) throws PVNClientException {
        List<SniEntity> sniList = new ArrayList<>();
        try (InputStream inputStream = getApplication().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            reader.lines().forEach(line -> {
                        // Trim whitespace and ignore empty or commented lines
                        String trimmedLine = line.trim();
                        if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                            sniList.add(SniEntity.builder()
                                    .sni(trimmedLine)
                                    .checked(false)
                                    .build());
                        }
                    }
            );
        } catch (Exception e) {
            XLog.tag(TAG).e("Failed to read SNI file: %s", e.getMessage());
            throw new PVNClientException("Error: Could not read the file.");
        }

        if (!sniList.isEmpty()) {
            ListenableFuture<Void> future = appDatabase.sniDAO().insertAll(sniList);
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    XLog.tag(TAG).i("SNI entries inserted to DB [count=%d]", sniList.size());
                    refreshSniCount();
                }

                @Override
                public void onFailure(Throwable t) {
                    XLog.tag(TAG).e("Failed to insert SNI entries to DB: %s", t.getMessage());
                }
            }, executorService);

        } else {
            XLog.tag(TAG).w("No valid SNI entries found in file — aborting insert");
            throw new PVNClientException("File is empty or contains no valid SNI entries.");
        }
    }

    public void subscribeService(SniCheckerService service) {
        service.getServiceState().observeForever(state -> {
            serviceState.postValue(state);
            if (state == SniCheckerServiceState.ACTIVE) {
                bypassCensorshipMethodMutableLiveData.postValue(service.getBypassCensorshipMethod());
            }
        });
        service.getSelectedServer().observeForever(selectedServer::postValue);
        service.getCurrentSniInfo().observeForever(currentCheckingSniInfo::postValue);
        service.getCurrentProgress().observeForever(currentProgress::postValue);

        service.getFoundedSniLiveData().observeForever(sni -> {
            if (sni != null) {
                foundedSniEvent.postValue(sni);
            }
        });
    }

    public void unsubscribe() {
        // todo: check memory leaks and maybe remove observers
    }

    public void loadDefaultSni() throws PVNClientException {
        List<SniEntity> sniList = new ArrayList<>();

        // load global
        try (InputStream inputStream = getApplication().getResources().openRawResource(R.raw.global);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            reader.lines().forEach(line -> {
                        // Trim whitespace and ignore empty or commented lines
                        String trimmedLine = line.trim();
                        if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                            sniList.add(SniEntity.builder()
                                    .sni(trimmedLine)
                                    .checked(false)
                                    .build());
                        }
                    }
            );
        } catch (Exception e) {
            XLog.tag(TAG).e("Failed to read global SNI file: %s", e.getMessage());
            throw new PVNClientException("Error: Could not read the file.");
        }

        // load russia
        try (InputStream inputStream = getApplication().getResources().openRawResource(R.raw.russia);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            reader.lines().forEach(line -> {
                        // Trim whitespace and ignore empty or commented lines
                        String trimmedLine = line.trim();
                        if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                            sniList.add(SniEntity.builder()
                                    .sni(trimmedLine)
                                    .checked(false)
                                    .build());
                        }
                    }
            );
        } catch (Exception e) {
            XLog.tag(TAG).e("Failed to read Russia SNI file: %s", e.getMessage());
            throw new PVNClientException("Error: Could not read the file.");
        }

        // save to db
        if (!sniList.isEmpty()) {
            ListenableFuture<Void> future = appDatabase.sniDAO().insertAll(sniList);
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    XLog.tag(TAG).i("Default SNI entries inserted to DB [count=%d]", sniList.size());
                    refreshSniCount();
                }

                @Override
                public void onFailure(Throwable t) {
                    XLog.tag(TAG).e("Failed to insert default SNI entries to DB: %s", t.getMessage());
                }
            }, executorService);

        } else {
            XLog.tag(TAG).w("No valid SNI entries in default files — aborting insert");
            throw new PVNClientException("File is empty or contains no valid SNI entries.");
        }
    }
}
