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

package org.fptn.vpn.views.perappvpn;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.dao.AppInfoDAO;
import org.fptn.vpn.utils.AppExclusion;
import org.fptn.vpn.database.entity.AppInfoEntity;
import org.fptn.vpn.enums.PerAppVpnMode;
import org.fptn.vpn.utils.SharedPrefUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PerAppVpnModeViewModel extends AndroidViewModel {

    private final MutableLiveData<PerAppVpnMode> perAppVpnModeMutableLiveData;

    private final MutableLiveData<List<AppInfo>> appListMutableLiveData;

    private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());

    private List<AppInfo> allLoadedApps = new ArrayList<>();

    private boolean showSystemApps;

    public PerAppVpnModeViewModel(@NonNull Application application) {
        super(application);

        showSystemApps = SharedPrefUtils.getShowSystemApps(application);
        perAppVpnModeMutableLiveData = new MutableLiveData<>(SharedPrefUtils.getPerAppVPNMode(application));

        appListMutableLiveData = new MutableLiveData<>(List.of());
    }

    // Written out explicitly (instead of Lombok's @Getter) because Kotlin's Java-interop
    // stub generation runs before the Lombok annotation processor, so Kotlin/Compose call
    // sites can't see a Lombok-generated getter here.
    public MutableLiveData<PerAppVpnMode> getPerAppVpnModeMutableLiveData() {
        return perAppVpnModeMutableLiveData;
    }

    public MutableLiveData<List<AppInfo>> getAppListMutableLiveData() {
        return appListMutableLiveData;
    }

    public boolean isShowSystemApps() {
        return showSystemApps;
    }

    public void setPerAppVpnMode(PerAppVpnMode perAppVpnMode) {
        perAppVpnModeMutableLiveData.postValue(perAppVpnMode);
        SharedPrefUtils.savePerAppVPNMode(getApplication(), perAppVpnMode);
    }

    public void setShowSystemApps(boolean show) {
        showSystemApps = show;
        SharedPrefUtils.saveShowSystemApps(getApplication(), show);
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
            AppExclusion exclusion = new AppExclusion(getApplication());

            for (ApplicationInfo appInfo : packages) {
                if (thisAppPackageName.equalsIgnoreCase(appInfo.packageName)) continue;

                AppInfo app = savedAppsMap.getOrDefault(appInfo.packageName, AppInfo.builder().packageName(appInfo.packageName).build());
                app.setIcon(appInfo.loadIcon(pm));
                app.setLabel(appInfo.loadLabel(pm).toString());
                app.setSystemApp(pm.getLaunchIntentForPackage(appInfo.packageName) == null);
                app.setForcedExcluded(exclusion.isExcluded(appInfo.packageName));

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

    public void saveSelectedApps() {
        PerAppVpnMode perAppVPNMode = perAppVpnModeMutableLiveData.getValue();
        // save app list only if selected mode
        if (perAppVPNMode == PerAppVpnMode.EXCEPT_DISALLOWED
                || perAppVPNMode == PerAppVpnMode.ONLY_ALLOWED) {
            // use the full list: appListMutableLiveData is filtered by showSystemApps,
            // saving it would drop rules for hidden system apps
            List<AppInfo> appInfoList = allLoadedApps;
            new Thread(() -> {
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
