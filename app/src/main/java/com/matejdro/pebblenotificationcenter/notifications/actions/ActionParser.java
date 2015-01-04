package com.matejdro.pebblenotificationcenter.notifications.actions;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import com.crashlytics.android.Crashlytics;
import com.matejdro.pebblenotificationcenter.NotificationKey;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.notifications.NotificationParser;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;
import com.matejdro.pebblenotificationcenter.util.TextUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * Created by Matej on 22.9.2014.
 */
@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN)
public class ActionParser
{
    public static void loadActions(Notification notification, PebbleNotification pebbleNotification,  Context context)
    {
        ArrayList<NotificationAction> actions = pebbleNotification.getActions();
        if (actions == null)
            actions = new ArrayList<NotificationAction>();

        AppSettingStorage settingStorage = pebbleNotification.getSettingStorage(context);

        if (settingStorage.getInt(AppSetting.DISMISS_ON_PHONE_OPTION_LOCATION) == NotificationAction.VISIBILITY_OPTION_BEFORE_APP_OPTIONS && pebbleNotification.isDismissable())
         actions.add(new DismissOnPhoneAction(context));
        if (settingStorage.getInt(AppSetting.DISMISS_ON_PEBBLE_OPTION_LOCATION) == NotificationAction.VISIBILITY_OPTION_BEFORE_APP_OPTIONS)
            actions.add(new DismissOnPebbleAction(context));
        if (notification.contentIntent != null && settingStorage.getInt(AppSetting.OPEN_ON_PHONE_OPTION_LOCATION) == NotificationAction.VISIBILITY_OPTION_BEFORE_APP_OPTIONS)
            actions.add(new PendingIntentAction(context.getString(R.string.openOnPhone), notification.contentIntent));

        TaskerAction.addTaskerTasks(settingStorage, actions);
        IntentAction.addIntentActions(settingStorage, actions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            if (settingStorage.getBoolean(AppSetting.LOAD_WEAR_ACTIONS))
                ActionParser.parseWearActions(context, notification, pebbleNotification, actions);

            if (settingStorage.getBoolean(AppSetting.LOAD_PHONE_ACTIONS))
                ActionParser.parseNativeActions(notification, pebbleNotification, actions);
        }

        if (settingStorage.getInt(AppSetting.DISMISS_ON_PHONE_OPTION_LOCATION) == NotificationAction.VISIBILITY_OPTION_AFTER_APP_OPTIONS && pebbleNotification.isDismissable())
            actions.add(new DismissOnPhoneAction(context));
        if (settingStorage.getInt(AppSetting.DISMISS_ON_PEBBLE_OPTION_LOCATION) == NotificationAction.VISIBILITY_OPTION_AFTER_APP_OPTIONS)
            actions.add(new DismissOnPebbleAction(context));
        if (notification.contentIntent != null && settingStorage.getInt(AppSetting.OPEN_ON_PHONE_OPTION_LOCATION) == NotificationAction.VISIBILITY_OPTION_AFTER_APP_OPTIONS)
            actions.add(new PendingIntentAction(context.getString(R.string.openOnPhone), notification.contentIntent));

        pebbleNotification.setActions(actions);

    }

    public static void parseWearActions(Context context, Notification notification, PebbleNotification pebbleNotification, List<NotificationAction> storage)
    {
        try
        {
            if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                return;

            Bundle extras = NotificationParser.getExtras(notification);

            if (extras.containsKey("android.wearable.EXTENSIONS"))
            {
                Bundle wearExtras = extras.getBundle("android.wearable.EXTENSIONS");

                if (wearExtras.containsKey("actions"))
                {
                    ArrayList<?> actionList = (ArrayList<?>) wearExtras.get("actions");
                    for (Object obj : actionList)
                    {
                        if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                            break;

                        NotificationAction action = null;

                        if (obj instanceof  Bundle)
                            action = WearVoiceAction.parseFromBundle((Bundle) obj);
                        else if (obj instanceof Notification.Action)
                            action = WearVoiceAction.parseFromAction((Notification.Action) obj);

                        if (action != null)
                            storage.add(action);
                    }
                }

                if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                    return;

                if (wearExtras.containsKey("pages"))
                {
                    Parcelable[] pages = wearExtras.getParcelableArray("pages");
                    int counter = 1;
                    for (Parcelable page : pages)
                    {
                        if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                            return;

                        PebbleNotification pageNotification = NotificationHandler.getPebbleNotificationFromAndroidNotification(context, new NotificationKey(null, null, null), (Notification) page, false);
                        pageNotification.setForceSwitch(true);
                        pageNotification.setScrollToEnd(true);
                        pageNotification.setText(TextUtil.trimStringFromBack(pageNotification.getText(), NotificationSendingModule.getMaximumTextLength(pebbleNotification.getSettingStorage(context))));

                        storage.add(new NotifyAction(context.getString(R.string.wearPageAction, counter), pageNotification));

                        counter++;
                    }
                }
            }
        }
        catch (ParcelFormatException e) //Some phones (or apps?) seems to throw this when unparceling data.
        {
            Timber.w("Got ParcelFormatException at parseWearActions!");
        }
    }

    @SuppressLint("NewApi")
    public static void parseNativeActions(Notification notification, PebbleNotification pebbleNotification, List<NotificationAction> storage)
    {
        if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
            return;

        Object[] actions = getActionsField(notification);

        if (actions == null)
            return;

        //Accessing through reflection is required for 4.2 devices
        Field titleMethod;
        Field intentMethod;

        try
        {
            Class actionClass = Class.forName("android.app.Notification$Action");
            titleMethod = actionClass.getDeclaredField("title");
            intentMethod = actionClass.getDeclaredField("actionIntent");
        } catch (ClassNotFoundException e) {
            Crashlytics.logException(e);
            return;
        } catch (NoSuchFieldException e) {
            Crashlytics.logException(e);
            return;
        }


        for (Object action : actions)
        {
            if (storage.size() >= NotificationAction.MAX_NUMBER_OF_ACTIONS)
                break;

            try {
                CharSequence title = (CharSequence) titleMethod.get(action);
                PendingIntent intent = (PendingIntent) intentMethod.get(action);

                if (title == null || intent == null)
                    continue;

                storage.add(new PendingIntentAction(title.toString(), intent));

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Get the actions array from a notification using reflection. Actions were present in
     * Jellybean notifications, but the field was private until KitKat.
     */
    public static Object[] getActionsField(Notification notif) {

        try {
            Field actionsField = Class.forName("android.app.Notification").getDeclaredField("actions");
            actionsField.setAccessible(true);

            Object[] actions = (Object[]) actionsField.get(notif);
            return actions;
        } catch (IllegalAccessException e) {
            Crashlytics.logException(e);
        } catch (NoSuchFieldException e) {
            Crashlytics.logException(e);
        } catch (ClassNotFoundException e) {
            Crashlytics.logException(e);
        }

        return null;
    }
}
