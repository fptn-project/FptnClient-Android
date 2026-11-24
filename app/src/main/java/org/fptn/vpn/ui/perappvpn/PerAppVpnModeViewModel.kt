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

package org.fptn.vpn.ui.perappvpn

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.database.entity.AppInfoEntity
import org.fptn.vpn.enums.PerAppVpnMode
import org.fptn.vpn.utils.AppExclusion
import org.fptn.vpn.utils.SharedPrefUtils

class PerAppVpnModeViewModel(application: Application) : AndroidViewModel(application) {

    val perAppVpnModeMutableLiveData =
        MutableLiveData(SharedPrefUtils.getPerAppVPNMode(application))

    val appListMutableLiveData = MutableLiveData<List<AppInfo>>(emptyList())

    private val appDatabase = AppDatabase.getInstance(application)

    private var allLoadedApps: List<AppInfo> = emptyList()

    var showSystemApps: Boolean = SharedPrefUtils.getShowSystemApps(application)
        private set

    fun setPerAppVpnMode(perAppVpnMode: PerAppVpnMode) {
        perAppVpnModeMutableLiveData.postValue(perAppVpnMode)
        SharedPrefUtils.savePerAppVPNMode(getApplication(), perAppVpnMode)
    }

    fun setShowSystemApps(show: Boolean) {
        showSystemApps = show
        SharedPrefUtils.saveShowSystemApps(getApplication(), show)
        appListMutableLiveData.postValue(allLoadedApps.filter { showSystemApps || !it.isSystemApp })
    }

    fun loadInstalledApps(pm: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedAppsMap = appDatabase.appInfoDAO().getAll().associateBy(
                keySelector = { it.packageName },
                valueTransform = { entity ->
                    AppInfo(entity.packageName).apply {
                        isAllowed = entity.isAllowed
                        isDisallowed = entity.isDisallowed
                    }
                },
            )

            val application = getApplication<Application>()
            val thisAppPackageName = application.packageName
            val exclusion = AppExclusion(application)

            // Batch-query all launcher activities once to avoid 200+ slow IPC calls
            val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val launcherPackages = pm.queryIntentActivities(launcherIntent, 0)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()

            val packages = application.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            val apps = packages
                .filterNot { it.packageName.equals(thisAppPackageName, ignoreCase = true) }
                .map { appInfo ->
                    (savedAppsMap[appInfo.packageName] ?: AppInfo(appInfo.packageName)).apply {
                        icon = appInfo.loadIcon(pm)
                        label = appInfo.loadLabel(pm).toString()
                        isSystemApp = !launcherPackages.contains(appInfo.packageName)
                        isForcedExcluded = exclusion.isExcluded(appInfo.packageName)
                    }
                }
                .sortedWith { a, b -> a.label.compareTo(b.label, ignoreCase = true) }

            allLoadedApps = apps
            appListMutableLiveData.postValue(apps.filter { showSystemApps || !it.isSystemApp })
        }
    }

    fun saveSelectedApps() {
        val perAppVpnMode = perAppVpnModeMutableLiveData.value
        // save app list only if selected mode
        if (perAppVpnMode == PerAppVpnMode.EXCEPT_DISALLOWED || perAppVpnMode == PerAppVpnMode.ONLY_ALLOWED) {
            // use the full list: appListMutableLiveData is filtered by showSystemApps,
            // saving it would drop rules for hidden system apps
            val appInfoList = allLoadedApps
            viewModelScope.launch(Dispatchers.IO) {
                if (appInfoList.isNotEmpty()) {
                    val entities = appInfoList.map {
                        AppInfoEntity.of(it.packageName, it.isAllowed, it.isDisallowed)
                    }
                    val appInfoDAO = appDatabase.appInfoDAO()
                    appInfoDAO.deleteAll() // delete all previous records
                    appInfoDAO.insertAll(entities)
                }
            }
        }
    }
}
