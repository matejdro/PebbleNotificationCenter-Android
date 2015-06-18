package com.matejdro.pebblenotificationcenter;

import android.content.pm.ApplicationInfo;
import com.crashlytics.android.Crashlytics;
import com.matejdro.pebblecommons.PebbleCompanionApplication;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebblecommons.util.LogWriter;
import com.matejdro.pebblenotificationcenter.util.SettingsMemoryStorage;

import io.fabric.sdk.android.Fabric;
import java.util.Map;
import java.util.UUID;

public class PebbleNotificationCenter extends PebbleCompanionApplication
{
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
    public static final String VIBRATION_DISABLED = "noVibration";


    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String ALTITUDE = "altitude";

    public final static UUID WATCHAPP_UUID = UUID.fromString("0a7575eb-e5b9-456b-8701-3eacb62d74f1");

    private static SettingsMemoryStorage settingsMemoryStorage;
    
    @Override public void onCreate() {
        super.onCreate();

        boolean isDebuggable =  ( 0 != ( getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE ) );
        if (!isDebuggable)
            Fabric.with(this, new Crashlytics());

        settingsMemoryStorage = new SettingsMemoryStorage(this);
        LogWriter.init(settingsMemoryStorage.getSharedPreferences(), "NotificationCenter");
    }
    	
	public static SettingsMemoryStorage getInMemorySettings()
	{		
		return settingsMemoryStorage;
	}

    @Override
    public UUID getPebbleAppUUID()
    {
        return WATCHAPP_UUID;
    }

    @Override
    public Class<? extends PebbleTalkerService> getTalkerServiceClass()
    {
        return NCTalkerService.class;
    }

    @Override
    public Map<String, String> getTextReplacementTable()
    {
        return settingsMemoryStorage.getReplacingStrings();
    }
}
