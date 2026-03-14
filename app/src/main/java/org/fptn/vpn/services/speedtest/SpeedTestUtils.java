package org.fptn.vpn.services.speedtest;

import android.util.Log;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
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
    private static final String PREMIUM_KEYWORD = "premium";

    public static ServerEntity findFastestServer(List<ServerEntity> serverEntityList, String sniHostName, BypassCensorshipMethod censorshipStrategy) throws PVNClientException {
        Log.d(TAG, "SpeedTestUtils.findFastestServer() start: " + Instant.now() + ", Thread.Id: " + Thread.currentThread().getId());

        if (serverEntityList != null && !serverEntityList.isEmpty()) {
            List<ServerEntity> selectedServers = selectServersForTesting(serverEntityList);
            Log.d(TAG, "SpeedTestUtils.findFastestServer() testing " + selectedServers.size() +
                    " out of " + serverEntityList.size() + " servers");

            ExecutorService executor = Executors.newFixedThreadPool(selectedServers.size());
            List<NativeSpeedTestTask> nativeSpeedTestTaskList = selectedServers.stream()
                    .map(fptnServerDto -> new NativeSpeedTestTask(fptnServerDto, sniHostName, censorshipStrategy))
                    .collect(Collectors.toList());
            try {
                NativeSpeedTestResult bestResult = executor.invokeAny(nativeSpeedTestTaskList, SEARCH_BEST_SERVER_MAX_TIMEOUT, TimeUnit.SECONDS);
                Log.d(TAG, "SpeedTestUtils.findFastestServer() bestServer: " + bestResult.getServerEntity().getServerInfo() +
                        " with response time: " + bestResult.getDurationsMillis() + " ms");
                Log.d(TAG, "SpeedTestUtils.findFastestServer() end: " + Instant.now());
                return bestResult.getServerEntity();
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

    private static List<ServerEntity> selectServersForTesting(List<ServerEntity> servers) {
        if (servers.size() <= 1) {
            return servers;
        }

        // Select premium servers
        List<ServerEntity> premiumServers = new ArrayList<>();
        List<ServerEntity> regularServers = new ArrayList<>();
        for (ServerEntity server : servers) {
            if (server.getName() != null &&
                    server.getName().toLowerCase().contains(PREMIUM_KEYWORD.toLowerCase())) {
                premiumServers.add(server);
            } else {
                regularServers.add(server);
            }
        }

        Log.d(TAG, "Found " + premiumServers.size() + " premium servers and " +
                regularServers.size() + " regular servers");

        List<ServerEntity> selectedServers = new ArrayList<>(premiumServers);
        if (!regularServers.isEmpty()) {
            Collections.shuffle(regularServers);
            int totalServersCount = servers.size();
            int regularCountToAdd = Math.max(0, (int) Math.ceil(totalServersCount * 0.3) - premiumServers.size());
            regularCountToAdd = Math.min(regularCountToAdd, regularServers.size());
            if (regularCountToAdd <= 0 && !regularServers.isEmpty()) {
                regularCountToAdd = 1;
            }
            if (regularCountToAdd > 0) {
                selectedServers.addAll(regularServers.subList(0, regularCountToAdd));
            }
        }
        Log.d(TAG, "Selected " + selectedServers.size() + " servers for testing: " +
                selectedServers.size() + " total (" + premiumServers.size() + " premium + " +
                (selectedServers.size() - premiumServers.size()) + " regular)");
        return selectedServers;
    }
}
