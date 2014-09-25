package com.matejdro.pebblenotificationcenter.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.DefaultAppSettingsStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;

public class TaskerReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent == null)
            return;

        Bundle bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
        if (bundle == null)
            return;

        int action = bundle.getInt("action");


        if (action == 0) //Notification
        {
            DefaultAppSettingsStorage storage = PebbleNotificationCenter.getInMemorySettings().getDefaultSettingsStorage();
            if (!storage.canAppSendNotifications(AppSetting.VIRTUAL_APP_TASKER_RECEIVER))
                return;

            String title = bundle.getString("title");
            String subtitle = bundle.getString("subtitle");
            String body = bundle.getString("body");
            boolean noHistory = !bundle.getBoolean("storeInHistory");

            PebbleNotification notification = new PebbleNotification(title, body, AppSetting.VIRTUAL_APP_TASKER_RECEIVER);
            notification.setSubtitle(subtitle);
            notification.setNoHistory(noHistory);

            Intent startIntent = new Intent(context, PebbleTalkerService.class);
            startIntent.putExtra("notification", notification);
            context.startService(startIntent);
        }
        else if (action == 1) //Global setting modify
        {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

            for (String key : bundle.keySet())
            {
                if (!key.startsWith("setting_"))
                    continue;

                String actualSetting = key.substring(8);
                writeIntoSharedPreferences(editor, actualSetting, bundle.get(key));
            }

            editor.apply();
        }
        else if (action == 2) //PerApp Setting modify
        {
            String appPackage = bundle.getString("appPackage");

            Editor editor;
            if (appPackage.equals(AppSetting.VIRTUAL_APP_DEFAULT_SETTINGS))
                editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            else
                editor = context.getSharedPreferences("app_".concat(SharedPreferencesAppStorage.filterAppName(appPackage)), Context.MODE_PRIVATE).edit();

            for (String key : bundle.keySet())
            {
                if (!key.startsWith("setting_"))
                    continue;

                String actualSetting = key.substring(8);
                writeIntoSharedPreferences(editor, actualSetting, bundle.get(key));
            }

            editor.apply();

            if (bundle.containsKey("special_appchecked"))
            {
                boolean checked = bundle.getBoolean("special_appchecked");
                editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

                if (checked)
                    editor.putBoolean("appChecked_".concat(appPackage), true);
                else
                    editor.remove("appChecked_".concat(appPackage));

                editor.apply();
            }

        }
    }


    public static void writeIntoSharedPreferences(Editor editor, String key, Object object)
    {
        if (object instanceof Integer)
            editor.putInt(key, (Integer) object);
        else if (object instanceof Boolean)
            editor.putBoolean(key, (Boolean) object);
        else if (object instanceof String)
            editor.putString(key, (String) object);
        else if (object instanceof Float)
           editor.putFloat(key, (Float) object);
        else if (object instanceof Long)
            editor.putLong(key, (Long) object);
    }
}
