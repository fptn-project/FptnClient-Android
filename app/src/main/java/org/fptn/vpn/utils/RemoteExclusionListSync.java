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

package org.fptn.vpn.utils;

import android.content.Context;

import com.elvishew.xlog.XLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Pulls always_excluded_apps / blocked_domains from GitHub so the exclusion lists can grow
// between app releases. Runs off the VPN's own executor so a slow/unreachable fetch can never
// delay connect/reconnect/disconnect; a fetched list only takes effect on the next connection,
// never the one in progress.
public final class RemoteExclusionListSync {

    private static final String TAG = "RemoteExclusionListSync";

    private static final String APPS_URL =
            "https://raw.githubusercontent.com/fptn-project/FptnClient-Android/refs/heads/develop/app/src/main/res/values/always_excluded_apps.xml";
    private static final String DOMAINS_URL =
            "https://raw.githubusercontent.com/fptn-project/FptnClient-Android/refs/heads/develop/app/src/main/res/values/blocked_domains.xml";

    private static final long MIN_SYNC_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(6);
    private static final int TIMEOUT_MILLIS = 15_000;

    private static final Pattern ITEM_PATTERN = Pattern.compile("<item>([^<]+)</item>");

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean syncInFlight = new AtomicBoolean(false);

    private RemoteExclusionListSync() {
    }

    // Call on every successful VPN connection; internally rate-limited so calling it often is
    // harmless. Returns immediately, sync happens on a dedicated background thread.
    public static void syncIfDue(Context context) {
        long lastSync = SharedPrefUtils.getRemoteListsLastSyncDate(context);
        if (System.currentTimeMillis() - lastSync < MIN_SYNC_INTERVAL_MILLIS) {
            return;
        }
        if (!syncInFlight.compareAndSet(false, true)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        executorService.execute(() -> {
            try {
                syncNow(appContext);
            } finally {
                syncInFlight.set(false);
            }
        });
    }

    private static void syncNow(Context context) {
        XLog.tag(TAG).i("Remote exclusion list sync started");
        try {
            Set<String> apps = fetchItems(APPS_URL);
            Set<String> domains = fetchItems(DOMAINS_URL);
            SharedPrefUtils.saveRemoteExcludedApps(context, apps);
            SharedPrefUtils.saveRemoteBlockedDomains(context, domains);
            SharedPrefUtils.saveRemoteListsLastSyncDate(context, System.currentTimeMillis());
            XLog.tag(TAG).i("Remote exclusion list sync finished OK [apps=%d, domains=%d]", apps.size(), domains.size());
        } catch (IOException e) {
            XLog.tag(TAG).w("Remote exclusion list sync failed, keeping previous cache: %s", e.getMessage());
        }
    }

    private static Set<String> fetchItems(String urlString) throws IOException {
        long startMillis = System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestMethod("GET");
        try {
            int code = connection.getResponseCode();
            long tookMillis = System.currentTimeMillis() - startMillis;
            if (code != HttpURLConnection.HTTP_OK) {
                XLog.tag(TAG).w("GET %s -> HTTP %d [%dms]", urlString, code, tookMillis);
                throw new IOException("HTTP " + code + " for " + urlString);
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line).append('\n');
                }
            }
            Set<String> items = new LinkedHashSet<>();
            Matcher matcher = ITEM_PATTERN.matcher(body);
            while (matcher.find()) {
                String item = matcher.group(1).trim();
                if (!item.isEmpty()) {
                    items.add(item);
                }
            }
            XLog.tag(TAG).i("GET %s -> HTTP %d, %d bytes, %d items [%dms]",
                    urlString, code, body.length(), items.size(), tookMillis);
            return items;
        } catch (IOException e) {
            long tookMillis = System.currentTimeMillis() - startMillis;
            XLog.tag(TAG).w("GET %s -> failed after %dms: %s", urlString, tookMillis, e.getMessage());
            throw e;
        } finally {
            connection.disconnect();
        }
    }
}
