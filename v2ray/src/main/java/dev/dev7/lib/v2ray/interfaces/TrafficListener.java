package dev.dev7.lib.v2ray.interfaces;

public interface TrafficListener {
    void onTrafficChanged(long uploadSpeed, long downloadSpeed, long uploadedTraffic, long downloadedTraffic);
}
