package com.matejdro.pebblenotificationcenter.notifications;

import android.app.Notification;
import android.support.v4.app.NotificationCompatSideChannelService;

/**
 * Created by Matej on 29.9.2014.
 */
public class SideChannelNotificationListener extends NotificationCompatSideChannelService
{
    @Override
    public void notify(String packageName, int id, String tag, Notification notification)
    {
        NotificationHandler.newNotification(this, packageName, notification, id, tag, true);
    }

    @Override
    public void cancel(String packageName, int id, String tag)
    {

    }

    @Override
    public void cancelAll(String packageName)
    {

    }
}
