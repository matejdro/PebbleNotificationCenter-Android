package com.matejdro.pebblenotificationcenter.pebble.modules;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.SparseArray;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.matejdro.pebblecommons.pebble.CommModule;
import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblecommons.pebble.PebbleVibrationPattern;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.pebble.WatchappHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import timber.log.Timber;

/**
 * Created by Matej on 29.11.2014.
 */
public class SystemModule extends CommModule
{
    public static int MODULE_SYSTEM = 0;

    public static final UUID UNKNOWN_UUID = new UUID(0, 0);
    public static final UUID MAIN_MENU_UUID = UUID.fromString("dec0424c-0625-4878-b1f2-147e57e83688");

    public static final String INTENT_PEBBLE_CONNECTED = "PebbleConnected";

    private Callable<Boolean> runOnNext;
    private UUID currentRunningApp;

    private int closeTries = 0;

    public SystemModule(PebbleTalkerService service)
    {
        super(service);
        service.registerIntent(INTENT_PEBBLE_CONNECTED, this);

        runOnNext = null;
    }

    @Override
    public boolean sendNextMessage()
    {
        if (runOnNext == null)
            return false;

        Callable<Boolean> oldRunOnNext = runOnNext;

        try
        {
            boolean ret = runOnNext.call();
            if (runOnNext == oldRunOnNext)
                runOnNext = null;

            return ret;
        } catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private void sendConfig()
    {
        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 0);
        data.addUint8(1, (byte) 0);

        List<Byte> vibrationPattern = PebbleVibrationPattern.parseVibrationPattern(getService().getGlobalSettings().getString(PebbleNotificationCenter.PERIODIC_VIBRATION_PATTERN, "100"));

        byte[] configBytes = new byte[15 + vibrationPattern.size()];

        int timeout = 0;
        try
        {
            timeout = Math.min(30000, Integer.parseInt(getService().getGlobalSettings().getString("watchappTimeout", "0")));
        } catch (NumberFormatException e)
        {
        }

        int vibratingTimeout = 0;
        try
        {
            vibratingTimeout = Math.min(30000, Integer.parseInt(getService().getGlobalSettings().getString("periodicVibrationTimeout", "0")));
        } catch (NumberFormatException e)
        {
        }

        boolean backlight = false;
        int backlightSetting = Integer.parseInt(getService().getGlobalSettings().getString(PebbleNotificationCenter.LIGHT_SCREEN_ON_NOTIFICATIONS, "2"));
        switch (backlightSetting)
        {
            case 1:
                break;
            case 2:
                backlight = true;
                break;
            case 3:
                NCTalkerService.fromPebbleTalkerService(getService()).getLocationLookup().lookup();
                backlight = SunriseSunsetCalculator.isSunDown(getService().getGlobalSettings());
                break;
        }

        int lightTimeout = 4;
        try
        {
            lightTimeout = Math.min(100, Integer.parseInt(getService().getGlobalSettings().getString("lightTimeout", "4")));
        }
        catch (NumberFormatException e)
        {

        }

        configBytes[3] = (byte) (timeout >>> 0x08);
        configBytes[4] = (byte) timeout;
        configBytes[5] = (byte) lightTimeout;

        byte flags = 0;
        flags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.CLOSE_TO_LAST_APP, false) ? 0x02 : 0);
        flags |= (byte) (NotificationHandler.isNotificationListenerSupported() ? 0x04 : 0);
        flags |= (byte) (NotificationSendingModule.get(getService()).isAnyNotificationWaiting() ? 0x08 : 0);
        flags |= (byte) (backlight ? 0x10 : 0);
        flags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.DONT_VIBRATE_WHEN_CHARGING, true) ? 0x20 : 0);
        flags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.NOTIFICATIONS_DISABLED, false) ? 0x80 : 0);
        flags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.WHITE_NOTIFICATION_TEXT, false) ? 0x40 : 0);
        flags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.VIBRATION_DISABLED, false) ? 0x01 : 0);

        configBytes[7] = flags;

        configBytes[8] = (byte) (WatchappHandler.SUPPORTED_PROTOCOL >>> 0x08);
        configBytes[9] = (byte) WatchappHandler.SUPPORTED_PROTOCOL;
        configBytes[11] = (byte) (vibratingTimeout >>> 0x08);
        configBytes[12] = (byte) vibratingTimeout;

        byte secondFlags = 0;
        secondFlags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.DISPLAY_SCROLL_SHADOW, true) ? 0x01 : 0);
        secondFlags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.SCROLL_BY_PAGE, false) ? 0x02 : 0);
        secondFlags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.DISPLAY_DISCONNECTED_NOTIFICATION, true) ? 0x04 : 0);
        secondFlags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.ENABLE_GESTURES, false) ? 0x08 : 0);
        configBytes[13] = secondFlags;

        configBytes[14] = (byte) vibrationPattern.size();
        for (int i = 0; i < vibrationPattern.size(); i++)
            configBytes[15 + i] = vibrationPattern.get(i);


        data.addBytes(2, configBytes);

        Timber.d("Sending config...");

        getService().getPebbleCommunication().sendToPebble(data);
    }

    private void sendConfigInvalidVersion(int version)
    {
        PebbleDictionary data = new PebbleDictionary();

        byte[] configBytes = new byte[13];
        configBytes[8] = (byte) (WatchappHandler.SUPPORTED_PROTOCOL >>> 0x08);
        configBytes[9] = (byte) WatchappHandler.SUPPORTED_PROTOCOL;

        if (version == 0) //Pre-2.4 protocol
        {
            data.addUint8(0, (byte) 3);
            data.addBytes(1, configBytes);

        }
        else
        {
            data.addUint8(0, (byte) 0);
            data.addUint8(1, (byte) 0);
            data.addBytes(2, configBytes);
        }

        Timber.d("Sending version mismatch config...");

        getService().getPebbleCommunication().sendToPebble(data);

        runOnNext = new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {
                //Pretend that I sent new message to prevent other modules sending potentially unsupported messages
                return true;
            }
        };

        if (version < WatchappHandler.SUPPORTED_PROTOCOL)
            WatchappHandler.showUpdateNotification(getService());
    }

    private void gotMessagePebbleOpened(PebbleDictionary message)
    {
        closeTries = 0;

        int version = 0;
        if (message.contains(2))
            version = message.getUnsignedIntegerAsLong(2).intValue();

        Timber.d("Version %d", version);

        final int finalVersion = version;

        if (version == WatchappHandler.SUPPORTED_PROTOCOL)
        {
            runOnNext = new Callable<Boolean>()
            {
                @Override
                public Boolean call() throws Exception
                {
                    sendConfig();
                    return true;
                }
            };

            int pebbleCapabilities = message.getUnsignedIntegerAsLong(3).intValue();
            getService().getPebbleCommunication().setConnectedWatchCapabilities(pebbleCapabilities);

            SparseArray<CommModule> modules = getService().getAllModules();
            for (int i = 0 ; i < modules.size(); i++)
                modules.valueAt(i).pebbleAppOpened();
        }
        else
        {
            runOnNext = new Callable<Boolean>()
            {
                @Override
                public Boolean call()
                {
                    sendConfigInvalidVersion(finalVersion);
                    return true;
                }
            };
        }


        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.resetBusy();
        communication.sendNext();
    }

    public void hideHourglass()
    {
        runOnNext = new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {
                PebbleDictionary data = new PebbleDictionary();

                data.addUint8(0, (byte) 0);
                data.addUint8(1, (byte) 1);

                Timber.d("Sending hide hourglass...");

                getService().getPebbleCommunication().sendToPebble(data);


                return true;
            }
        };

        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.sendNext();
    }

    private void gotMessageConfigChange(PebbleDictionary message)
    {
        SharedPreferences.Editor editor = getService().getGlobalSettings().edit();

        int id = message.getUnsignedIntegerAsLong(2).intValue();
        switch (id)
        {
            case 0: //Notifications disabled
                boolean value = message.getUnsignedIntegerAsLong(3) != 0;
                editor.putBoolean(PebbleNotificationCenter.NOTIFICATIONS_DISABLED, value);
                break;
            case 1: //Vibration disabled
                value = message.getUnsignedIntegerAsLong(3) != 0;
                editor.putBoolean(PebbleNotificationCenter.VIBRATION_DISABLED, value);
                break;
            case 2: //Clear history
                NCTalkerService.fromPebbleTalkerService(getService()).getHistoryDatabase().clearDatabase();
                break;
        }

        editor.apply();
    }

    private void gotMessageMenuItem(PebbleDictionary message)
    {
        int id = message.getUnsignedIntegerAsLong(2).intValue();
        ListModule.get(getService()).showList(id);
    }

    @Override
    public void gotIntent(Intent intent)
    {
        if (intent.getAction().equals(INTENT_PEBBLE_CONNECTED))
        {
            PebbleCommunication communication = getService().getPebbleCommunication();
            communication.resetBusy();
            communication.sendNext();

            if (    NotificationSendingModule.get(getService()).isAnyNotificationWaiting() &&
                    getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.OPEN_NC_AFTER_RECONNECT, false))
            {
                Timber.d("Opening App after reconnect");

                openApp();
            }
        }

    }

    @Override
    public void gotMessageFromPebble(PebbleDictionary message)
    {
        int id = 0;
        if (message.contains(1)) //Open message from older Pebble app does not have entry at 1.
            id = message.getUnsignedIntegerAsLong(1).intValue();

        Timber.d("system packet %d", id);

        switch (id)
        {
            case 0: //Pebble opened
                gotMessagePebbleOpened(message);
                break;
            case 1: //Menu entry picked
                gotMessageMenuItem(message);
                break;
            case 2: //Config change
                gotMessageConfigChange(message);
                break;
            case 3: //Close me
                closeApp();
                break;


        }
    }

    public void updateCurrentlyRunningApp()
    {
        UUID newApp = getService().getDeveloperConnection().getCurrentRunningApp();

        if (newApp != null && (!newApp.equals(PebbleNotificationCenter.WATCHAPP_UUID) || currentRunningApp == null) && !newApp.equals(UNKNOWN_UUID))
        {
            currentRunningApp = newApp;
        }
    }

    public UUID getCurrentRunningApp()
    {
        return currentRunningApp;
    }

    public void openApp()
    {
        PebbleKit.startAppOnPebble(getService(), PebbleNotificationCenter.WATCHAPP_UUID);
    }

    public void closeApp()
    {
        Timber.d("CloseApp %s", currentRunningApp);

        NotificationSendingModule notificationSendingModule = NotificationSendingModule.get(getService());
        if (notificationSendingModule.isAnyNotificationWaiting())
            return;

        if (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.CLOSE_TO_LAST_APP, false) && canCloseToApp(currentRunningApp) && closeTries < 2)
            PebbleKit.startAppOnPebble(getService(), currentRunningApp);
        else
            PebbleKit.closeAppOnPebble(getService(), PebbleNotificationCenter.WATCHAPP_UUID);

        SharedPreferences.Editor editor = getService().getGlobalSettings().edit();
        editor.putLong("lastClose", System.currentTimeMillis());
        editor.apply();

        closeTries++;
    }

    private static boolean canCloseToApp(UUID uuid)
    {
        return uuid != null && !uuid.equals(PebbleNotificationCenter.WATCHAPP_UUID) && !uuid.equals(MAIN_MENU_UUID) && !uuid.equals(UNKNOWN_UUID);
    }

    public static SystemModule get(PebbleTalkerService service)
    {
        return (SystemModule) service.getModule(MODULE_SYSTEM);
    }
}
