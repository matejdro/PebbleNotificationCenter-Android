package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import com.crashlytics.android.Crashlytics;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by Matej on 22.9.2014.
 */
@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
public class ActionParser
{
    @SuppressLint("NewApi")
    public static ArrayList<NotificationAction> getActions(Notification notification)
    {
        Notification.Action[] actions = getActionsField(notification);

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

    /**
     * Get the actions array from a notification using reflection. Actions were present in
     * Jellybean notifications, but the field was private until KitKat.
     */
    public static Notification.Action[] getActionsField(Notification notif) {

        try {
            Field actionsField = Notification.class.getDeclaredField("actions");
            actionsField.setAccessible(true);

            Notification.Action[] actions = (Notification.Action[]) actionsField.get(notif);
            return actions;
        } catch (IllegalAccessException e) {
            Crashlytics.logException(e);
        } catch (NoSuchFieldException e) {
            Crashlytics.logException(e);
        }

        return null;
    }
}
