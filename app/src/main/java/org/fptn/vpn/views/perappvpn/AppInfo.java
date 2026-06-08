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
}
