package dev.dev7.lib.v2ray.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;

public class utils {
    public static ArrayList<String> BLOCKED_APPS = null;
    public static String APPLICATION_NAME;
    public static int APPLICATION_ICON;
    public static final int LOCAL_SOCKS5_PORT = 10808;
    public static final int FLAG_VPN_START = 3198;
    public static final int FLAG_VPN_STOP = 3149;

    public static final int V2RAY_TUN_CONNECTED = 3813;
    public static final int V2RAY_TUN_CONNECTING = 4309;
    public static final int V2RAY_TUN_DISCONNECTED = 4329;


    private static final long B = 1;
    private static final long KB = B * 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;


    public static void CopyFiles(InputStream src, File dst) throws IOException {
        try (OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = src.read(buf)) > 0) {
                out.write(buf, 0, len);
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
            for (String assets_obj : context.getAssets().list("")) {
                if (geo.contains(assets_obj)) {
                    CopyFiles(context.getAssets().open(assets_obj), new File(extFolder, assets_obj));
                }
            }
            Log.e("COPY_ASSETS", "SUCCESS");
        } catch (Exception e) {
            Log.e("COPY_ASSETS", "FAILED=>", e);
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    public static Notification getVpnServiceNotification(final Context context) {
        String NOTIFICATION_CHANNEL_ID = context.getPackageName();
        String channelName = APPLICATION_NAME + " Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        //       Alternative get application icon
        //       .setSmallIcon(context.getApplicationInfo().icon)
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getApplicationInfo().packageName);
        launchIntent.setAction("FROM_DISCONNECT_BTN");
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE);
        return notificationBuilder.setOngoing(true)
                .setSmallIcon(APPLICATION_ICON)
                .setContentTitle("Connected To " + APPLICATION_NAME)
                .setContentText("tap to open application")
                .setContentIntent(launchPendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }


    public static String convertTwoDigit(int value) {
        if (value < 10) return "0" + value;
        else return value + "";
    }

    public static String parseTraffic(final double bytes, final boolean inBits, final boolean isMomentary) {
        double value = inBits ? bytes * 8 : bytes;
        if (value < KB) {
            return String.format(Locale.getDefault(), "%.1f " + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value);
        } else if (value < MB) {
            return String.format(Locale.getDefault(), "%.1f K" + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value / KB);
        } else if (value < GB) {
            return String.format(Locale.getDefault(), "%.1f M" + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value / MB);
        } else {
            return String.format(Locale.getDefault(), "%.2f G" + (inBits ? "b" : "B") + (isMomentary ? "/s" : ""), value / GB);
        }
    }


}
