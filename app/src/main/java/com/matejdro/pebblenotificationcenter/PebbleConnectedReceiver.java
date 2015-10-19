package com.matejdro.pebblenotificationcenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.matejdro.pebblenotificationcenter.pebble.modules.SystemModule;

import timber.log.Timber;

public class PebbleConnectedReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Timber.d("Pebble reconnected");

        Intent serviceIntent = new Intent(context, NCTalkerService.class);
        serviceIntent.setAction(SystemModule.INTENT_PEBBLE_CONNECTED);
        context.startService(serviceIntent);
    }
}
