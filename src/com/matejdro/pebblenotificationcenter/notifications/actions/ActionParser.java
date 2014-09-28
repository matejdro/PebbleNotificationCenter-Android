package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import android.os.Bundle;
import com.crashlytics.android.Crashlytics;
import com.matejdro.pebblenotificationcenter.notifications.NotificationParser;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matej on 22.9.2014.
 */
@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
public class ActionParser
{
    public static void parseWearActions(Notification notification, List<NotificationAction> storage)
    {
        if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
            return;

        Bundle bundle = NotificationParser.getExtras(notification);

        if (bundle.containsKey("android.wearable.EXTENSIONS"))
        {
            Bundle bundle1 = bundle.getBundle("android.wearable.EXTENSIONS");

            if (bundle1.containsKey("actions"))
            {
                ArrayList<Bundle> actionList = (ArrayList<Bundle>) bundle1.get("actions");
                for (Bundle b : actionList)
                {
                    if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                        break;

                    NotificationAction action = WearAction.parseFromBundle(b);
                    if (action != null)
                        storage.add(action);
                }
            }

        }
    }

    @SuppressLint("NewApi")
    public static void parseNativeActions(Notification notification, List<NotificationAction> storage)
    {
        if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
            return;

        Notification.Action[] actions = getActionsField(notification);

        if (actions == null)
            return;

        for (Notification.Action action : actions)
        {
            if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                break;

            storage.add(new IntentAction(action.title.toString(), action.actionIntent));
        }
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
        } catch (IllegalAccessError e)
        {
            //Weird error that appears on some devices (Only Xiaomi reported so far) and apparently means that Notification.Action on these devices is different than usual Android.
            //Unsupported for now.
        }


        return null;
    }
}
