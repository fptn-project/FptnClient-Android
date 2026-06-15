package org.fptn.vpn.views.perappvpn;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.dao.AppInfoDAO;
import org.fptn.vpn.database.entity.AppInfoEntity;
import org.fptn.vpn.enums.PerAppVpnMode;
import org.fptn.vpn.utils.SharedPrefUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;

public class PerAppVpnModeViewModel extends AndroidViewModel {

    @Getter
    private final MutableLiveData<PerAppVpnMode> perAppVpnModeMutableLiveData;

    @Getter
    private final MutableLiveData<List<AppInfo>> appListMutableLiveData;

    private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());

    private List<AppInfo> allLoadedApps = new ArrayList<>();

    @Getter
    private boolean showSystemApps;

    public PerAppVpnModeViewModel(@NonNull Application application) {
        super(application);

        showSystemApps = SharedPrefUtils.getShowSystemApps(application);
        perAppVpnModeMutableLiveData = new MutableLiveData<>(SharedPrefUtils.getPerAppVPNMode(application));

        appListMutableLiveData = new MutableLiveData<>(List.of());
    }

    public void setPerAppVpnMode(PerAppVpnMode perAppVpnMode) {
        perAppVpnModeMutableLiveData.postValue(perAppVpnMode);
    }

    public void setShowSystemApps(boolean show) {
        showSystemApps = show;
        List<AppInfo> filtered = allLoadedApps.stream()
                .filter(app -> showSystemApps || !app.isSystemApp())
                .collect(Collectors.toList());
        appListMutableLiveData.postValue(filtered);
    }

    public void loadInstalledApps(PackageManager pm) {
        new Thread(() -> {
            List<AppInfoEntity> savedApps = appDatabase.appInfoDAO().getAll();

            Map<String, AppInfo> savedAppsMap = new HashMap<>();
            for (AppInfoEntity entity : savedApps) {
                AppInfo appInfo = AppInfo.builder()
                        .packageName(entity.getPackageName())
                        .allowed(entity.isAllowed())
                        .disallowed(entity.isDisallowed())
                        .build();
                savedAppsMap.put(entity.getPackageName(), appInfo);
            }

            List<AppInfo> apps = new ArrayList<>();
            List<ApplicationInfo> packages = getApplication().getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

            String thisAppPackageName = getApplication().getPackageName();

            for (ApplicationInfo appInfo : packages) {
                if (thisAppPackageName.equalsIgnoreCase(appInfo.packageName)) continue;

                AppInfo app = savedAppsMap.getOrDefault(appInfo.packageName, AppInfo.builder().packageName(appInfo.packageName).build());
                app.setIcon(appInfo.loadIcon(pm));
                app.setLabel(appInfo.loadLabel(pm).toString());
                app.setSystemApp(pm.getLaunchIntentForPackage(appInfo.packageName) == null);

                apps.add(app);
            }

            Collections.sort(apps, (a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));
            allLoadedApps = apps;
            List<AppInfo> filtered = apps.stream()
                    .filter(app -> showSystemApps || !app.isSystemApp())
                    .collect(Collectors.toList());
            appListMutableLiveData.postValue(filtered);
        }).start();
    }

    public boolean hasSelectedApps() {
        PerAppVpnMode mode = perAppVpnModeMutableLiveData.getValue();
        if (mode == PerAppVpnMode.ONLY_ALLOWED) {
            return allLoadedApps.stream().anyMatch(AppInfo::isAllowed);
        }
        return true;
    }

    public void saveAllSettings() {
        savePerAppVpnMode();
        saveSelectedApps();
        SharedPrefUtils.saveShowSystemApps(getApplication(), showSystemApps);
    }

    private void savePerAppVpnMode() {
        PerAppVpnMode perAppVPNMode = perAppVpnModeMutableLiveData.getValue();
        if (perAppVPNMode != null) {
            SharedPrefUtils.savePerAppVPNMode(getApplication(), perAppVPNMode);
        }
    }

    public void saveSelectedApps() {
        PerAppVpnMode perAppVPNMode = perAppVpnModeMutableLiveData.getValue();
        // save app list only if selected mode
        if (perAppVPNMode == PerAppVpnMode.EXCEPT_DISALLOWED
                || perAppVPNMode == PerAppVpnMode.ONLY_ALLOWED) {
            new Thread(() -> {
                List<AppInfo> appInfoList = getAppListMutableLiveData().getValue();
                if (appInfoList != null && !appInfoList.isEmpty()) {
                    List<AppInfoEntity> entities = appInfoList.stream()
                            .map(app -> AppInfoEntity.builder()
                                    .packageName(app.getPackageName())
                                    .allowed(app.isAllowed())
                                    .disallowed(app.isDisallowed())
                                    .build()
                            )
                            .collect(Collectors.toList());
                    AppInfoDAO appInfoDAO = appDatabase.appInfoDAO();
                    appInfoDAO.deleteAll(); // delete all previous records
                    appInfoDAO.insertAll(entities);
                }
            }).start();
        }
    }
}
