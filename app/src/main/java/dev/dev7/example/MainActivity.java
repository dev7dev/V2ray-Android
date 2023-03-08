package dev.dev7.example;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import dev.dev7.lib.v2ray.V2rayController;
import dev.dev7.lib.v2ray.utils.AppConfigs;

public class MainActivity extends AppCompatActivity {
    private Button connection;
    private TextView connection_speed, connection_traffic, connection_time,
            server_delay, connected_server_delay, connection_mode;
    private EditText v2ray_json_config;
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
        connection_mode = findViewById(R.id.connection_mode);
        connected_server_delay = findViewById(R.id.connected_server_delay);
        v2ray_json_config = findViewById(R.id.v2ray_json_config);

        // Checking the previous state value each time the activity is opened
        switch (V2rayController.getConnectionState().toString()) {
            case "V2RAY_CONNECTED":
                connection.setText("CONNECTED");
                break;
            case "V2RAY_DISCONNECTED":
                connection.setText("DISCONNECTED");
                break;
            case "V2RAY_CONNECTING":
                connection.setText("CONNECTING");
                break;
            default:
                break;
        }
        connection_mode.setText("connection mode : " + V2rayController.getConnectionMode() + " (tap to toggle)");
        v2ray_json_config.setText(getConfigContent());

        // Checking for access to tunneling the entire device network
        Intent intent = VpnService.prepare(getApplicationContext());
        if (intent != null) {
            // we have not permission so taking it :)
            activityResultLauncher.launch(intent);
        }

        connection.setOnClickListener(view -> {
            if (V2rayController.getConnectionState() == AppConfigs.V2RAY_STATES.V2RAY_DISCONNECTED) {
                // in StartV2ray function we can set remark to show that on notification.
                // StartV2ray function steel need json config of v2ray. Unfortunately, it does not accept URI or base64 type at the moment.
                V2rayController.StartV2ray(getApplicationContext(), "Default", v2ray_json_config.getText().toString(), null);
                connected_server_delay.setText("connected server delay : measuring...");
                //getConnectedV2rayServerDelay function need a text view for now
                new Handler().postDelayed(() -> V2rayController.getConnectedV2rayServerDelay(getApplicationContext(), connected_server_delay), 1000);
            } else {
                connected_server_delay.setText("connected server delay : wait for connection");
                V2rayController.StopV2ray(getApplicationContext());
            }
        });

        // Another way to check the connectio delay of a config without connecting to it.
        server_delay.setOnClickListener(view -> {
            server_delay.setText("server delay : measuring...");
            new Handler().post(() -> server_delay.setText("server delay : " + V2rayController.getV2rayServerDelay(v2ray_json_config.getText().toString())));
        });

        // The connection mode determines whether the entire phone is tunneled or whether an internal proxy (http , socks) is run
        connection_mode.setOnClickListener(view -> {
            // Oh,Sorry you can`t change connection mode when you have active connection. I can`t solve that for now. ):
            if (V2rayController.getConnectionState() != AppConfigs.V2RAY_STATES.V2RAY_DISCONNECTED) {
                Toast.makeText(this, "You can change the connection type only when you do not have an active connection.", Toast.LENGTH_LONG).show();
                return;
            }
            if (V2rayController.getConnectionMode() == AppConfigs.V2RAY_CONNECTION_MODES.PROXY_ONLY) {
                V2rayController.changeConnectionMode(AppConfigs.V2RAY_CONNECTION_MODES.VPN_TUN);
            } else {
                V2rayController.changeConnectionMode(AppConfigs.V2RAY_CONNECTION_MODES.PROXY_ONLY);
            }
            connection_mode.setText("connection mode : " + V2rayController.getConnectionMode() + " (tap to toggle)");
        });

        //I tested several different ways to send information from the connection process side
        // to other places (such as interfaces, AIDL and singleton ,...) apparently the best way
        // to send information is broadcast.
        // So v2ray library will be broadcast information with action CONNECTION_INFO.
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                connection_time.setText("connection time : " + arg1.getExtras().getString("DURATION"));
                connection_speed.setText("connection speed : " + arg1.getExtras().getString("UPLOAD_SPEED") + " | " + arg1.getExtras().getString("DOWNLOAD_SPEED"));
                connection_traffic.setText("connection traffic : " + arg1.getExtras().getString("UPLOAD_TRAFFIC") + " | " + arg1.getExtras().getString("DOWNLOAD_TRAFFIC"));
                switch (arg1.getExtras().getSerializable("STATE").toString()) {
                    case "V2RAY_CONNECTED":
                        connection.setText("CONNECTED");
                        break;
                    case "V2RAY_DISCONNECTED":
                        connection.setText("DISCONNECTED");
                        break;
                    case "V2RAY_CONNECTING":
                        connection.setText("CONNECTING");
                        break;
                    default:
                        break;
                }
            }
        }, new IntentFilter("CONNECTION_INFO"));

    }
    
    public static String getConfigContent() {
        return "{\n" +
                "   \"dns\":{\n" +
                "      \"hosts\":{\n" +
                "         \"domain:googleapis.cn\":\"googleapis.com\"\n" +
                "      },\n" +
                "      \"servers\":[\n" +
                "         \"1.1.1.1\"\n" +
                "      ]\n" +
                "   },\n" +
                "   \"inbounds\":[\n" +
                "      {\n" +
                "         \"listen\":\"127.0.0.1\",\n" +
                "         \"port\":10808,\n" +
                "         \"protocol\":\"socks\",\n" +
                "         \"settings\":{\n" +
                "            \"auth\":\"noauth\",\n" +
                "            \"udp\":true,\n" +
                "            \"userLevel\":8\n" +
                "         },\n" +
                "         \"sniffing\":{\n" +
                "            \"destOverride\":[\n" +
                "               \"http\",\n" +
                "               \"tls\"\n" +
                "            ],\n" +
                "            \"enabled\":true\n" +
                "         },\n" +
                "         \"tag\":\"socks\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"listen\":\"127.0.0.1\",\n" +
                "         \"port\":10809,\n" +
                "         \"protocol\":\"http\",\n" +
                "         \"settings\":{\n" +
                "            \"userLevel\":8\n" +
                "         },\n" +
                "         \"tag\":\"http\"\n" +
                "      }\n" +
                "   ],\n" +
                "   \"log\":{\n" +
                "      \"loglevel\":\"warning\"\n" +
                "   },\n" +
                "   \"outbounds\":[\n" +
                "      {\n" +
                "         \"mux\":{\n" +
                "            \"concurrency\":8,\n" +
                "            \"enabled\":false\n" +
                "         },\n" +
                "         \"protocol\":\"vmess\",\n" +
                "         \"settings\":{\n" +
                "            \"vnext\":[\n" +
                "               {\n" +
                "                  \"address\":\"amqq.ir\",\n" +
                "                  \"port\":8080,\n" +
                "                  \"users\":[\n" +
                "                     {\n" +
                "                        \"alterId\":0,\n" +
                "                        \"encryption\":\"\",\n" +
                "                        \"flow\":\"\",\n" +
                "                        \"id\":\"d149cedd-660e-43c5-812f-e28e33fe7b5d\",\n" +
                "                        \"level\":8,\n" +
                "                        \"security\":\"auto\"\n" +
                "                     }\n" +
                "                  ]\n" +
                "               }\n" +
                "            ]\n" +
                "         },\n" +
                "         \"streamSettings\":{\n" +
                "            \"network\":\"tcp\",\n" +
                "            \"security\":\"\",\n" +
                "            \"tcpSettings\":{\n" +
                "               \"header\":{\n" +
                "                  \"request\":{\n" +
                "                     \"headers\":{\n" +
                "                        \"Connection\":[\n" +
                "                           \"keep-alive\"\n" +
                "                        ],\n" +
                "                        \"Host\":[\n" +
                "                           \"ezZboX5F.divarcdn.com\"\n" +
                "                        ],\n" +
                "                        \"Pragma\":\"no-cache\",\n" +
                "                        \"Accept-Encoding\":[\n" +
                "                           \"gzip, deflate\"\n" +
                "                        ],\n" +
                "                        \"User-Agent\":[\n" +
                "                           \"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36\",\n" +
                "                           \"Mozilla/5.0 (iPhone; CPU iPhone OS 10_0_2 like Mac OS X) AppleWebKit/601.1 (KHTML, like Gecko) CriOS/53.0.2785.109 Mobile/14A456 Safari/601.1.46\"\n" +
                "                        ]\n" +
                "                     },\n" +
                "                     \"method\":\"GET\",\n" +
                "                     \"path\":[\n" +
                "                        \"/downloads/\"\n" +
                "                     ],\n" +
                "                     \"version\":\"1.1\"\n" +
                "                  },\n" +
                "                  \"type\":\"http\"\n" +
                "               }\n" +
                "            }\n" +
                "         },\n" +
                "         \"tag\":\"proxy\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"protocol\":\"freedom\",\n" +
                "         \"settings\":{\n" +
                "            \n" +
                "         },\n" +
                "         \"tag\":\"direct\"\n" +
                "      },\n" +
                "      {\n" +
                "         \"protocol\":\"blackhole\",\n" +
                "         \"settings\":{\n" +
                "            \"response\":{\n" +
                "               \"type\":\"http\"\n" +
                "            }\n" +
                "         },\n" +
                "         \"tag\":\"block\"\n" +
                "      }\n" +
                "   ],\n" +
                "   \"routing\":{\n" +
                "      \"domainMatcher\":\"mph\",\n" +
                "      \"domainStrategy\":\"IPIfNonMatch\",\n" +
                "      \"rules\":[\n" +
                "         {\n" +
                "            \"ip\":[\n" +
                "               \"1.1.1.1\"\n" +
                "            ],\n" +
                "            \"outboundTag\":\"proxy\",\n" +
                "            \"port\":\"53\",\n" +
                "            \"type\":\"field\"\n" +
                "         }\n" +
                "      ]\n" +
                "   }\n" +
                "}";
    }


}