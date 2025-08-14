package org.fptn.vpn.viewmodel;

import static org.fptn.vpn.utils.ResourcesUtils.getStringResourceByName;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.fptn.vpn.R;
import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.repository.FptnServerRepository;
import org.fptn.vpn.services.CustomVpnService;
import org.fptn.vpn.services.CustomVpnServiceState;
import org.fptn.vpn.utils.CountryFlags;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.utils.TimeUtils;
import org.fptn.vpn.viewmodel.model.FptnToken;
import org.fptn.vpn.viewmodel.model.FptnTokenServer;
import org.fptn.vpn.viewmodel.model.FptnTokenValidationUtils;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import lombok.Getter;

public class FptnServerViewModel extends AndroidViewModel {
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private final FptnServerRepository fptnServerRepository;

    @Getter
    private final MutableLiveData<CustomVpnServiceState> serviceStateMutableLiveData = new MutableLiveData<>(CustomVpnServiceState.INITIAL);

    /*Only for show on views*/
    @Getter
    private final MutableLiveData<String> timerTextLiveData = new MutableLiveData<>(getApplication().getString(R.string.zero_time));
    @Getter
    private final MutableLiveData<String> downloadSpeedAsStringLiveData = new MutableLiveData<>(getApplication().getString(R.string.zero_speed));
    @Getter
    private final MutableLiveData<String> uploadSpeedAsStringLiveData = new MutableLiveData<>(getApplication().getString(R.string.zero_speed));
    @Getter
    private final MutableLiveData<String> errorTextLiveData = new MutableLiveData<>("");
    @Getter
    private final MutableLiveData<String> statusTextLiveData = new MutableLiveData<>(getApplication().getString(R.string.disconnected));
    @Getter
    private final LiveData<List<FptnServerDto>> serverDtoListLiveData;

    // observers
    private final Observer<CustomVpnServiceState> serviceStateObserver;

    public FptnServerViewModel(@NonNull Application application) {
        super(application);

        fptnServerRepository = new FptnServerRepository(getApplication());
        serverDtoListLiveData = fptnServerRepository.getAllServersLiveData();

        serviceStateObserver = customVpnServiceState -> {
            if (customVpnServiceState != null) {
                ConnectionState connectionState = customVpnServiceState.getConnectionState();
                switch (connectionState) {
                    case DISCONNECTED ->
                            statusTextLiveData.postValue(getApplication().getString(R.string.disconnected));
                    case CONNECTING -> {
                        statusTextLiveData.postValue(getApplication().getString(R.string.connecting));
                        resetErrorMessage();
                    }
                    case CONNECTED -> {
                        statusTextLiveData.postValue(getApplication().getString(R.string.connected));
                        resetErrorMessage();
                    }
                    case RECONNECTING ->
                            statusTextLiveData.postValue(getApplication().getString(R.string.connected));
                }

                PVNClientException exception = customVpnServiceState.getException();
                if (exception != null) {
                    handlePVNClientException(exception);
                }
            }
        };
        serviceStateMutableLiveData.observeForever(serviceStateObserver);
    }

    private void handlePVNClientException(PVNClientException exception) {
        ErrorCode errorCode = exception.errorCode;
        if (errorCode != ErrorCode.UNKNOWN_ERROR) {
            String stringResourceByName = getStringResourceByName(getApplication(), errorCode.getValue());
            Log.e(getTag(), "Error as text: " + stringResourceByName);

            errorTextLiveData.postValue(stringResourceByName);
        } else {
            errorTextLiveData.postValue(exception.errorMessage);
        }
    }

    public void resetErrorMessage() {
        errorTextLiveData.postValue("");
    }

    public ListenableFuture<List<FptnServerDto>> getAllServers() {
        return fptnServerRepository.getAllServersListFuture();
    }

    public void deleteAll() {
        fptnServerRepository.deleteAllServers();
    }

    public void parseAndSaveFptnLink(String fptnTokenString) throws PVNClientException {
        try {
            List<FptnServerDto> serverDtoList = new ArrayList<>();

            // removes all whitespaces and non-visible characters (e.g., tab, \n) and prefixes fptn://  fptn:
            final String clearedToken = fptnTokenString.replaceAll("\\s+", "")
                    .replace("fptn://", "")
                    .replace("fptn:", "");
            try {
                String decodedToken = new String(Base64.getDecoder().decode(clearedToken));
                FptnToken fptnToken = OBJECT_MAPPER.readValue(decodedToken, FptnToken.class);
                FptnTokenValidationUtils.validate(fptnToken);

                String username = fptnToken.getUsername();
                String password = fptnToken.getPassword();
                // add normal servers
                List<FptnTokenServer> servers = fptnToken.getServers();
                if (servers != null && !servers.isEmpty()) {
                    for (FptnTokenServer server : servers) {
                        FptnServerDto fptnServerDto = createFptnServerDto(server, username, password, false);
                        serverDtoList.add(fptnServerDto);
                        Log.d(getTag(), "Server from token: " + fptnServerDto.getServerInfo());
                    }
                }
                // add censured servers
                List<FptnTokenServer> censoredServers = fptnToken.getCensoredServers();
                if (censoredServers != null && !censoredServers.isEmpty()) {
                    for (FptnTokenServer server : censoredServers) {
                        FptnServerDto fptnServerDto = createFptnServerDto(server, username, password, true);
                        serverDtoList.add(fptnServerDto);
                        Log.d(getTag(), "Censured server from token: " + fptnServerDto.getServerInfo());
                    }
                }
            } catch (IOException e) {
                String errorMessage = "Can't parse FPTNLink!: ";
                Log.e(getTag(), errorMessage, e);
                throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
            }

            if (serverDtoList.isEmpty()) {
                Log.e(getTag(), "Server list from token is empty!");
                throw new PVNClientException(ErrorCode.SERVER_LIST_NULL_OR_EMPTY);
            }

            fptnServerRepository.deleteAllServers(); // delete all old servers
            fptnServerRepository.insertAll(serverDtoList); // add new servers
        } catch (PVNClientException e) {
            handlePVNClientException(e);
            throw e;
        }
    }

    @NonNull
    private static FptnServerDto createFptnServerDto(FptnTokenServer server, String username, String password, boolean censured) {
        String countryCode = Optional.ofNullable(server.getCountryCode())
                .orElse(CountryFlags.getCountryCodeFromHostName(server.getName()));
        return new FptnServerDto(
                0,
                false,
                server.getName(),
                username,
                password,
                server.getHost(),
                server.getPort(),
                countryCode,
                server.getMd5Fingerprint(),
                censured);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        serviceStateMutableLiveData.removeObserver(serviceStateObserver);
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }

    public void subscribeService(CustomVpnService service) {
        service.getSpeedAndDurationMutableLiveData().observeForever(speedAndDuration -> {
            if (speedAndDuration != null) {
                downloadSpeedAsStringLiveData.postValue(speedAndDuration.getFirst());
                uploadSpeedAsStringLiveData.postValue(speedAndDuration.getSecond());
                timerTextLiveData.postValue(TimeUtils.getTime(speedAndDuration.getThird()));
            }
        });

        service.getServiceStateMutableLiveData().observeForever(serviceStateMutableLiveData::postValue);
    }

    public void unsubscribe() {
        // todo: check memory leaks and maybe remove observers
    }
}
