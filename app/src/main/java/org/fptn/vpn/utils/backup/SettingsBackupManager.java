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

package org.fptn.vpn.utils.backup;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.dao.AppInfoDAO;
import org.fptn.vpn.database.entity.AppInfoEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.PerAppVpnMode;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.utils.SharedPrefUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exports all user-configurable FPTN settings to JSON and applies them back.
 * Servers and access tokens are intentionally excluded: the backup file is
 * meant to be shareable and must not contain credentials.
 * Call only on a background thread (Room access).
 */
public final class SettingsBackupManager {
    public static final String BACKUP_TYPE = "fptn-settings-backup";
    public static final int BACKUP_VERSION = 1;

    private SettingsBackupManager() {
    }

    private static class BackupFile {
        String type;
        Integer version;
        BackupSettings settings;
        List<BackupAppRule> perAppVpnApps;
    }

    // Boxed types: a field missing from the file stays null and is skipped on
    // import instead of silently resetting the setting to a default value.
    private static class BackupSettings {
        String sniHostname;
        String bypassCensorshipMethod;
        String sniSpoofingMode;
        String perAppVpnMode;
        Boolean showSystemApps;
        Boolean adBlockEnabled;
        Boolean splitTunnelDomainsEnabled;
        String splitTunnelDomains;
        Boolean domainBlacklistEnabled;
        String domainBlacklist;
        Boolean customDnsEnabled;
        String customDnsIpv4;
        Boolean reconnectOnChangeNetworkType;
        Boolean reconnectOnChangeIp;
        Integer reconnectAttemptsCount;
        Integer delayBetweenReconnect;
        Boolean resetSelectedServerOnDisconnect;
        Boolean resetSelectedServerOnException;
        Boolean autoFallbackEnabled;
        Integer autoFallbackThreshold;
        Boolean showSpeedInNotification;
        Boolean showTrafficChart;
        Boolean killSwitchEnabled;
    }

    private static class BackupAppRule {
        String packageName;
        boolean allowed;
        boolean disallowed;
    }

    public static String exportToJson(Context context) {
        BackupFile backup = new BackupFile();
        backup.type = BACKUP_TYPE;
        backup.version = BACKUP_VERSION;

        BackupSettings settings = new BackupSettings();
        settings.sniHostname = SharedPrefUtils.getSniHostname(context);
        settings.bypassCensorshipMethod = SharedPrefUtils.getBypassCensorshipMethod(context).name();
        settings.sniSpoofingMode = SharedPrefUtils.getSniSpoofingMode(context).name();
        settings.perAppVpnMode = SharedPrefUtils.getPerAppVPNMode(context).name();
        settings.showSystemApps = SharedPrefUtils.getShowSystemApps(context);
        settings.adBlockEnabled = SharedPrefUtils.getAdBlockEnabled(context);
        settings.splitTunnelDomainsEnabled = SharedPrefUtils.getSplitTunnelDomainsEnabled(context);
        settings.splitTunnelDomains = SharedPrefUtils.getSplitTunnelDomains(context);
        settings.domainBlacklistEnabled = SharedPrefUtils.getDomainBlacklistEnabled(context);
        settings.domainBlacklist = SharedPrefUtils.getDomainBlacklistDomains(context);
        settings.customDnsEnabled = SharedPrefUtils.getCustomDnsEnabled(context);
        settings.customDnsIpv4 = SharedPrefUtils.getCustomDnsIpv4(context);
        settings.reconnectOnChangeNetworkType = SharedPrefUtils.getReconnectOnChangeNetworkTypeEnabled(context);
        settings.reconnectOnChangeIp = SharedPrefUtils.getReconnectOnChangeIPEnabled(context);
        settings.reconnectAttemptsCount = SharedPrefUtils.getReconnectAttemptsCount(context);
        settings.delayBetweenReconnect = SharedPrefUtils.getDelayBetweenReconnect(context);
        settings.resetSelectedServerOnDisconnect = SharedPrefUtils.getResetSelectedServerEnabled(context);
        settings.resetSelectedServerOnException = SharedPrefUtils.getResetSelectedServerOnExceptionEnabled(context);
        settings.autoFallbackEnabled = SharedPrefUtils.getAutoFallbackEnabled(context);
        settings.autoFallbackThreshold = SharedPrefUtils.getAutoFallbackThreshold(context);
        settings.showSpeedInNotification = SharedPrefUtils.getShowSpeedInNotification(context);
        settings.showTrafficChart = SharedPrefUtils.getShowTrafficChart(context);
        settings.killSwitchEnabled = SharedPrefUtils.getKillSwitchEnabled(context);
        backup.settings = settings;

        backup.perAppVpnApps = AppDatabase.getInstance(context).appInfoDAO().getAll().stream()
                .map(entity -> {
                    BackupAppRule rule = new BackupAppRule();
                    rule.packageName = entity.getPackageName();
                    rule.allowed = entity.isAllowed();
                    rule.disallowed = entity.isDisallowed();
                    return rule;
                })
                .collect(Collectors.toList());

        return new GsonBuilder().setPrettyPrinting().create().toJson(backup);
    }

    public static void importFromJson(Context context, String json) {
        BackupFile backup = new Gson().fromJson(json, BackupFile.class);
        if (backup == null || !BACKUP_TYPE.equals(backup.type) || backup.settings == null) {
            throw new IllegalArgumentException("Not a valid FPTN settings backup file");
        }
        if (backup.version == null || backup.version > BACKUP_VERSION) {
            throw new IllegalArgumentException("Unsupported backup version: " + backup.version);
        }

        BackupSettings settings = backup.settings;
        if (settings.sniHostname != null) {
            SharedPrefUtils.saveSniHostname(context, settings.sniHostname);
        }
        BypassCensorshipMethod bypassMethod =
                parseEnum(BypassCensorshipMethod.class, settings.bypassCensorshipMethod);
        if (bypassMethod != null) {
            SharedPrefUtils.saveBypassCensorshipMethod(context, bypassMethod);
        }
        SniSpoofingMode sniSpoofingMode = parseEnum(SniSpoofingMode.class, settings.sniSpoofingMode);
        if (sniSpoofingMode != null) {
            SharedPrefUtils.saveSniSpoofingMode(context, sniSpoofingMode);
        }
        PerAppVpnMode perAppVpnMode = parseEnum(PerAppVpnMode.class, settings.perAppVpnMode);
        if (perAppVpnMode != null) {
            SharedPrefUtils.savePerAppVPNMode(context, perAppVpnMode);
        }
        if (settings.showSystemApps != null) {
            SharedPrefUtils.saveShowSystemApps(context, settings.showSystemApps);
        }
        if (settings.adBlockEnabled != null) {
            SharedPrefUtils.saveAdBlockEnabled(context, settings.adBlockEnabled);
        }
        if (settings.splitTunnelDomainsEnabled != null) {
            SharedPrefUtils.saveSplitTunnelDomainsEnabled(context, settings.splitTunnelDomainsEnabled);
        }
        if (settings.splitTunnelDomains != null) {
            SharedPrefUtils.saveSplitTunnelDomains(context, settings.splitTunnelDomains);
        }
        if (settings.domainBlacklistEnabled != null) {
            SharedPrefUtils.saveDomainBlacklistEnabled(context, settings.domainBlacklistEnabled);
        }
        if (settings.domainBlacklist != null) {
            SharedPrefUtils.saveDomainBlacklistDomains(context, settings.domainBlacklist);
        }
        if (settings.customDnsEnabled != null) {
            SharedPrefUtils.saveCustomDnsEnabled(context, settings.customDnsEnabled);
        }
        if (settings.customDnsIpv4 != null) {
            SharedPrefUtils.saveCustomDnsIpv4(context, settings.customDnsIpv4);
        }
        if (settings.reconnectOnChangeNetworkType != null) {
            SharedPrefUtils.saveReconnectOnChangeNetworkTypeEnabled(context, settings.reconnectOnChangeNetworkType);
        }
        if (settings.reconnectOnChangeIp != null) {
            SharedPrefUtils.saveReconnectOnChangeIPEnabled(context, settings.reconnectOnChangeIp);
        }
        if (settings.reconnectAttemptsCount != null) {
            SharedPrefUtils.saveReconnectAttemptsCount(context, settings.reconnectAttemptsCount);
        }
        if (settings.delayBetweenReconnect != null) {
            SharedPrefUtils.saveDelayBetweenReconnect(context, settings.delayBetweenReconnect);
        }
        if (settings.resetSelectedServerOnDisconnect != null) {
            SharedPrefUtils.saveResetSelectedServerEnabled(context, settings.resetSelectedServerOnDisconnect);
        }
        if (settings.resetSelectedServerOnException != null) {
            SharedPrefUtils.saveResetSelectedServerOnExceptionEnabled(context, settings.resetSelectedServerOnException);
        }
        if (settings.autoFallbackEnabled != null) {
            SharedPrefUtils.saveAutoFallbackEnabled(context, settings.autoFallbackEnabled);
        }
        if (settings.autoFallbackThreshold != null) {
            SharedPrefUtils.saveAutoFallbackThreshold(context, settings.autoFallbackThreshold);
        }
        if (settings.showSpeedInNotification != null) {
            SharedPrefUtils.saveShowSpeedInNotification(context, settings.showSpeedInNotification);
        }
        if (settings.showTrafficChart != null) {
            SharedPrefUtils.saveShowTrafficChart(context, settings.showTrafficChart);
        }
        if (settings.killSwitchEnabled != null) {
            SharedPrefUtils.saveKillSwitchEnabled(context, settings.killSwitchEnabled);
        }

        if (backup.perAppVpnApps != null) {
            List<AppInfoEntity> entities = backup.perAppVpnApps.stream()
                    .map(rule -> AppInfoEntity.builder()
                            .packageName(rule.packageName)
                            .allowed(rule.allowed)
                            .disallowed(rule.disallowed)
                            .build())
                    .collect(Collectors.toList());
            AppInfoDAO appInfoDAO = AppDatabase.getInstance(context).appInfoDAO();
            appInfoDAO.deleteAll();
            appInfoDAO.insertAll(entities);
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String name) {
        try {
            return name != null ? Enum.valueOf(enumClass, name) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
