package com.matejdro.pebblenotificationcenter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.SparseArray;
import com.crashlytics.android.Crashlytics;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.DefaultAppSettingsStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import com.matejdro.pebblenotificationcenter.lists.ActiveNotificationsAdapter;
import com.matejdro.pebblenotificationcenter.lists.NotificationHistoryAdapter;
import com.matejdro.pebblenotificationcenter.lists.NotificationListAdapter;
import com.matejdro.pebblenotificationcenter.lists.actions.ActionList;
import com.matejdro.pebblenotificationcenter.lists.actions.NotificationActionList;
import com.matejdro.pebblenotificationcenter.location.LocationLookup;
import com.matejdro.pebblenotificationcenter.notifications.JellybeanNotificationListener;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import com.matejdro.pebblenotificationcenter.pebble.DeliveryListener;
import com.matejdro.pebblenotificationcenter.pebble.PebbleCommunication;
import com.matejdro.pebblenotificationcenter.pebble.PebbleDeveloperConnection;
import com.matejdro.pebblenotificationcenter.pebble.PebblePacket;
import com.matejdro.pebblenotificationcenter.pebble.WatchappHandler;
import com.matejdro.pebblenotificationcenter.pebble.modules.CommModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;
import com.matejdro.pebblenotificationcenter.util.PreferencesUtil;
import com.matejdro.pebblenotificationcenter.util.TextUtil;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import org.json.JSONException;
import timber.log.Timber;

public class PebbleTalkerService extends Service
{
    private SharedPreferences settings;
    private DefaultAppSettingsStorage defaultSettingsStorage;
    private NotificationHistoryStorage historyDb;

    private PebbleDeveloperConnection devConn;
    private UUID previousUUID;

    private NotificationListAdapter listHandler;

    private Queue<Integer> notificationRemovalQueue = new LinkedList<Integer>();

    ProcessedNotification curSendingNotification;
    private Queue<ProcessedNotification> sendingQueue = new LinkedList<ProcessedNotification>();
    private SparseArray<ProcessedNotification> sentNotifications = new SparseArray<ProcessedNotification>();
    private HashMap<String, Long> lastAppVibration = new HashMap<String, Long>();
    private HashMap<String, Long> lastAppNotification = new HashMap<String, Long>();

    public PebbleCommunication pebbleCommunication;

    private LocationLookup locationLookup;
    private int closingAttempts = 0;

    public SparseArray<CommModule> modules = new SparseArray<CommModule>();
    public HashMap<String, CommModule> registeredIntents = new HashMap<String, CommModule>();

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        if (devConn != null)
        {
            devConn.close();
        }
        historyDb.close();
        locationLookup.close();
    }

    @Override
    public void onCreate()
    {
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        defaultSettingsStorage = new DefaultAppSettingsStorage(settings, settings.edit());
        historyDb = new NotificationHistoryStorage(this);

        try
        {
            devConn = new PebbleDeveloperConnection();
            devConn.connectBlocking();
        } catch (InterruptedException e)
        {
        } catch (URISyntaxException e)
        {
        }

        locationLookup = new LocationLookup(this.getApplicationContext());
        locationLookup.lookup();
        pebbleCommunication = new PebbleCommunication(this);

        addModule(new NotificationSendingModule(this), 1);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (devConn == null || devConn.isClosed())
        {
            try
            {
                devConn = new PebbleDeveloperConnection();
                devConn.connectBlocking();
            } catch (InterruptedException e)
            {
            } catch (URISyntaxException e)
            {
            }
        }

       if (intent.getAction() != null)
       {
           CommModule receivingModule = registeredIntents.get(intent.getAction());
           if (receivingModule != null)
               receivingModule.gotIntent(intent);
       }

        return super.onStartCommand(intent, flags, startId);
    }

    private void addModule(CommModule module, int id)
    {
        modules.put(id, module);
    }

    public CommModule getModule(int id)
    {
        return modules.get(id);
    }

    public void registerIntent(String action, CommModule module)
    {
        registeredIntents.put(action, module);
    }

    public SparseArray getAllModules()
    {
        return modules;
    }

    public void dismissOnPebble(Integer id, boolean dontClose)
    {
        Timber.d("Dismissing upwards...");

        ProcessedNotification notification = sentNotifications.get(id);
        if (notification != null && !notification.sent)
            return;

        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 4);
        data.addInt32(1, id);
        if (dontClose || !sendingQueue.isEmpty())
            data.addUint8(2, (byte) 1);

        pebbleCommunication.sendToPebble(data);
    }

    public void processDismissUpwards(NotificationKey key, boolean dontClose)
    {
        Timber.d("got dismiss: " + key);

        if (key.getAndroidId() == null)
            return;

        AppSettingStorage settingsStorage;
        if (key.getPackage() == null)
            settingsStorage = defaultSettingsStorage;
        else
            settingsStorage = new SharedPreferencesAppStorage(this, key.getPackage(), defaultSettingsStorage);

        boolean syncDismissUp = settingsStorage.getBoolean(AppSetting.DISMISS_UPRWADS);
        if (!syncDismissUp)
            return;

        for (int i = 0; i < sentNotifications.size(); i++)
        {
            ProcessedNotification notification = sentNotifications.valueAt(i);

            if (!notification.source.isListNotification() && notification.source.isSameNotification(key))
            {
                dismissUpwards(notification, dontClose);

                sentNotifications.removeAt(i);
                i--;
            }
        }

        //Remove now dismissed notification from queue so it does not spam Pebble later
        Iterator<ProcessedNotification> iterator = sendingQueue.iterator();
        while (iterator.hasNext())
        {
            ProcessedNotification notification = iterator.next();

            if (!notification.source.isListNotification() && notification.source.isSameNotification(key))
            {
                iterator.remove();
            }
        }
    }

    public void processDismissUpwardsWholePackage(String pkg, boolean dontClose)
    {
        Timber.d("got package dismiss: " + pkg );

        AppSettingStorage settingsStorage;
        if (pkg == null)
            settingsStorage = defaultSettingsStorage;
        else
            settingsStorage = new SharedPreferencesAppStorage(this, pkg, defaultSettingsStorage);

        boolean syncDismissUp = settingsStorage.getBoolean(AppSetting.DISMISS_UPRWADS);
        if (!syncDismissUp)
            return;

        for (int i = 0; i < sentNotifications.size(); i++)
        {
            ProcessedNotification notification = sentNotifications.valueAt(i);

            if (!notification.source.isListNotification() && notification.source.getKey().getPackage().equals(pkg))
            {
                dismissUpwards(notification, dontClose);

                sentNotifications.removeAt(i);
                i--;

            }
        }

        //Remove now dismissed notification from queue so it does not spam Pebble later
        Iterator<ProcessedNotification> iterator = sendingQueue.iterator();
        while (iterator.hasNext())
        {
            ProcessedNotification notification = iterator.next();

            if (!notification.source.isListNotification() && notification.source.getKey().getPackage().equals(pkg))
            {
                iterator.remove();
            }
        }
    }

    public void dismissUpwards(ProcessedNotification notification, boolean dontClose)
    {
        //Dismiss from Pebble
//        if (commBusy)
//            notificationRemovalQueue.add(notification.id);
//        else
//            dismissOnPebble(notification.id, dontClose);

        //Also dismiss related group messages from this notification (some apps have trouble with dismissing to side channel directly)
        if (notification.source.getWearGroupType() == PebbleNotification.WEAR_GROUP_TYPE_GROUP_SUMMARY)
        {
            for (int i = 0; i < sentNotifications.size(); i++)
            {
                ProcessedNotification compare = sentNotifications.valueAt(i);

                if (notification.source.isInSameGroup(compare.source) && compare.source.getWearGroupType() == PebbleNotification.WEAR_GROUP_TYPE_GROUP_MESSAGE)
                {
                    processDismissUpwards(compare.source.getKey(), dontClose);
                    i = -1; //processDismissUpwards will ususally remove some entries so we should start from beginning
                }
            }
        }

    }



    private void dismissOnPebbleSucceeded(PebbleDictionary data)
    {
        Timber.d("	dismiss success: " + data.contains(2) + " " + notificationRemovalQueue.size());

        if (data.contains(2))
        {
            closeApp();
            return;
        }
    }

    private void updateCurrentlyRunningApp()
    {
//        UUID currentApp = devConn.getCurrentRunningApp();
//
//        if (currentApp != null && !(currentApp.getLeastSignificantBits() == 0 && currentApp.getMostSignificantBits() == 0) && (!currentApp.equals(DataReceiver.pebbleAppUUID) || previousUUID == null) && !currentApp.equals(UNKNOWN_UUID))
//        {
//            previousUUID = currentApp;
//        }
    }

    private void openApp()
    {
        PebbleKit.startAppOnPebble(this, DataReceiver.pebbleAppUUID);
    }

    private void closeApp()
    {
        Timber.d("CloseApp " + previousUUID + " " + closingAttempts);

        //startAppOnPebble seems to fail sometimes so I fallback to regular closing if it fails 2 times.
//        if (settings.getBoolean(PebbleNotificationCenter.CLOSE_TO_LAST_APP, false) && previousUUID != null && !previousUUID.equals(DataReceiver.pebbleAppUUID) && !previousUUID.equals(MAIN_MENU_UUID) && closingAttempts < 3)
//            PebbleKit.startAppOnPebble(this, previousUUID);
//        else
//            PebbleKit.closeAppOnPebble(this, DataReceiver.pebbleAppUUID);


        Editor editor = settings.edit();
        editor.putLong("lastClose", System.currentTimeMillis());
        editor.apply();

        closingAttempts++;
    }

    private void appOpened()
    {
        sendConfig(sendingQueue.size() > 0 || curSendingNotification != null);
    }

    private void menuPicked(PebbleDictionary data)
    {
        int index = data.getUnsignedIntegerAsLong(1).intValue();
        if (index == 1 || !NotificationHandler.isNotificationListenerSupported())
        {
            listHandler = new NotificationHistoryAdapter(this, historyDb);
            listHandler.sendNotification(0);
        } else
        {
            listHandler = new ActiveNotificationsAdapter(this);
            listHandler.sendNotification(0);
        }

    }

//    private void actionsTextRequested(PebbleDictionary data)
//    {
//        int id = data.getInteger(1).intValue();
//
//        ProcessedNotification notification = sentNotifications.get(id);
//        if (notification == null)
//        {
//            Timber.d("Unknown ID!");
//
//            dismissOnPebble(id, false);
//            notificationTransferCompleted(true);
//            return;
//        }
//
//        data = new PebbleDictionary();
//
//        data.addUint8(0, (byte) 5);
//        data.addInt32(1, id);
//
//        List<NotificationAction> actions = notification.source.getActions();
//        int size = Math.min(actions.size(), 5);
//
//        byte[] textData = new byte[size * 19];
//
//        for (int i = 0; i < size; i++)
//        {
//            String text = TextUtil.prepareString(actions.get(i).getActionText(), 18);
//            System.arraycopy(text.getBytes(), 0, textData, i * 19, text.length());
//            textData[19 * (i + 1) -1 ] = 0;
//        }
//
//        data.addBytes(3, textData);
//
//        pebbleCommunication.sendToPebble(data);
//    }
//
//
//    private void notificationTransferCompleted(boolean sendNext)
//    {
//        Timber.d("Transfer completed...");
//
//        if (curSendingNotification != null)
//        {
//            if (curSendingNotification.vibrated)
//                lastAppVibration.put(curSendingNotification.source.getKey().getPackage(), System.currentTimeMillis());
//
//            lastAppNotification.put(curSendingNotification.source.getKey().getPackage(), System.currentTimeMillis());
//        }
//
//        curSendingNotification = null;
//
//        Timber.d("csn null: " + (curSendingNotification == null));
//        Timber.d("queue size: " + sendingQueue.size());
//
//        if (sendNext)
//            commWentIdle();
//        else
//            commBusy = false;
//
//    }
//
//    private void pebbleSelectPressed(PebbleDictionary data)
//    {
//        int id = data.getInteger(1).intValue();
//        boolean hold = data.contains(2);
//
//        Timber.d("Select button pressed on Pebble, Hold: " + hold);
//
//        ProcessedNotification notification = sentNotifications.get(id);
//        if (notification == null)
//            return;
//
//        if (notification == curSendingNotification)
//            notificationTransferCompleted(false);
//
//
//        if (notification.source.getActions() == null || notification.source.getActions().size() == 0)
//        {
//            dismissOnPhone(notification);
//            return;
//        }
//
//        AppSetting relevantSetting = hold ? AppSetting.SELECT_HOLD_ACTION : AppSetting.SELECT_PRESS_ACTION;
//        AppSettingStorage settingStorage = notification.source.getSettingStorage(this);
//
//        int action = settingStorage.getInt(relevantSetting);
//
//        if (notification.source.shouldForceActionMenu() || action == 2)
//        {
//            notification.activeActionList = new NotificationActionList(notification);
//            notification.activeActionList.showList(this, notification);
//        }
//        else if (action == 0)
//        {
//            return;
//        }
//        else if (action == 1)
//        {
//            dismissOnPhone(notification);
//        }
//        else
//        {
//            action -= 3;
//            if (notification.source.getActions().size() <= action)
//                return;
//
//            notification.source.getActions().get(action).executeAction(this, notification);
//        }
//    }
//
//    private void pebbleDismissRequested(PebbleDictionary data)
//    {
//        int id = data.getInteger(1).intValue();
//
//        Timber.d("Dismiss requested from Pebble");
//
//        ProcessedNotification notification = sentNotifications.get(id);
//        if (notification == null)
//            return;
//
//        dismissOnPhone(notification);
//    }
//
//    public void dismissOnPhone(ProcessedNotification notification)
//    {
//        dismissOnPebble(notification.id, false);
//
//        //Group messages can't be dismissed (they are not even displayed), so I should find relevat message in actual notification tray
//        if (notification.source.getWearGroupType() == PebbleNotification.WEAR_GROUP_TYPE_GROUP_MESSAGE)
//        {
//            for (int i = 0; i < sentNotifications.size(); i++)
//            {
//                ProcessedNotification compare = sentNotifications.valueAt(i);
//
//                if (notification.source.isInSameGroup(compare.source) && compare.source.getWearGroupType() == PebbleNotification.WEAR_GROUP_TYPE_GROUP_SUMMARY)
//                {
//                    notification = compare;
//                    break;
//                }
//            }
//        }
//
//        if (!notification.source.isDismissable())
//            return;
//
//        JellybeanNotificationListener.dismissNotification(notification.source.getKey());
//
//    }
//
//    private void actionListPacket(int packetId, PebbleDictionary data)
//    {
//        int notificationId = data.getInteger(1).intValue();
//
//        ProcessedNotification notification = sentNotifications.get(notificationId);
//        if (notification != null)
//        {
//            ActionList list = notification.activeActionList;
//            if (list != null)
//            {
//                list.handlePacket(this, packetId, data);
//            }
//        }
//    }

    private void sendConfig(boolean notificationWaiting)
    {
        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 3);

        byte[] configBytes = new byte[13];

        int timeout = 0;
        try
        {
            timeout = Math.min(30000, Integer.parseInt(settings.getString("watchappTimeout", "0")));
        } catch (NumberFormatException e)
        {
        }

        int vibratingTimeout = 0;
        try
        {
            vibratingTimeout = Math.min(30000, Integer.parseInt(settings.getString("periodicVibrationTimeout", "0")));
        } catch (NumberFormatException e)
        {
        }

        boolean backlight = false;
        int backlightSetting = Integer.parseInt(settings.getString(PebbleNotificationCenter.LIGHT_SCREEN_ON_NOTIFICATIONS, "2"));
        switch (backlightSetting)
        {
            case 1:
                break;
            case 2:
                backlight = true;
                break;
            case 3:
                locationLookup.lookup();
                backlight = SunriseSunsetCalculator.isSunDown(settings);
                break;
        }

        configBytes[0] = (byte) Integer.parseInt(settings.getString(PebbleNotificationCenter.FONT_TITLE, "6"));
        configBytes[1] = (byte) Integer.parseInt(settings.getString(PebbleNotificationCenter.FONT_SUBTITLE, "5"));
        configBytes[2] = (byte) Integer.parseInt(settings.getString(PebbleNotificationCenter.FONT_BODY, "4"));
        configBytes[3] = (byte) (timeout >>> 0x08);
        configBytes[4] = (byte) timeout;

        byte flags = 0;
        flags |= (byte) (settings.getBoolean(PebbleNotificationCenter.CLOSE_TO_LAST_APP, false) ? 0x02 : 0);
        flags |= (byte) (NotificationHandler.isNotificationListenerSupported() ? 0x04 : 0);
        flags |= (byte) (notificationWaiting ? 0x08 : 0);
        flags |= (byte) (backlight ? 0x10 : 0);
        flags |= (byte) (settings.getBoolean(PebbleNotificationCenter.DONT_VIBRATE_WHEN_CHARGING, true) ? 0x20 : 0);
        flags |= (byte) (settings.getBoolean(PebbleNotificationCenter.INVERT_COLORS, false) ? 0x40 : 0);
        flags |= (byte) (settings.getBoolean(PebbleNotificationCenter.NOTIFICATIONS_DISABLED, false) ? 0x80 : 0);
        flags |= (byte) (settings.getBoolean(PebbleNotificationCenter.VIBRATION_DISABLED, false) ? 0x01 : 0);


        configBytes[7] = flags;

        configBytes[8] = (byte) (WatchappHandler.SUPPORTED_PROTOCOL >>> 0x08);
        configBytes[9] = (byte) WatchappHandler.SUPPORTED_PROTOCOL;
        configBytes[10] = (byte) Integer.parseInt(settings.getString(PebbleNotificationCenter.SHAKE_ACTION, "1"));
        configBytes[11] = (byte) (vibratingTimeout >>> 0x08);
        configBytes[12] = (byte) vibratingTimeout;

        data.addBytes(1, configBytes);

        Timber.d("Sending config...");

        pebbleCommunication.sendToPebble(data);
    }

    private void receivedConfigChange(PebbleDictionary data)
    {
        Editor editor = settings.edit();

        int id = data.getUnsignedIntegerAsLong(1).intValue();
        switch (id)
        {
            case 0: //Notifications disabled
                boolean value = data.getUnsignedIntegerAsLong(2) != 0;
                editor.putBoolean(PebbleNotificationCenter.NOTIFICATIONS_DISABLED, value);
                break;
            case 1: //Vibration disabled
                value = data.getUnsignedIntegerAsLong(2) != 0;
                editor.putBoolean(PebbleNotificationCenter.VIBRATION_DISABLED, value);
                break;
        }

        editor.apply();
    }

    private void pebbleReconnected()
    {
        if (sendingQueue.size() > 0 || curSendingNotification != null)
            openApp();
    }

	private void receivedPacketFromPebble(String jsonPacket)
	{
        PebbleDictionary data = null;
        try
        {
            data = PebbleDictionary.fromJson(jsonPacket);
        } catch (JSONException e)
        {
            Crashlytics.logException(e);
            e.printStackTrace();
            return;
        }

        int module = data.getUnsignedIntegerAsLong(0).intValue();

        Timber.d("Pebble packet " + module);
	}
}
