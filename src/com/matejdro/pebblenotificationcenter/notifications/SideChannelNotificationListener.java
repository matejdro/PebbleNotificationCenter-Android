package com.matejdro.pebblenotificationcenter.notifications;

import android.app.Notification;
import android.content.Intent;
import android.support.v4.app.NotificationCompatSideChannelService;
import com.matejdro.pebblenotificationcenter.NotificationKey;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;

/**
 * Created by Matej on 29.9.2014.
 */
public class SideChannelNotificationListener extends NotificationCompatSideChannelService
{
    @Override
    public void notify(String packageName, int id, String tag, Notification notification)
    {
        NotificationHandler.newNotification(this, new NotificationKey(packageName, id, tag), notification,  true);
    }

    @Override
    public void cancel(String packageName, int id, String tag)
    {
        NotificationKey key = new NotificationKey(packageName, id, tag);

        Intent intent = new Intent(this, PebbleTalkerService.class);
        intent.putExtra("dismissUpwardsKey", key);
        startService(intent);
    }

    @Override
    public void cancelAll(String packageName)
    {
        Intent intent = new Intent(this, PebbleTalkerService.class);
        intent.putExtra("dismissUpwardsPackage", packageName);
        startService(intent);
    }
}
