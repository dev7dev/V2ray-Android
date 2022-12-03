package dev.dev7.lib.v2ray.core;

import static dev.dev7.lib.v2ray.core.utils.getUserAssetsPath;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import dev.dev7.lib.v2ray.interfaces.FBProtectListener;
import libv2ray.Libv2ray;
import libv2ray.V2RayPoint;
import libv2ray.V2RayVPNServiceSupportsSet;

public class V2rayCore {
    public static FBProtectListener fbProtectListener = null;
    private static final V2RayPoint v2RayPoint = Libv2ray.newV2RayPoint(new V2RayVPNServiceSupportsSet() {
        @Override
        public long onEmitStatus(long l, String s) {
            return 0;
        }

        @Override
        public long prepare() {
            return 0;
        }

        @Override
        public boolean protect(long l) {
            if (fbProtectListener != null)
                return fbProtectListener.onProtect((int) l);
            return true;
        }

        @Override
        public long setup(String s) {
            return 0;
        }

        @Override
        public long shutdown() {
            return 0;
        }
    }, Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1);

    public static boolean isV2rayRunning() {
        if (v2RayPoint == null) {
            return false;
        }
        return v2RayPoint.getIsRunning();
    }

    public static Long getV2rayDelay() {
        if (isV2rayRunning()) {
            try {
                return v2RayPoint.measureDelay();
            } catch (Exception e) {
                return -1L;
            }
        }
        return -1L;
    }

    public static boolean start(final Context context, String config) {
        Libv2ray.initV2Env(getUserAssetsPath(context));
        try {
            JSONObject config_json = new JSONObject(config);
            String server_address;
            String server_port;
            try {
                server_address = config_json.getJSONArray("outbounds")
                        .getJSONObject(0).getJSONObject("settings")
                        .getJSONArray("vnext").getJSONObject(0)
                        .getString("address");
                server_port = config_json.getJSONArray("outbounds")
                        .getJSONObject(0).getJSONObject("settings")
                        .getJSONArray("vnext").getJSONObject(0)
                        .getString("port");
            } catch (Exception e) {
                server_address = config_json.getJSONArray("outbounds")
                        .getJSONObject(0).getJSONObject("settings")
                        .getJSONArray("servers").getJSONObject(0)
                        .getString("address");
                server_port = config_json.getJSONArray("outbounds")
                        .getJSONObject(0).getJSONObject("settings")
                        .getJSONArray("servers").getJSONObject(0)
                        .getString("port");
            }

            v2RayPoint.setConfigureFileContent(config);
            v2RayPoint.setDomainName(server_address + ":" + server_port);
            v2RayPoint.runLoop(false);
        } catch (Exception e) {
            Log.e("V2RAY_CORE_START", "FAILED=>", e);
            return false;
        }
        return true;
    }

    public static void stop() {
        try {
            v2RayPoint.stopLoop();
        } catch (Exception e) {
            Log.e("V2RAY_CORE_STOP", "FAILED=>", e);
        }
    }


}
