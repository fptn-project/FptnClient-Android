package org.fptn.vpn.views.speedtest;

import android.util.Log;

import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class SpeedTestUtils {
    private static final String TAG = SpeedTestUtils.class.getName();
    private static final long SEARCH_BEST_SERVER_MAX_TIMEOUT = 30L;

    public static FptnServerDto findFastestServer(List<FptnServerDto> fptnServerDtoList, String sniHostName) throws PVNClientException {
        Log.d(TAG, "SpeedTestUtils.findFastestServer() start: " + Instant.now() + ", Thread.Id: " + Thread.currentThread().getId());

        if (fptnServerDtoList != null && !fptnServerDtoList.isEmpty()) {
            List<FptnServerDto> selectedServers = selectRandomServers(fptnServerDtoList);
            Log.d(TAG, "SpeedTestUtils.findFastestServer() testing " + selectedServers.size() +
                    " out of " + fptnServerDtoList.size() + " servers");

            ExecutorService executor = Executors.newFixedThreadPool(selectedServers.size());
            List<NativeSpeedTestTask> nativeSpeedTestTaskList = selectedServers.stream()
                    .map(fptnServerDto -> new NativeSpeedTestTask(fptnServerDto, sniHostName))
                    .collect(Collectors.toList());
            try {
                NativeSpeedTestResult bestResult = executor.invokeAny(nativeSpeedTestTaskList, SEARCH_BEST_SERVER_MAX_TIMEOUT, TimeUnit.SECONDS);
                Log.d(TAG, "SpeedTestUtils.findFastestServer() bestServer: " + bestResult.getFptnServerDto().getServerInfo() +
                        " with response time: " + bestResult.getDurationsMillis() + " ms");
                Log.d(TAG, "SpeedTestUtils.findFastestServer() end: " + Instant.now());
                return bestResult.getFptnServerDto();
            } catch (InterruptedException e) {
                throw new PVNClientException(ErrorCode.FIND_FASTEST_SERVER_TIMEOUT);
            } catch (ExecutionException | TimeoutException e) {
                throw new PVNClientException(ErrorCode.ALL_SERVERS_UNREACHABLE);
            } finally {
                executor.shutdown();
            }
        }
        throw new PVNClientException(ErrorCode.SERVER_LIST_NULL_OR_EMPTY);
    }

    private static List<FptnServerDto> selectRandomServers(List<FptnServerDto> servers) {
        if (servers.size() <= 1) {
            return servers;
        }
        List<FptnServerDto> shuffled = new ArrayList<>(servers);
        Collections.shuffle(shuffled);
        int count = Math.max(1, (int) Math.ceil(servers.size() * 0.5));
        return shuffled.subList(0, count);
    }
}
