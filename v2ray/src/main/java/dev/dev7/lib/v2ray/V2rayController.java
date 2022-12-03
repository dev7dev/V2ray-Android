package dev.dev7.lib.v2ray;

import static dev.dev7.lib.v2ray.core.utils.APPLICATION_ICON;
import static dev.dev7.lib.v2ray.core.utils.APPLICATION_NAME;
import static dev.dev7.lib.v2ray.core.utils.BLOCKED_APPS;
import static dev.dev7.lib.v2ray.core.utils.FLAG_VPN_START;
import static dev.dev7.lib.v2ray.core.utils.FLAG_VPN_STOP;
import static dev.dev7.lib.v2ray.core.utils.copyAssets;
import static dev.dev7.lib.v2ray.services.V2rayVPNService.V2RAY_TUN_STATE;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.ArrayList;

import dev.dev7.lib.v2ray.core.V2rayCore;
import dev.dev7.lib.v2ray.interfaces.V2rayStatsListener;
import dev.dev7.lib.v2ray.services.V2rayVPNService;

public class V2rayController {

    public static void init(final Context context, final int app_icon, final String app_name) {
        copyAssets(context);
        APPLICATION_ICON = app_icon;
        APPLICATION_NAME = app_name;
    }


    public static void StartV2ray(final Context context, final String config, final ArrayList<String> blocked_apps) {
        BLOCKED_APPS = blocked_apps;
        Intent start_intent = new Intent(context, V2rayVPNService.class);
        start_intent.putExtra("ACTION", FLAG_VPN_START);
        start_intent.putExtra("V2RAY_CONFIG", config);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(start_intent);
        else
            context.startService(start_intent);
    }

    public static void StopV2ray(final Context context) {
        BLOCKED_APPS = null;
        Intent stop_intent = new Intent(context, V2rayVPNService.class);
        stop_intent.putExtra("ACTION", FLAG_VPN_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(stop_intent);
        else
            context.startService(stop_intent);
    }

    public static void registerV2rayStatsListener(final V2rayStatsListener v2rayStatsListener) {
        V2rayVPNService.registerStatsListener(v2rayStatsListener);
    }

    public static void unRegisterV2rayStatsListeners() {
        V2rayVPNService.unRegisterStatsListeners();
    }


    public static String getServerDelay() {
        Long server_delay = V2rayCore.getV2rayDelay();
        if (server_delay == -1) {
            return "Connection Error";
        }
        return String.valueOf(server_delay);
    }

    public static int getV2rayState() {
        return V2RAY_TUN_STATE;
    }

    public static boolean isV2raySocketRunning() {
        return V2rayCore.isV2rayRunning();
    }
}
