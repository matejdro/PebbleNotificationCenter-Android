package com.matejdro.pebblenotificationcenter;

import android.app.Application;
import android.util.Log;

import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class PebbleNotificationCenter extends android.app.Application {
    public static final String APP_INCLUSION_MODE = "includeMode";
    public static final String SELECTED_PACKAGES = "CheckedApps";
    public static final String REGEX_INCLUSION_MODE = "regexMode";

    @Override public void onCreate() {
        super.onCreate();
        Timber.plant(new DebugTree());
        Timber.d("Timber.plant()");
    }
}
