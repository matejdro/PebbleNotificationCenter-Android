package com.matejdro.pebblenotificationcenter.util;

import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Matej on 23.10.2014.
 */
public class BluetoothHeadsetListener extends BroadcastReceiver
{
    private static final String SETTING_HEADSET_CONNECTED = "bluetoothHeadsetConnected";

    public static boolean isHeadsetConnected(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SETTING_HEADSET_CONNECTED, false);
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(SETTING_HEADSET_CONNECTED, state == BluetoothHeadset.STATE_CONNECTED);
        editor.apply();
    }
}
