package dev.dev7.lib.v2ray;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Build;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import java.util.ArrayList;
import java.util.Objects;

import dev.dev7.lib.v2ray.core.V2rayCoreManager;
import dev.dev7.lib.v2ray.services.V2rayProxyOnlyService;
import dev.dev7.lib.v2ray.services.V2rayVPNService;
import dev.dev7.lib.v2ray.utils.AppConfigs;
import dev.dev7.lib.v2ray.utils.Utilities;
import libv2ray.Libv2ray;

public class V2rayController {

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static void init(final Context context, final int app_icon, final String app_name) {
        Utilities.copyAssets(context);
        AppConfigs.APPLICATION_ICON = app_icon;
        AppConfigs.APPLICATION_NAME = app_name;
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                AppConfigs.V2RAY_STATE = (AppConfigs.V2RAY_STATES) Objects.requireNonNull(arg1.getExtras()).getSerializable("STATE");
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(br, new IntentFilter("V2RAY_CONNECTION_INFO"), Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(br, new IntentFilter("V2RAY_CONNECTION_INFO"));
        }
    }

    public static void changeConnectionMode(final AppConfigs.V2RAY_CONNECTION_MODES connection_mode) {
        if (getConnectionState() == AppConfigs.V2RAY_STATES.V2RAY_DISCONNECTED) {
            AppConfigs.V2RAY_CONNECTION_MODE = connection_mode;
        }
    }

    public static boolean IsPreparedForConnection(final Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED) {
                return false;
            }
        }
        Intent vpnServicePrepareIntent = VpnService.prepare(context);
        return vpnServicePrepareIntent == null;
    }


    public static void StartV2ray(final Context context, final String remark, final String config, final ArrayList<String> blocked_apps) {
        AppConfigs.V2RAY_CONFIG = Utilities.parseV2rayJsonFile(remark, config, blocked_apps);
        if (AppConfigs.V2RAY_CONFIG == null) {
            return;
        }
        Intent start_intent;
        if (AppConfigs.V2RAY_CONNECTION_MODE == AppConfigs.V2RAY_CONNECTION_MODES.PROXY_ONLY) {
            start_intent = new Intent(context, V2rayProxyOnlyService.class);
        } else if (AppConfigs.V2RAY_CONNECTION_MODE == AppConfigs.V2RAY_CONNECTION_MODES.VPN_TUN) {
            start_intent = new Intent(context, V2rayVPNService.class);
        } else {
            return;
        }
        start_intent.setPackage(context.getPackageName());
        start_intent.putExtra("COMMAND", AppConfigs.V2RAY_SERVICE_COMMANDS.START_SERVICE);
        start_intent.putExtra("V2RAY_CONFIG", AppConfigs.V2RAY_CONFIG);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            context.startForegroundService(start_intent);
        } else {
            context.startService(start_intent);
        }
    }

    public static void StopV2ray(final Context context) {
        Intent stop_intent;
        if (AppConfigs.V2RAY_CONNECTION_MODE == AppConfigs.V2RAY_CONNECTION_MODES.PROXY_ONLY) {
            stop_intent = new Intent(context, V2rayProxyOnlyService.class);
        } else if (AppConfigs.V2RAY_CONNECTION_MODE == AppConfigs.V2RAY_CONNECTION_MODES.VPN_TUN) {
            stop_intent = new Intent(context, V2rayVPNService.class);
        } else {
            return;
        }
        stop_intent.putExtra("COMMAND", AppConfigs.V2RAY_SERVICE_COMMANDS.STOP_SERVICE);
        stop_intent.setPackage(context.getPackageName());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            context.startForegroundService(stop_intent);
        } else {
            context.startService(stop_intent);
        }
        AppConfigs.V2RAY_CONFIG = null;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static void getConnectedV2rayServerDelay(final Context context, final TextView tvDelay) {
        Intent check_delay;
        if (AppConfigs.V2RAY_CONNECTION_MODE == AppConfigs.V2RAY_CONNECTION_MODES.PROXY_ONLY) {
            check_delay = new Intent(context, V2rayProxyOnlyService.class);
        } else if (AppConfigs.V2RAY_CONNECTION_MODE == AppConfigs.V2RAY_CONNECTION_MODES.VPN_TUN) {
            check_delay = new Intent(context, V2rayVPNService.class);
        } else {
            return;
        }
        check_delay.putExtra("COMMAND", AppConfigs.V2RAY_SERVICE_COMMANDS.MEASURE_DELAY);
        context.startService(check_delay);
        BroadcastReceiver br = new BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                String delay = Objects.requireNonNull(arg1.getExtras()).getString("DELAY");
                tvDelay.setText("connected server delay : " + delay);
                context.unregisterReceiver(this);
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(br, new IntentFilter("CONNECTED_V2RAY_SERVER_DELAY"), Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(br, new IntentFilter("CONNECTED_V2RAY_SERVER_DELAY"));
        }

    }

    public static String getV2rayServerDelay(final String config) {
        final long server_delay = V2rayCoreManager.getInstance().getV2rayServerDelay(config);
        if (server_delay == -1L) {
            return "Network or Server Error";
        } else {
            return String.valueOf(server_delay);
        }
    }

    public static AppConfigs.V2RAY_CONNECTION_MODES getConnectionMode() {
        return AppConfigs.V2RAY_CONNECTION_MODE;
    }

    public static AppConfigs.V2RAY_STATES getConnectionState() {
        return AppConfigs.V2RAY_STATE;
    }

    public static String getCoreVersion() {
        return Libv2ray.checkVersionX();
    }


}
