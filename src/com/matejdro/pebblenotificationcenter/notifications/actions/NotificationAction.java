package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.os.Parcelable;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;

/**
 * Created by Matej on 22.9.2014.
 */
public abstract class NotificationAction implements Parcelable
{
    protected String actionText;

    public NotificationAction(String actionText)
    {
        this.actionText = actionText;
    }

    public String getActionText()
    {
        return actionText;
    }

    public abstract void executeAction(PebbleTalkerService service);
}
