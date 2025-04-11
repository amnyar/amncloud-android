package com.limelight;

import android.app.Application;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FontOverride.setDefaultFont("SERIF", "IRANSansWeb.ttf");
    }

    private static App instance;

    public App() {
        instance = this;
    }

    public static App getContext() {
        return instance;
    }
}
