package com.matejdro.pebblenotificationcenter.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

/**
 * @author Ben Grynhaus on 24/12/2014.
 */
public class DeviceUtil {

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    /**
     * Use this method to determine the state of the screen.
     * calling PowerManager.isScreenOn doesn't guarantee that the device will go into deep-sleep mode afterwards
     * and might keep the device awake.
     * @see http://developer.android.com/reference/android/os/PowerManager.html#isScreenOn()
     */
    public static boolean isScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
            return pm.isInteractive();
        else
            //noinspection deprecation
            return pm.isScreenOn();
    }
}