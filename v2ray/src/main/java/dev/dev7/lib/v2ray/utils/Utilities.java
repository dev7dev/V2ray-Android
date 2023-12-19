package dev.dev7.lib.v2ray.utils;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import dev.dev7.lib.v2ray.core.V2rayCoreManager;

public class Utilities {

    public static String getDeviceIdForXUDPBaseKey() {
        String androidId = Settings.Secure.ANDROID_ID;
        byte[] androidIdBytes = androidId.getBytes(StandardCharsets.UTF_8);
        return Base64.encodeToString(Arrays.copyOf(androidIdBytes, 32), Base64.URL_SAFE);
    }

    public static void CopyFiles(InputStream src, File dst) throws IOException {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try (OutputStream out = Files.newOutputStream(dst.toPath())) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = src.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        } else {
            try (OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = src.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public static String getUserAssetsPath(Context context) {
        File extDir = context.getExternalFilesDir("assets");
        if (extDir == null) {
            return "";
        }
        if (!extDir.exists()) {
            return context.getDir("assets", 0).getAbsolutePath();
        } else {
            return extDir.getAbsolutePath();
        }
    }

    public static void copyAssets(final Context context) {
        String extFolder = getUserAssetsPath(context);
        try {
            String geo = "geosite.dat,geoip.dat";
            for (String assets_obj : Objects.requireNonNull(context.getAssets().list(""))) {
                if (geo.contains(assets_obj)) {
                    CopyFiles(context.getAssets().open(assets_obj), new File(extFolder, assets_obj));
                }
            }
        } catch (Exception e) {
            Log.e("Utilities", "copyAssets failed=>", e);
        }
    }


    public static String convertIntToTwoDigit(int value) {
        if (value < 10) return "0" + value;
        else return String.valueOf(value);
    }

    public static String parseTraffic(final double bytes, final boolean inBits, final boolean isMomentary) {
        double value = inBits ? bytes * 8 : bytes;
        if (value < AppConfigs.KILO_BYTE) {
            return String.format(Locale.getDefault(), "%.1f " + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value);
        } else if (value < AppConfigs.MEGA_BYTE) {
            return String.format(Locale.getDefault(), "%.1f K" + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value / AppConfigs.KILO_BYTE);
        } else if (value < AppConfigs.GIGA_BYTE) {
            return String.format(Locale.getDefault(), "%.1f M" + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value / AppConfigs.MEGA_BYTE);
        } else {
            return String.format(Locale.getDefault(), "%.2f G" + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value / AppConfigs.GIGA_BYTE);
        }
    }

    public static V2rayConfig parseV2rayJsonFile(final String remark, String config, final ArrayList<String> blockedApplication) {
        final V2rayConfig v2rayConfig = new V2rayConfig();
        v2rayConfig.REMARK = remark;
        v2rayConfig.BLOCKED_APPS = blockedApplication;
        v2rayConfig.APPLICATION_ICON = AppConfigs.APPLICATION_ICON;
        v2rayConfig.APPLICATION_NAME = AppConfigs.APPLICATION_NAME;
        try {
            JSONObject config_json = new JSONObject(config);
            try {
                JSONArray inbounds = config_json.getJSONArray("inbounds");
                for (int i = 0; i < inbounds.length(); i++) {
                    try {
                        if (inbounds.getJSONObject(i).getString("protocol").equals("socks")) {
                            v2rayConfig.LOCAL_SOCKS5_PORT = inbounds.getJSONObject(i).getInt("port");
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                    try {
                        if (inbounds.getJSONObject(i).getString("protocol").equals("http")) {
                            v2rayConfig.LOCAL_HTTP_PORT = inbounds.getJSONObject(i).getInt("port");
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
            } catch (Exception e) {
                Log.w(V2rayCoreManager.class.getSimpleName(), "startCore warn => can`t find inbound port of socks5 or http.");
                return null;
            }
            try {
                v2rayConfig.CONNECTED_V2RAY_SERVER_ADDRESS = config_json.getJSONArray("outbounds").getJSONObject(0).getJSONObject("settings").getJSONArray("vnext").getJSONObject(0).getString("address");
                v2rayConfig.CONNECTED_V2RAY_SERVER_PORT = config_json.getJSONArray("outbounds").getJSONObject(0).getJSONObject("settings").getJSONArray("vnext").getJSONObject(0).getString("port");
            } catch (Exception e) {
                v2rayConfig.CONNECTED_V2RAY_SERVER_ADDRESS = config_json.getJSONArray("outbounds").getJSONObject(0).getJSONObject("settings").getJSONArray("servers").getJSONObject(0).getString("address");
                v2rayConfig.CONNECTED_V2RAY_SERVER_PORT = config_json.getJSONArray("outbounds").getJSONObject(0).getJSONObject("settings").getJSONArray("servers").getJSONObject(0).getString("port");
            }
            try {
                if (config_json.has("policy")) {
                    config_json.remove("policy");
                }
                if (config_json.has("stats")) {
                    config_json.remove("stats");
                }
            } catch (Exception ignore_error) {
                //ignore
            }
            if (AppConfigs.ENABLE_TRAFFIC_AND_SPEED_STATICS) {
                try {
                    JSONObject policy = new JSONObject();
                    JSONObject levels = new JSONObject();
                    levels.put("8", new JSONObject().put("connIdle", 300).put("downlinkOnly", 1).put("handshake", 4).put("uplinkOnly", 1));
                    JSONObject system = new JSONObject().put("statsOutboundUplink", true).put("statsOutboundDownlink", true);
                    policy.put("levels", levels);
                    policy.put("system", system);
                    config_json.put("policy", policy);
                    config_json.put("stats", new JSONObject());
                    config = config_json.toString();
                    v2rayConfig.ENABLE_TRAFFIC_STATICS = true;
                } catch (Exception e) {
                    //ignore
                }
            }
        } catch (Exception e) {
            Log.e(Utilities.class.getName(), "parseV2rayJsonFile failed => ", e);
            //ignore
            return null;
        }
        v2rayConfig.V2RAY_FULL_JSON_CONFIG = config;
        return v2rayConfig;
    }


}
