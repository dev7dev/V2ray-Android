package dev.dev7.example;

import android.app.Application;
import android.graphics.BitmapFactory;

import dev.dev7.lib.v2ray.V2rayController;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        V2rayController.init(this, R.drawable.ic_launcher,"V2ray Example");
    }
}
