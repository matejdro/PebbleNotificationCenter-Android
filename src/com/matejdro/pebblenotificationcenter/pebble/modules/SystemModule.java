package com.matejdro.pebblenotificationcenter.pebble.modules;

import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.ContactsContract;
import android.util.SparseArray;
import android.widget.Toast;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.matejdro.pebblenotificationcenter.DataReceiver;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.pebble.PebbleCommunication;
import com.matejdro.pebblenotificationcenter.pebble.WatchappHandler;
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

    private long lastRunningAppUpdate = 0;

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

        byte[] configBytes = new byte[13];

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
                getService().getLocationLookup().lookup();
                backlight = SunriseSunsetCalculator.isSunDown(getService().getGlobalSettings());
                break;
        }

        configBytes[0] = (byte) Integer.parseInt(getService().getGlobalSettings().getString(PebbleNotificationCenter.FONT_TITLE, "6"));
        configBytes[1] = (byte) Integer.parseInt(getService().getGlobalSettings().getString(PebbleNotificationCenter.FONT_SUBTITLE, "5"));
        configBytes[2] = (byte) Integer.parseInt(getService().getGlobalSettings().getString(PebbleNotificationCenter.FONT_BODY, "4"));
        configBytes[3] = (byte) (timeout >>> 0x08);
        configBytes[4] = (byte) timeout;

        byte flags = 0;
        flags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.CLOSE_TO_LAST_APP, false) ? 0x02 : 0);
        flags |= (byte) (NotificationHandler.isNotificationListenerSupported() ? 0x04 : 0);
        flags |= (byte) (NotificationSendingModule.get(getService()).isAnyNotificationWaiting() ? 0x08 : 0);
        flags |= (byte) (backlight ? 0x10 : 0);
        flags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.DONT_VIBRATE_WHEN_CHARGING, true) ? 0x20 : 0);
        flags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.INVERT_COLORS, false) ? 0x40 : 0);
        flags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.NOTIFICATIONS_DISABLED, false) ? 0x80 : 0);
        flags |= (byte) (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.VIBRATION_DISABLED, false) ? 0x01 : 0);

        configBytes[7] = flags;

        configBytes[8] = (byte) (WatchappHandler.SUPPORTED_PROTOCOL >>> 0x08);
        configBytes[9] = (byte) WatchappHandler.SUPPORTED_PROTOCOL;
        configBytes[10] = (byte) Integer.parseInt(getService().getGlobalSettings().getString(PebbleNotificationCenter.SHAKE_ACTION, "1"));
        configBytes[11] = (byte) (vibratingTimeout >>> 0x08);
        configBytes[12] = (byte) vibratingTimeout;

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
    }

    private void gotMessagePebbleOpened(PebbleDictionary message)
    {
        closeTries = 0;

        int version = 0;
        if (message.contains(2))
            version = message.getUnsignedIntegerAsLong(2).intValue();

        Timber.d("Version " + version);

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

        SparseArray<CommModule> modules = getService().modules;
        for (int i = 0 ; i < modules.size(); i++)
            modules.valueAt(i).pebbleAppOpened();

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
        if (intent.getAction().equals(INTENT_PEBBLE_CONNECTED) && NotificationSendingModule.get(getService()).isAnyNotificationWaiting())
        {
            openApp();
        }
    }

    @Override
    public void gotMessageFromPebble(PebbleDictionary message)
    {
        int id = 0;
        if (message.contains(1)) //Open message from older Pebble app does not have entry at 1.
            id = message.getUnsignedIntegerAsLong(1).intValue();

        Timber.d("system packet " + id);

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

        if (newApp != null && (!newApp.equals(DataReceiver.pebbleAppUUID) || currentRunningApp == null) && !newApp.equals(UNKNOWN_UUID))
        {
            currentRunningApp = newApp;
        }

        lastRunningAppUpdate = System.currentTimeMillis();
    }

    public UUID getCurrentRunningApp()
    {
        return currentRunningApp;
    }

    public void openApp()
    {
        if (System.currentTimeMillis() - lastRunningAppUpdate > 5000)
            updateCurrentlyRunningApp();

        PebbleKit.startAppOnPebble(getService(), DataReceiver.pebbleAppUUID);
    }

    public void closeApp()
    {
        Timber.d("CloseApp " + currentRunningApp);

        if (getService().getGlobalSettings().getBoolean(PebbleNotificationCenter.CLOSE_TO_LAST_APP, false) && canCloseToApp(currentRunningApp) && closeTries < 2)
            PebbleKit.startAppOnPebble(getService(), currentRunningApp);
        else
            PebbleKit.closeAppOnPebble(getService(), DataReceiver.pebbleAppUUID);

        SharedPreferences.Editor editor = getService().getGlobalSettings().edit();
        editor.putLong("lastClose", System.currentTimeMillis());
        editor.apply();

        closeTries++;
    }

    private static boolean canCloseToApp(UUID uuid)
    {
        return uuid != null && !uuid.equals(DataReceiver.pebbleAppUUID) && !uuid.equals(MAIN_MENU_UUID) && !uuid.equals(UNKNOWN_UUID);
    }

    public static SystemModule get(PebbleTalkerService service)
    {
        return (SystemModule) service.getModule(MODULE_SYSTEM);
    }
}
