package org.fptn.vpn.views.login;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.utils.token.TokenUtils;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;

public class LoginActivityViewModel extends AndroidViewModel {

    @Getter
    private final MutableLiveData<String> errorTextLiveData = new MutableLiveData<>("");

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AppDatabase appDatabase = AppDatabase.getInstance(getApplication());

    public LoginActivityViewModel(@NonNull Application application) {
        super(application);
    }

    public ListenableFuture<Void> parseAndSaveToken(String token) throws PVNClientException {
        List<ServerEntity> serverEntities = TokenUtils.parseToken(token);
        return MoreExecutors.listeningDecorator(executorService).submit(() -> {
            appDatabase.serverDAO().deleteAndInsert(serverEntities);
            return null;
        });
    }
}
