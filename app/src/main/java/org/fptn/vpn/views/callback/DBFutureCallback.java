package org.fptn.vpn.views.callback;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.FutureCallback;

import org.jspecify.annotations.Nullable;

public interface DBFutureCallback<V extends @Nullable Object> extends FutureCallback<V> {

    @Override
    default void onFailure(@NonNull Throwable t) {
        Log.e(this.getClass().getSimpleName(), "Failed to load from DB", t);
    }

}
