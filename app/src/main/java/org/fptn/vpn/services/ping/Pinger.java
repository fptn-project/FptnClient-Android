package org.fptn.vpn.services.ping;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Pinger {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ExecutorService executor;
    private PingCallback callback;

    public interface PingCallback {
        void onResult(PingResult result);
    }

    public void start(String[] hosts, PingCallback callback) {
        stop();

        this.callback = callback;
        this.isRunning.set(true);
        this.executor = Executors.newFixedThreadPool(4);

        for (String host : hosts) {
            if (!isRunning.get()) break;
            if (host == null || host.trim().isEmpty()) continue;

            executor.execute(() -> {
                if (!isRunning.get()) return;

                long pingTime = -1;
                boolean reachable = false;

                try {
                    Process process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 2 " + host.trim());
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("time=")) {
                                String[] parts = line.split("time=");
                                if (parts.length > 1) {
                                    String timeStr = parts[1].replace("ms", "").trim();
                                    pingTime = (long)(Float.parseFloat(timeStr));
                                    reachable = true;
                                }
                                break;
                            }
                        }
                        reader.close();
                    }
                } catch (Exception e) {
                    Log.e("Pinger", "Error pinging " + host, e);
                }

                if (isRunning.get()) {
                    PingResult result = new PingResult(host, pingTime, reachable);
                    mainHandler.post(() -> {
                        if (Pinger.this.callback != null) {
                            Pinger.this.callback.onResult(result);
                        }
                    });
                }
            });
        }
    }

    public void stop() {
        isRunning.set(false);
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        callback = null;
    }
}
