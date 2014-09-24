package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import java.util.ArrayList;

/**
 * Created by Matej on 22.9.2014.
 */
@TargetApi(value = Build.VERSION_CODES.KITKAT)
public class ActionParser
{
    public static ArrayList<NotificationAction> getActions(Notification notification)
    {
        Notification.Action[] actions = notification.actions;

        if (actions == null)
            return null;

        ArrayList<NotificationAction> pebbleActions = new ArrayList<NotificationAction>(actions.length);

        for (Notification.Action action : actions)
        {
            pebbleActions.add(new IntentAction(action.title.toString(), action.actionIntent));
            if (pebbleActions.size() >= 5)
                break;
        }

        return pebbleActions;
    }
}
