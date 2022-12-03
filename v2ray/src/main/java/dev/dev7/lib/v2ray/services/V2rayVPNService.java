package dev.dev7.lib.v2ray.services;

import static dev.dev7.lib.v2ray.core.utils.APPLICATION_NAME;
import static dev.dev7.lib.v2ray.core.utils.BLOCKED_APPS;
import static dev.dev7.lib.v2ray.core.utils.FLAG_VPN_START;
import static dev.dev7.lib.v2ray.core.utils.FLAG_VPN_STOP;
import static dev.dev7.lib.v2ray.core.utils.LOCAL_SOCKS5_PORT;
import static dev.dev7.lib.v2ray.core.utils.V2RAY_TUN_CONNECTED;
import static dev.dev7.lib.v2ray.core.utils.V2RAY_TUN_CONNECTING;
import static dev.dev7.lib.v2ray.core.utils.V2RAY_TUN_DISCONNECTED;
import static dev.dev7.lib.v2ray.core.utils.convertTwoDigit;
import static dev.dev7.lib.v2ray.core.utils.parseTraffic;

import android.app.Notification;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.TrafficStats;
import android.net.VpnService;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import dev.dev7.lib.v2ray.core.V2rayCore;
import dev.dev7.lib.v2ray.core.utils;
import dev.dev7.lib.v2ray.interfaces.FBProtectListener;
import dev.dev7.lib.v2ray.interfaces.V2rayStatsListener;

public class V2rayVPNService extends VpnService implements FBProtectListener, Runnable {
    private ParcelFileDescriptor mInterface;
    private Process process;
    private Thread mThread;

    public static int V2RAY_TUN_STATE = V2RAY_TUN_DISCONNECTED;
    public static String SERVICE_DURATION = "00:00:00";
    private int seconds, minutes, hours;
    private static final ArrayList<V2rayStatsListener> v2rayStatsListeners = new ArrayList<>();
    private CountDownTimer countDownTimer;
    private long baseRx = 0;
    private long baseTx = 0;
    private long mStartRX = 0;
    private long mStartTX = 0;
    private final int CHECK_V2RAY_DELAY_FINAL_STEP = 4;
    private int CHECK_V2RAY_DELAY_STEPS = 0;

    public static void registerStatsListener(final V2rayStatsListener v2rayStatsListener) {
        v2rayStatsListeners.add(v2rayStatsListener);
    }

    public static void unRegisterStatsListeners() {
        v2rayStatsListeners.clear();
    }


    private void makeDurationTimer() {
        baseRx = TrafficStats.getTotalRxBytes();
        baseTx = TrafficStats.getTotalTxBytes();
        mStartRX = TrafficStats.getTotalRxBytes();
        mStartTX = TrafficStats.getTotalTxBytes();
        countDownTimer = new CountDownTimer(300000000, 1000) {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onTick(long millisUntilFinished) {
                long downSpeed = TrafficStats.getTotalRxBytes() - mStartRX;
                long upSpeed = TrafficStats.getTotalTxBytes() - mStartTX;
                long totalDownload = TrafficStats.getTotalRxBytes() - baseRx;
                long totalUpload = TrafficStats.getTotalTxBytes() - baseTx;
                seconds++;
                if (seconds == 59) {
                    minutes++;
                    seconds = 0;
                }
                if (minutes == 59) {
                    minutes = 0;
                    hours++;
                }
                if (hours == 23) {
                    hours = 0;
                }
                SERVICE_DURATION = convertTwoDigit(hours) + ":" + convertTwoDigit(minutes) + ":" + convertTwoDigit(seconds);
                if (v2rayStatsListeners.size() > 0) {
                    for (int i = 0; i < v2rayStatsListeners.size(); i++) {
                        v2rayStatsListeners.get(i).onStatsCommit(V2RAY_TUN_STATE, SERVICE_DURATION, parseTraffic(downSpeed, false, true), parseTraffic(upSpeed, false, true)
                                , parseTraffic(totalDownload, false, false), parseTraffic(totalUpload, false, false));
                    }
                }
                mStartRX = TrafficStats.getTotalRxBytes();
                mStartTX = TrafficStats.getTotalTxBytes();
            }

            public void onFinish() {
                countDownTimer.cancel();
                if (V2RAY_TUN_STATE != V2RAY_TUN_DISCONNECTED) {
                    makeDurationTimer();
                }
            }
        }.start();
    }

    @Override
    public boolean onProtect(int socket) {
        return protect(socket);
    }

    @Override
    public void run() {
        V2rayCore.fbProtectListener = this;
        configure();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        SERVICE_DURATION = "00:00:00";
        if (v2rayStatsListeners.size() > 0) {
            for (int i = 0; i < v2rayStatsListeners.size(); i++) {
                v2rayStatsListeners.get(i).onStatsCommit(V2RAY_TUN_STATE, SERVICE_DURATION, parseTraffic(0, false, true), parseTraffic(0, false, true)
                        , parseTraffic(0, false, false), parseTraffic(0, false, false));
            }
        }
        makeDurationTimer();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startForeground(2, utils.getVpnServiceNotification(this));
        else
            startForeground(1, new Notification());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        V2RAY_TUN_STATE = V2RAY_TUN_CONNECTING;
        int action = intent.getIntExtra("ACTION", 0);
        if (action == FLAG_VPN_START) {
            String config = intent.getStringExtra("V2RAY_CONFIG");
            if (config == null) {
                this.onDestroy();
            }
            if (V2rayCore.isV2rayRunning()) {
                V2rayCore.stop();
            }
            if (!V2rayCore.start(getApplicationContext(), config)) {
                this.onDestroy();
            } else {
                if (mThread != null) {
                    mThread.interrupt();
                }
                mThread = new Thread(this, "VPN_THREAD");
                mThread.start();
            }
        } else if (action == FLAG_VPN_STOP) {
            this.onDestroy();
            stopSelf();
        }
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        V2rayCore.stop();
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        if (process != null) {
            process.destroy();
            process = null;
        }
        if (mInterface != null) {
            try {
                mInterface.close();
                mInterface = null;
            } catch (IOException e) { /* Ignore a close error here */ }
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        CHECK_V2RAY_DELAY_STEPS = 0;
        V2RAY_TUN_STATE = V2RAY_TUN_DISCONNECTED;
        SERVICE_DURATION = "00:00:00";
        if (v2rayStatsListeners.size() > 0) {
            for (int i = 0; i < v2rayStatsListeners.size(); i++) {
                v2rayStatsListeners.get(i).onStatsCommit(V2RAY_TUN_STATE, SERVICE_DURATION, parseTraffic(0, false, true), parseTraffic(0, false, true)
                        , parseTraffic(0, false, false), parseTraffic(0, false, false));
            }
        }
        stopSelf();
    }

    @Override
    public void onRevoke() {
        onDestroy();
    }

    private void configure() {
        if (mInterface != null) {
            return;
        }
        try {
            Builder builder = new Builder();
            builder.setSession(APPLICATION_NAME);
            builder.setMtu(1500);
            builder.addAddress("26.26.26.1", 24);
            builder.addRoute("0.0.0.0", 0);
            if (BLOCKED_APPS != null) {
                for (int i = 0; i < BLOCKED_APPS.size(); i++) {
                    builder.addDisallowedApplication(BLOCKED_APPS.get(i));
                }
            }
//            builder.allowFamily(android.system.OsConstants.AF_INET6); prevent ipv6 leak
            mInterface = builder.establish();
            if (mInterface == null) {
                this.onDestroy();
                return;
            }
            runTun2socks();
        } catch (Exception e) {
            Log.e("VPN_SERVICE", "FAILED=>", e);
            this.onDestroy();
        }
    }


    private void runTun2socks() {
        ArrayList<String> cmd = new ArrayList<>(Arrays.asList(new File(getApplicationInfo().nativeLibraryDir, "libtun2socks.so").getAbsolutePath(),
                "--netif-ipaddr", "26.26.26.2",
                "--netif-netmask", "255.255.255.252",
                "--socks-server-addr", "127.0.0.1:" + LOCAL_SOCKS5_PORT,
                "--tunmtu", "1500",
                "--sock-path", "sock_path",
                "--enable-udprelay",
                "--loglevel", "error"));
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.directory(getApplicationContext().getFilesDir()).start();
            sendFd();
        } catch (Exception e) {
            Log.e("VPN_SERVICE", "FAILED=>", e);
            this.onDestroy();
        }
    }

    private void sendFd() {
        String localSocksFile = new File(getApplicationContext().getFilesDir(), "sock_path").getAbsolutePath();
        LocalSocket clientSocket = new LocalSocket();
        FileDescriptor tunFd = mInterface.getFileDescriptor();
        try {
            Thread.sleep(1000);
            clientSocket.connect(new LocalSocketAddress(localSocksFile, LocalSocketAddress.Namespace.FILESYSTEM));
            if (!clientSocket.isConnected()) {
                Log.e("SOCK_FILE", "Unable to connect to localSocksFile [" + localSocksFile + "]");
            }
            OutputStream clientOutStream = clientSocket.getOutputStream();
            clientSocket.setFileDescriptorsForSend(new FileDescriptor[]{tunFd});
            clientOutStream.write(32);
            clientSocket.setFileDescriptorsForSend(null);
            clientSocket.shutdownOutput();
            clientSocket.close();
            if (V2rayCore.isV2rayRunning()) {
                checkV2rayConnection();
            } else {
                onDestroy();
            }
            Log.e("V2rayService", "sendFD ENDED");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("SOCK_FILE", "Unable to connect to localSocksFile [" + localSocksFile + "]");
            this.onDestroy();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e("SOCK_FILE", "FAILED=>", e);
            this.onDestroy();
        }
    }

    Timer checkV2rayConnectionTimer = new Timer();
    private void checkV2rayConnection() {
        checkV2rayConnectionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.e("V2rayService", "checking connection delay");
                if (V2rayCore.getV2rayDelay() > 0){
                    V2RAY_TUN_STATE = V2RAY_TUN_CONNECTED;
                    checkV2rayConnectionTimer.cancel();
                }else {
                    if (CHECK_V2RAY_DELAY_STEPS == CHECK_V2RAY_DELAY_FINAL_STEP){
                        checkV2rayConnectionTimer.cancel();
                        onDestroy();
                    }
                    CHECK_V2RAY_DELAY_STEPS++;
                }
            }
        }, 0, 1000);
    }

}
