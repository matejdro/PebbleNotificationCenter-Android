package com.matejdro.pebblenotificationcenter.appsetting;

import android.content.SharedPreferences;

import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;

import java.util.UUID;

public class PebbleAppNotificationSettings
{
    public static int getPebbleAppNotificationMode(SharedPreferences preferences, UUID uuid)
    {
        if (uuid.equals(PebbleNotificationCenter.WATCHAPP_UUID)) //When NC is open, send NC notifications
            return 0;

        int mode = preferences.getInt("pebble_app_mode_".concat(uuid.toString()), -1);
        if (mode == -1)
            mode = preferences.getInt("pebble_app_mode_default", 0);

        if (mode < 0 || mode > 2)
            mode = 0;

        return mode;
    }

    public static void setPebbleAppNotificationMode(SharedPreferences.Editor editor, UUID uuid, int mode)
    {
        editor.putInt("pebble_app_mode_".concat(uuid.toString()), mode);
        editor.apply();
    }

}
