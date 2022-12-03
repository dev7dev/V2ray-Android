package dev.dev7.example;

import static dev.dev7.lib.v2ray.core.utils.V2RAY_TUN_CONNECTED;
import static dev.dev7.lib.v2ray.core.utils.V2RAY_TUN_CONNECTING;
import static dev.dev7.lib.v2ray.core.utils.V2RAY_TUN_DISCONNECTED;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import dev.dev7.lib.v2ray.V2rayController;

public class MainActivity extends AppCompatActivity {
    private Button connection;
    private TextView connection_speed, connection_traffic, connection_time, server_delay, core_state;

    private int LAST_V2RAY_STATE = 2313;
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK) {
                    Toast.makeText(this, "Permission not granted.", Toast.LENGTH_SHORT).show();
                }
            });

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connection = findViewById(R.id.btn_connection);
        connection_speed = findViewById(R.id.connection_speed);
        connection_time = findViewById(R.id.connection_duration);
        connection_traffic = findViewById(R.id.connection_traffic);
        server_delay = findViewById(R.id.server_delay);
        core_state = findViewById(R.id.core_state);

        Intent intent = VpnService.prepare(getApplicationContext());
        if (intent != null) {
            activityResultLauncher.launch(intent);
        }

        V2rayController.registerV2rayStatsListener((v2ray_state, duration, downloadSpeed, uploadSpeed, totalDownload, totalUpload) -> {
            try {
                if (v2ray_state != LAST_V2RAY_STATE) {
                    switch (v2ray_state) {
                        case V2RAY_TUN_DISCONNECTED:
                            connection.setText("DISCONNECTED");
                            server_delay.setText("server delay : wait for connection");
                            break;
                        case V2RAY_TUN_CONNECTING:
                            connection.setText("CONNECTING...");
                            server_delay.setText("server delay : wait for connection");
                            break;
                        case V2RAY_TUN_CONNECTED:
                            connection.setText("CONNECTED");
                            server_delay.setText("server delay : measuring...");
                            server_delay.setText("server delay : " + V2rayController.getServerDelay());
                            break;
                        default:
                            connection.setText("UNKNOWN");
                            break;
                    }
                    LAST_V2RAY_STATE = v2ray_state;
                }
                core_state.setText("v2ray core state : " + (V2rayController.isV2raySocketRunning() ? "Running" : "Stop"));
                connection_time.setText("connection time : " + duration);
                connection_speed.setText("connection speed : " + downloadSpeed + " | " + uploadSpeed);
                connection_traffic.setText("connection traffic : " + totalDownload + " | " + totalUpload);
            } catch (Exception e) {
                //ignore if layout elements have not a context
            }
        });
        connection.setOnClickListener(view -> {
            if (V2rayController.getV2rayState() == V2RAY_TUN_DISCONNECTED) {
                V2rayController.StartV2ray(getApplicationContext(), getConfigContent(), null);
            } else {
                V2rayController.StopV2ray(getApplicationContext());
            }
        });
    }


    public static String getConfigContent() {
       return "{\n" +
               "  \"dns\": {\n" +
               "    \"hosts\": {\n" +
               "      \"domain:googleapis.cn\": \"googleapis.com\"\n" +
               "    },\n" +
               "    \"servers\": [\n" +
               "      \"1.1.1.1\"\n" +
               "    ]\n" +
               "  },\n" +
               "  \"inbounds\": [\n" +
               "    {\n" +
               "      \"listen\": \"127.0.0.1\",\n" +
               "      \"port\": 10808,\n" +
               "      \"protocol\": \"socks\",\n" +
               "      \"settings\": {\n" +
               "        \"auth\": \"noauth\",\n" +
               "        \"udp\": true,\n" +
               "        \"userLevel\": 8\n" +
               "      },\n" +
               "      \"sniffing\": {\n" +
               "        \"destOverride\": [],\n" +
               "        \"enabled\": false\n" +
               "      },\n" +
               "      \"tag\": \"socks\"\n" +
               "    },\n" +
               "    {\n" +
               "      \"listen\": \"127.0.0.1\",\n" +
               "      \"port\": 10809,\n" +
               "      \"protocol\": \"http\",\n" +
               "      \"settings\": {\n" +
               "        \"userLevel\": 8\n" +
               "      },\n" +
               "      \"tag\": \"http\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"log\": {\n" +
               "    \"loglevel\": \"warning\"\n" +
               "  },\n" +
               "  \"outbounds\": [\n" +
               "    {\n" +
               "      \"mux\": {\n" +
               "        \"concurrency\": 8,\n" +
               "        \"enabled\": false\n" +
               "      },\n" +
               "      \"protocol\": \"vmess\",\n" +
               "      \"settings\": {\n" +
               "        \"vnext\": [\n" +
               "          {\n" +
               "            \"address\": \"d1.dev7.dev\",\n" +
               "            \"port\": 443,\n" +
               "            \"users\": [\n" +
               "              {\n" +
               "                \"alterId\": 0,\n" +
               "                \"encryption\": \"\",\n" +
               "                \"flow\": \"\",\n" +
               "                \"id\": \"53ecdecc-d078-414d-80b4-dd6f81d157dc\",\n" +
               "                \"level\": 8,\n" +
               "                \"security\": \"auto\"\n" +
               "              }\n" +
               "            ]\n" +
               "          }\n" +
               "        ]\n" +
               "      },\n" +
               "      \"streamSettings\": {\n" +
               "        \"network\": \"ws\",\n" +
               "        \"security\": \"tls\",\n" +
               "        \"tlsSettings\": {\n" +
               "          \"allowInsecure\": false,\n" +
               "          \"fingerprint\": \"\",\n" +
               "          \"serverName\": \"d1.dev7.dev\"\n" +
               "        },\n" +
               "        \"wsSettings\": {\n" +
               "          \"headers\": {\n" +
               "            \"Host\": \"d1.dev7.dev\"\n" +
               "          },\n" +
               "          \"path\": \"/ddf5aa4/\"\n" +
               "        }\n" +
               "      },\n" +
               "      \"tag\": \"proxy\"\n" +
               "    },\n" +
               "    {\n" +
               "      \"protocol\": \"freedom\",\n" +
               "      \"settings\": {},\n" +
               "      \"tag\": \"direct\"\n" +
               "    },\n" +
               "    {\n" +
               "      \"protocol\": \"blackhole\",\n" +
               "      \"settings\": {\n" +
               "        \"response\": {\n" +
               "          \"type\": \"http\"\n" +
               "        }\n" +
               "      },\n" +
               "      \"tag\": \"block\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"routing\": {\n" +
               "    \"domainMatcher\": \"mph\",\n" +
               "    \"domainStrategy\": \"IPIfNonMatch\",\n" +
               "    \"rules\": [\n" +
               "      {\n" +
               "        \"ip\": [\n" +
               "          \"1.1.1.1\"\n" +
               "        ],\n" +
               "        \"outboundTag\": \"proxy\",\n" +
               "        \"port\": \"53\",\n" +
               "        \"type\": \"field\"\n" +
               "      }\n" +
               "    ]\n" +
               "  }\n" +
               "}";
    }

    @Override
    protected void onDestroy() {
        V2rayController.unRegisterV2rayStatsListeners();
        super.onDestroy();
    }
}