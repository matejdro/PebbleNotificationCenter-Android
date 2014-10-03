package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.os.Parcelable;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;

/**
 * Created by Matej on 22.9.2014.
 */
public abstract class NotificationAction implements Parcelable
{
    public static final int MAX_NUMBER_OF_ACTIONS = 20;

    public static final int VISIBILITY_OPTION_HIDDEN = 0;
    public static final int VISIBILITY_OPTION_BEFORE_APP_OPTIONS = 1;
    public static final int VISIBILITY_OPTION_AFTER_APP_OPTIONS = 2;


    protected String actionText;

    public NotificationAction(String actionText)
    {
        this.actionText = actionText;
    }

    public String getActionText()
    {
        return actionText;
    }

    public abstract void executeAction(PebbleTalkerService service, ProcessedNotification notification);
}
