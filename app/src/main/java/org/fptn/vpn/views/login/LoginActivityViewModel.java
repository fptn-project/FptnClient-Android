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

package org.fptn.vpn.views.login;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.utils.SharedPrefUtils;
import org.fptn.vpn.utils.token.TokenUtils;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivityViewModel extends AndroidViewModel {

    private final MutableLiveData<String> errorTextLiveData = new MutableLiveData<>("");

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());

    public LoginActivityViewModel(@NonNull Application application) {
        super(application);
    }

    // Written out explicitly (instead of Lombok's @Getter) because Kotlin's Java-interop
    // stub generation runs before the Lombok annotation processor, so Kotlin/Compose call
    // sites can't see a Lombok-generated getter here.
    public MutableLiveData<String> getErrorTextLiveData() {
        return errorTextLiveData;
    }

    public ListenableFuture<Void> parseAndSaveToken(String token) throws PVNClientException {
        List<ServerEntity> serverEntities = TokenUtils.parseToken(token);
        return MoreExecutors.listeningDecorator(executorService).submit(() -> {
            appDatabase.serverDAO().deleteAndInsert(serverEntities);
            SharedPrefUtils.saveTokenUpdatedDate(getApplication(), System.currentTimeMillis());
            return null;
        });
    }
}
