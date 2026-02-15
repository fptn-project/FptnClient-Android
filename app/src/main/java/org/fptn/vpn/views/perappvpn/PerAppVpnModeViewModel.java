package org.fptn.vpn.views.perappvpn;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import org.fptn.vpn.database.FptnDatabase;
import org.fptn.vpn.database.dao.AppInfoDAO;
import org.fptn.vpn.database.model.AppInfoEntity;
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

    public PerAppVpnModeViewModel(@NonNull Application application) {
        super(application);

        perAppVpnModeMutableLiveData = new MutableLiveData<>(SharedPrefUtils.getPerAppVPNMode(application));

        appListMutableLiveData = new MutableLiveData<>(List.of());
    }

    public void setPerAppVpnMode(PerAppVpnMode perAppVpnMode) {
        perAppVpnModeMutableLiveData.postValue(perAppVpnMode);
    }

    public void loadInstalledApps(PackageManager pm) {
        new Thread(() -> {
            List<AppInfoEntity> savedApps = FptnDatabase.getInstance(getApplication()).appInfoDAO().getAll();

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
                if (pm.getLaunchIntentForPackage(appInfo.packageName) != null && !thisAppPackageName.equalsIgnoreCase(appInfo.packageName)) {
                    AppInfo app = savedAppsMap.getOrDefault(appInfo.packageName, AppInfo.builder().packageName(appInfo.packageName).build());
                    app.setIcon(appInfo.loadIcon(pm));
                    app.setLabel(appInfo.loadLabel(pm).toString());

                    apps.add(app);
                }
            }

            Collections.sort(apps, (a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));
            appListMutableLiveData.postValue(apps);
        }).start();
    }

    public void saveAllSettings() {
        savePerAppVpnMode();
        saveSelectedApps();
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
                            .map(app -> {
                                AppInfoEntity appInfoEntity = new AppInfoEntity();
                                appInfoEntity.setPackageName(app.getPackageName());
                                appInfoEntity.setAllowed(app.isAllowed());
                                appInfoEntity.setDisallowed(app.isDisallowed());
                                return appInfoEntity;
                            })
                            .collect(Collectors.toList());
                    AppInfoDAO appInfoDAO = FptnDatabase.getInstance(getApplication()).appInfoDAO();
                    appInfoDAO.deleteAll(); // delete all previous records
                    appInfoDAO.insertAll(entities);
                }
            }).start();
        }
    }
}
