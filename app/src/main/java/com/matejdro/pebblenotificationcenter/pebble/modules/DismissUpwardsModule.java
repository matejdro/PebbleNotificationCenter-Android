package com.matejdro.pebblenotificationcenter.pebble.modules;

import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblecommons.pebble.CommModule;
import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebblecommons.pebble.PebbleUtil;
import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.NotificationKey;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import com.matejdro.pebblenotificationcenter.pebble.NotificationCenterDeveloperConnection;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import timber.log.Timber;

/**
 * Created by Matej on 4.12.2014.
 */
public class DismissUpwardsModule extends CommModule
{
    public static final int MODULE_DISMISS_UPWARDS = 3;
    public static final String INTENT_DISMISS_NOTIFICATION = "DismissOneNotification";
    public static final String INTENT_DISMISS_PEBBLE_ID = "DismissPebbleID";
    public static final String INTENT_DISMISS_PACKAGE = "DismissOneNotification";
    public static final String INTENT_DISMISS_NOTIFICATION_ID = "DismissOneExactID";

    private Queue<Integer> dismissQueue = new LinkedList<Integer>();

    public DismissUpwardsModule(PebbleTalkerService service)
    {
        super(service);

        service.registerIntent(INTENT_DISMISS_NOTIFICATION, this);
        service.registerIntent(INTENT_DISMISS_PACKAGE, this);
        service.registerIntent(INTENT_DISMISS_PEBBLE_ID, this);
        service.registerIntent(INTENT_DISMISS_NOTIFICATION_ID, this);
    }

    private void sendDismiss(Integer id)
    {
        Timber.d("Dismissing upwards $d", id);

        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 3);
        data.addUint8(1, (byte) 0);
        data.addInt32(2, id);
        data.addUint8(3, (byte) (NotificationSendingModule.get(getService()).isAnyNotificationWaiting() ? 1 : 0));
        
        getService().getPebbleCommunication().sendToPebble(data);
    }

    public void queueDismiss(Integer id)
    {
        Timber.d("Queueing dismiss packet for notification %d", id);

        ProcessedNotification notification = NCTalkerService.fromPebbleTalkerService(getService()).sentNotifications.get(id);
        if (notification == null)
        {
            Timber.w("Invalid notification ID!");
            return;
        }

        if (notification.nativeNotification)
        {
            PebbleKit.FirmwareVersionInfo watchfirmware = PebbleUtil.getPebbleFirmwareVersion(getService());
            if (watchfirmware != null && watchfirmware.getMajor() > 2) //Dismissing is only supported on Pebble 3.0+
            {
                // Dismissing native notifications is apparently performing by re-sending notification with dismissable flag set to false.
                NotificationCenterDeveloperConnection.fromDevConn(getService().getDeveloperConnection()).sendSDK3Notification(notification, false);
            }

        }
        else
        {
            dismissQueue.add(id);
            PebbleCommunication communication = getService().getPebbleCommunication();
            communication.queueModule(this);
            communication.sendNext();
        }
    }


    @Override
    public boolean sendNextMessage()
    {
        if (dismissQueue.isEmpty())
            return false;

        Integer nextDismiss = dismissQueue.poll();
        sendDismiss(nextDismiss);

        return true;
    }

    public void dismissUpwards(ProcessedNotification notification)
    {
        queueDismiss(notification.id);
        NCTalkerService.fromPebbleTalkerService(getService()).sentNotifications.remove(notification.id);
        NotificationSendingModule.get(getService()).removeNotificationFromSendingQueue(notification.id);

    }

    /**
     * @param dismissImmediately when <code>false</code>, dismiss will be queued and will execute with next intent
     */
    public void processDismissUpwards(NotificationKey key, boolean dismissImmediately)
    {
        Timber.d("got dismiss: %s", key);

        if (key == null || key.getAndroidId() == null)
            return;

        AppSettingStorage settingsStorage;
        if (key.getPackage() == null)
            settingsStorage = NCTalkerService.fromPebbleTalkerService(getService()).getDefaultSettingsStorage();
        else
            settingsStorage = new SharedPreferencesAppStorage(getService(), key.getPackage(), NCTalkerService.fromPebbleTalkerService(getService()).getDefaultSettingsStorage());

        boolean syncDismissUp = settingsStorage.getBoolean(AppSetting.DISMISS_UPRWADS);
        if (!syncDismissUp)
            return;

        SparseArray<ProcessedNotification> sentNotifications = NCTalkerService.fromPebbleTalkerService(getService()).sentNotifications;

        for (int i = 0; i < sentNotifications.size(); i++)
        {
            ProcessedNotification notification = sentNotifications.valueAt(i);

            if (!notification.source.isListNotification() && notification.source.isSameNotification(key))
            {
                if (dismissImmediately)
                {
                    dismissUpwards(notification);
                    dismissSimilarWearNotifications(notification);
                    i--;
                }
                else
                {
                    dismissProcessedNotification(getService(), notification.id);
                }
            }
        }
    }

    /**
     * Also dismiss related group messages from this notification (some apps have trouble with dismissing to side channel directly)
     */
    public void dismissSimilarWearNotifications(ProcessedNotification notification)
    {

        if (notification.source.getWearGroupType() == PebbleNotification.WEAR_GROUP_TYPE_GROUP_SUMMARY)
        {
            SparseArray<ProcessedNotification> sentNotifications = NCTalkerService.fromPebbleTalkerService(getService()).sentNotifications;

            for (int i = 0; i < sentNotifications.size(); i++)
            {
                ProcessedNotification compare = sentNotifications.valueAt(i);

                if (notification.source.isInSameGroup(compare.source) && compare.source.getWearGroupType() == PebbleNotification.WEAR_GROUP_TYPE_GROUP_MESSAGE)
                {
                    dismissNotification(getService(), compare.source.getKey());
                }
            }
        }
    }

    public void processDismissUpwardsWholePackage(String pkg)
    {
        Timber.d("got dismiss package: %s", pkg);

        if (pkg == null)
            return;

        AppSettingStorage settingsStorage = new SharedPreferencesAppStorage(getService(), pkg, NCTalkerService.fromPebbleTalkerService(getService()).getDefaultSettingsStorage());

        boolean syncDismissUp = settingsStorage.getBoolean(AppSetting.DISMISS_UPRWADS);
        if (!syncDismissUp)
            return;

        SparseArray<ProcessedNotification> sentNotifications = NCTalkerService.fromPebbleTalkerService(getService()).sentNotifications;
        for (int i = 0; i < sentNotifications.size(); i++)
        {
            ProcessedNotification notification = sentNotifications.valueAt(i);

            if (!notification.source.isListNotification() && notification.source.getKey().getPackage().equals(pkg))
            {
                dismissUpwards(notification);
                i--;
            }
        }
    }

    @Override
    public void gotIntent(Intent intent)
    {
        if (intent.getAction().equals(INTENT_DISMISS_NOTIFICATION))
        {
            NotificationKey key = (NotificationKey) intent.getParcelableExtra("key");
            processDismissUpwards(key, true);
        }
        else if (intent.getAction().equals(INTENT_DISMISS_PACKAGE))
        {
            String pkg = intent.getStringExtra("package");
            processDismissUpwardsWholePackage(pkg);
        }
        else if (intent.getAction().equals(INTENT_DISMISS_PEBBLE_ID))
        {
            int id = intent.getIntExtra("id", -1);
            queueDismiss(id);
        }
        else if (intent.getAction().equals(INTENT_DISMISS_NOTIFICATION_ID))
        {
            int id = intent.getIntExtra("id", -1);

            ProcessedNotification notification = NCTalkerService.fromPebbleTalkerService(getService()).sentNotifications.get(id);
            if (notification == null)
            {
                Timber.w("Invalid notification ID!");
                return;
            }

            dismissUpwards(notification);
        }

    }

    @Override
    public void pebbleAppOpened()
    {
        //If Pebble app just opened, it means all notifications are gone, except one that might still be in sending
        ProcessedNotification notificationInSending = NotificationSendingModule.get(getService()).getCurrrentSendingNotification();
        if (notificationInSending == null)
        {
            dismissQueue.clear();
            return;
        }

        Iterator<Integer> queueIterator = dismissQueue.iterator();
        while (queueIterator.hasNext())
        {
            int id = queueIterator.next();
            if (id != notificationInSending.id)
                queueIterator.remove();
        }
    }

    @Override
    public void gotMessageFromPebble(PebbleDictionary message)
    {
    }

    public static void dismissPebbleID(Context context, int id)
    {
        Intent intent = new Intent(context, NCTalkerService.class);
        intent.setAction(INTENT_DISMISS_PEBBLE_ID);
        intent.putExtra("id", id);

        context.startService(intent);
    }

    public static void dismissNotification(Context context, NotificationKey key)
    {
        Intent intent = new Intent(context, NCTalkerService.class);
        intent.setAction(INTENT_DISMISS_NOTIFICATION);
        intent.putExtra("key", key);

        context.startService(intent);
    }

    public static void dismissProcessedNotification(Context context, int id)
    {
        Intent intent = new Intent(context, NCTalkerService.class);
        intent.setAction(INTENT_DISMISS_NOTIFICATION_ID);
        intent.putExtra("id", id);

        context.startService(intent);
    }


    public static void dismissWholePackage(Context context, String pkg)
    {
        Intent intent = new Intent(context, NCTalkerService.class);
        intent.setAction(INTENT_DISMISS_PACKAGE);
        intent.putExtra("package", pkg);

        context.startService(intent);

    }

    public static DismissUpwardsModule get(PebbleTalkerService service)
    {
        return (DismissUpwardsModule) service.getModule(MODULE_DISMISS_UPWARDS);
    }

}
