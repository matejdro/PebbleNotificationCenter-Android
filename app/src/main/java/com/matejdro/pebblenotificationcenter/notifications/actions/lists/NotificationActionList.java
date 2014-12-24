package com.matejdro.pebblenotificationcenter.notifications.actions.lists;

import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import java.util.List;

/**
 * Created by Matej on 2.10.2014.
 */
public class NotificationActionList extends ActionList
{
    private ProcessedNotification notification;

    public NotificationActionList(ProcessedNotification notification)
    {
        this.notification = notification;
    }

    @Override
    public int getNumberOfItems()
    {
        if (notification.source.getActions() == null)
            return 0;

        return notification.source.getActions().size();
    }

    @Override
    public String getItem(int id)
    {
        NotificationAction action = notification.source.getActions().get(id);
        if (action == null)
            return "";

        return action.getActionText();
    }

    @Override
    public boolean itemPicked(PebbleTalkerService service, int id)
    {
        NotificationAction action = notification.source.getActions().get(id);
        if (action == null)
            return false;

        return action.executeAction(service, notification);
    }
}
