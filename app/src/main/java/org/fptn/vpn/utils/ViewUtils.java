package org.fptn.vpn.utils;

import android.view.View;

public class ViewUtils {

    public static void hideView(View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    public static void showView(View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }
}
