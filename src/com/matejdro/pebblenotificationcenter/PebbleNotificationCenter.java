package com.matejdro.pebblenotificationcenter;

import android.content.pm.ApplicationInfo;
import com.crashlytics.android.Crashlytics;
import com.matejdro.pebblenotificationcenter.util.SettingsMemoryStorage;
import timber.log.Timber;
import timber.log.Timber.DebugTree;

public class PebbleNotificationCenter extends android.app.Application {
    public static final String PACKAGE = "com.matejdro.pebblenotificationcenter";

    public static final String APP_INCLUSION_MODE = "includeMode";
    public static final String SELECTED_PACKAGES = "CheckedApps";
    public static final String REPLACING_KEYS_LIST = "ReplacingKeys";
    public static final String REPLACING_VALUES_LIST = "ReplacingValues";
    public static final String FONT_TITLE = "fontTitle";
    public static final String FONT_SUBTITLE = "fontSubtitle";
    public static final String FONT_BODY = "fontBody";
    public static final String LIGHT_SCREEN_ON_NOTIFICATIONS = "lightScreenNotifications";
    public static final String CLOSE_TO_LAST_APP = "closeToLastApp";
    public static final String DONT_VIBRATE_WHEN_CHARGING = "noVibrateCharge";
    public static final String SHAKE_ACTION = "shakeAction";
    public static final String NO_NOTIFY_VIBRATE = "noNotificationsSilent";
    public static final String INVERT_COLORS = "invertColors";
    public static final String NOTIFICATIONS_DISABLED = "noNotifications";

    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String ALTITUDE = "altitude";
    
    
    private static SettingsMemoryStorage settingsMemoryStorage;
    
    @Override public void onCreate() {
        super.onCreate();

        boolean isDebuggable =  ( 0 != ( getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE ) );
        if (!isDebuggable)
            Crashlytics.start(this);

        Timber.plant(new DebugTree());
        Timber.d("Timber.plant()");

        settingsMemoryStorage = new SettingsMemoryStorage(this);
    }
    	
	public static SettingsMemoryStorage getInMemorySettings()
	{		
		return settingsMemoryStorage;
	}
}
