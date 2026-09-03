package org.fptn.vpn.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Walks up the `ContextWrapper` chain to find the hosting `Activity`, if any. */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
