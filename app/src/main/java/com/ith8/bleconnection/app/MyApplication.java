package com.ith8.bleconnection.app;

import android.app.Application;

public class MyApplication extends Application {

    private static final String PREF_NAME = "BleConnection";
    private static MyApplication mInstance;


    @Override
    public void onCreate() {
        super.onCreate();
        mInstance=this;
        mInstance.initPref();

    }

    public static MyApplication getInstance() {
        return mInstance;
    }

    private void initPref() {
        new Prefs.Builder()
                .setContext(this)
                .setMode(MODE_PRIVATE)
                .setPrefsName(PREF_NAME)
                .setUseDefaultSharedPreference(false)
                .build();
    }
}
