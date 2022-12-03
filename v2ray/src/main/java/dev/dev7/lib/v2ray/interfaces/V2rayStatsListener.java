package dev.dev7.lib.v2ray.interfaces;

public interface V2rayStatsListener {
    void onStatsCommit(final int v2ray_state, final String duration,
                       final String downloadSpeed, final String uploadSpeed,
                       final String totalDownload, final String totalUpload);
}
