package org.fptn.vpn.services.speedtest;

import com.elvishew.xlog.XLog;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.enums.BypassCensorshipMethod;
import org.fptn.vpn.enums.SniSpoofingMode;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

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

    public static ServerEntity findFastestServer(List<ServerEntity> serverEntityList, String sniHostName, BypassCensorshipMethod censorshipStrategy, SniSpoofingMode sniSpoofingMode) throws PVNClientException {
        XLog.tag(TAG).i("Speed test started [candidates=%d]", serverEntityList != null ? serverEntityList.size() : 0);

        if (serverEntityList != null && !serverEntityList.isEmpty()) {
            List<ServerEntity> selectedServers = selectServersForTesting(serverEntityList);
            XLog.tag(TAG).d("Testing %d/%d servers", selectedServers.size(), serverEntityList.size());

            ExecutorService executor = Executors.newFixedThreadPool(selectedServers.size());
            List<NativeSpeedTestTask> nativeSpeedTestTaskList = selectedServers.stream()
                    .map(fptnServerDto -> new NativeSpeedTestTask(fptnServerDto, sniHostName, censorshipStrategy, sniSpoofingMode))
                    .collect(Collectors.toList());
            try {
                NativeSpeedTestResult bestResult = executor.invokeAny(nativeSpeedTestTaskList, SEARCH_BEST_SERVER_MAX_TIMEOUT, TimeUnit.SECONDS);
                XLog.tag(TAG).i("Fastest server found [server=%s, latency=%dms]", bestResult.getServerEntity().getServerInfo(), bestResult.getDurationsMillis());
                XLog.tag(TAG).d("Speed test completed");
                return bestResult.getServerEntity();
            } catch (InterruptedException e) {
                // EXTERNAL STOP: The thread was interrupted from the outside
                XLog.tag(TAG).w("Speed test was externally cancelled");
                Thread.currentThread().interrupt(); // Restore interrupted status
            } catch (TimeoutException e) {
                // TIMEOUT: The timer expired before any server responded
                XLog.tag(TAG).e("Speed test timed out after %d seconds", SEARCH_BEST_SERVER_MAX_TIMEOUT);
                throw new PVNClientException(ErrorCode.FIND_FASTEST_SERVER_TIMEOUT);
            } catch (ExecutionException e) {
                // FAILURE: Tasks failed or crashed
                XLog.tag(TAG).e("Speed test execution failed: %s", e.getMessage());
                throw new PVNClientException(ErrorCode.ALL_SERVERS_UNREACHABLE);
            } finally {
                executor.shutdownNow();
            }
        } else {
            // Must never happen
            throw new PVNClientException(ErrorCode.SERVER_LIST_NULL_OR_EMPTY);
        }
        return null;
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

        XLog.tag(TAG).d("Server classification [premium=%d, regular=%d]", premiumServers.size(), regularServers.size());

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
        XLog.tag(TAG).d("Selected %d servers for testing [premium=%d, regular=%d]", selectedServers.size(), premiumServers.size(), selectedServers.size() - premiumServers.size());
        return selectedServers;
    }
}
