package org.fptn.vpn.services.vpn;

import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class FptnServiceState {
    private final ConnectionState connectionState;
    private final PVNClientException exception;

    public static final FptnServiceState INITIAL = FptnServiceState.builder()
            .connectionState(ConnectionState.DISCONNECTED)
            .exception(null)
            .build();

    public static final FptnServiceState FAKE_CONNECTING = FptnServiceState.builder()
            .connectionState(ConnectionState.CONNECTING)
            .exception(null)
            .build();
}
