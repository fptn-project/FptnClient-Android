package org.fptn.vpn.services.ping;

public class PingResult {
    private final String host;
    private final long pingMs;
    private final boolean success;

    public PingResult(String host, long pingMs, boolean success) {
        this.host = host;
        this.pingMs = pingMs;
        this.success = success;
    }

    public String getHost() { return host; }
    public long getPingMs() { return pingMs; }
    public boolean isSuccess() { return success; }
}
