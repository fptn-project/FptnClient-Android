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

import android.graphics.drawable.Drawable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AppInfo {
    private String label;
    private String packageName;
    private Drawable icon;
    private boolean allowed;
    private boolean disallowed;
    private boolean systemApp;
    private boolean forcedExcluded;

    // Written out explicitly (instead of relying on Lombok's @Data) for the subset of
    // accessors the Compose screen needs: Kotlin's Java-interop stub generation runs before
    // the Lombok annotation processor, so Kotlin call sites can't see Lombok-generated members.
    public String getLabel() {
        return label;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public boolean isDisallowed() {
        return disallowed;
    }

    public void setDisallowed(boolean disallowed) {
        this.disallowed = disallowed;
    }

    public boolean isForcedExcluded() {
        return forcedExcluded;
    }
}
