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

import org.fptn.vpn.R;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class AppExclusion {

    private final Set<String> excluded = new HashSet<>();
    private final Set<String> exceptions = new HashSet<>();

    public AppExclusion(Context context) {
        for (String packageName : context.getResources().getStringArray(R.array.always_excluded_apps_ru)) {
            excluded.add(packageName.toLowerCase(Locale.ROOT));
        }
        excluded.add(context.getPackageName().toLowerCase(Locale.ROOT));
        for (String packageName : context.getResources().getStringArray(R.array.app_exclusion_exceptions_ru)) {
            exceptions.add(packageName.toLowerCase(Locale.ROOT));
        }
    }

    public boolean isExcluded(String packageName) {
        if (packageName == null) {
            return false;
        }
        String pkg = packageName.toLowerCase(Locale.ROOT);
        if (matches(exceptions, pkg)) {
            return false;
        }
        return matches(excluded, pkg);
    }

    private static boolean matches(Set<String> set, String packageName) {
        String prefix = packageName;
        while (prefix.contains(".")) {
            if (set.contains(prefix)) {
                return true;
            }
            prefix = prefix.substring(0, prefix.lastIndexOf('.'));
        }
        return false;
    }
}
