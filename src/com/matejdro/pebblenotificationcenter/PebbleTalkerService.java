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
import com.matejdro.pebblenotificationcenter.pebble.PebbleDeveloperConnection;
import com.matejdro.pebblenotificationcenter.pebble.WatchappHandler;
import com.matejdro.pebblenotificationcenter.util.PreferencesUtil;
import com.matejdro.pebblenotificationcenter.util.TextUtil;
import java.net.URISyntaxException;
import java.util.Calendar;
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
    //This used to be UUID for all system apps, but now they all get their separate UUID it seems
    public static final UUID UNKNOWN_UUID = UUID.fromString("0a7575eb-e5b9-456b-8701-3eacb62d74f1");
    public static final UUID MAIN_MENU_UUID = UUID.fromString("dec0424c-0625-4878-b1f2-147e57e83688");

    public static final int TEXT_LIMIT = 900;

    private SharedPreferences settings;
    private DefaultAppSettingsStorage defaultSettingsStorage;
    private NotificationHistoryStorage historyDb;
    private Handler handler;

    private PebbleDeveloperConnection devConn;
    private UUID previousUUID;

    private NotificationListAdapter listHandler;

    private boolean commBusy = false;
    private Queue<Integer> notificationRemovalQueue = new LinkedList<Integer>();

    ProcessedNotification curSendingNotification;
    private Queue<ProcessedNotification> sendingQueue = new LinkedList<ProcessedNotification>();
    private SparseArray<ProcessedNotification> sentNotifications = new SparseArray<ProcessedNotification>();

    private LocationLookup locationLookup;
    private int closingAttempts = 0;

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
        handler.removeCallbacksAndMessages(null);
        locationLookup.close();
    }

    @Override
    public void onCreate()
    {
        handler = new Handler();
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

        if (intent != null)
        {
            if (intent.hasExtra("notification"))
            {
                PebbleNotification notification = intent.getParcelableExtra("notification");
                processNotification(notification);
            } else if (intent.hasExtra("packet"))
            {
                String jsonPacket = intent.getStringExtra("packet");
                receivedPacketFromPebble(jsonPacket);
            }
            else if (intent.hasExtra("dismissUpwardsId"))
            {
                int id = intent.getIntExtra("dismissUpwardsId", 0);
                String pkg = intent.getStringExtra("pkg");
                String tag = intent.getStringExtra("tag");

                processDismissUpwards(id, pkg, tag, false);
            }
            else if (intent.hasExtra("dismissUpwardsPackage"))
            {
                String pkg = intent.getStringExtra("dismissUpwardsPackage");

                processDismissUpwards(pkg, false);
            }
            else if (intent.hasExtra("PebbleConnected"))
            {
                pebbleReconnected();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void send(ProcessedNotification notification)
    {
        Timber.d("Send " + notification.id + " " + notification.source.getTitle() + " " + notification.source.getSubtitle());

        curSendingNotification = notification;

        AppSettingStorage settingStorage = notification.source.getSettingStorage(this);

        int periodicVibrationInterval = 0;
        try
        {
            periodicVibrationInterval = Math.min(Integer.parseInt(settingStorage.getString(AppSetting.PERIODIC_VIBRATION)), 128);
        } catch (NumberFormatException e)
        {
        }

        PebbleDictionary data = new PebbleDictionary();
        List<Byte> vibrationPattern = AppSetting.parseVibrationPattern(settingStorage);

        byte[] configBytes = new byte[6 + vibrationPattern.size()];

        byte flags = 0;
        flags |= (byte) (notification.source.isDismissable() ? 0x01 : 0);
        flags |= (byte) (notification.source.isListNotification() ? 0x2 : 0);
        flags |= (byte) ((settingStorage.getBoolean(AppSetting.SWITCH_TO_MOST_RECENT_NOTIFICATION) || notification.source.shouldNCForceSwitchToThisNotification()) ? 0x4 : 0);
        flags |= (byte) (notification.source.shouldScrollToEnd() ? 0x8 : 0);

        configBytes[0] = Byte.parseByte(settings.getString("textSize", "0"));
        configBytes[1] = flags;
        configBytes[2] = (byte) periodicVibrationInterval;

        configBytes[3] = (byte) vibrationPattern.size();
        for (int i = 0; i < vibrationPattern.size(); i++)
            configBytes[4 + i] = vibrationPattern.get(i);

        int timeout = 0;
        try
        {
            timeout = Math.min(30000, Integer.parseInt(settings.getString("watchappTimeout", "0")));
        } catch (NumberFormatException e)
        {
        }

        data.addUint8(0, (byte) 0);
        data.addInt32(1, notification.id);
        data.addBytes(2, configBytes);
        data.addUint16(3, (short) timeout);
        data.addUint8(4, (byte) notification.textChunks.size());
        data.addString(5, notification.source.getTitle());
        data.addString(6, notification.source.getSubtitle());

        PebbleKit.sendDataToPebble(this, DataReceiver.pebbleAppUUID, data);
        commStarted();
    }

    public void dismissOnPebble(Integer id, boolean dontClose)
    {
        Timber.d("Dismissing upwards...");

        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 4);
        data.addInt32(1, id);
        if (dontClose)
            data.addUint8(2, (byte) 1);

        PebbleKit.sendDataToPebble(this, DataReceiver.pebbleAppUUID, data);
        commStarted();
    }

    public void processDismissUpwards(Integer androidId, String pkg, String tag, boolean dontClose)
    {
        Timber.d("got dismiss: " + pkg + " " + androidId + " " + tag);

        if (androidId == null)
            return;

        AppSettingStorage settingsStorage;
        if (pkg == null)
            settingsStorage = defaultSettingsStorage;
        else
            settingsStorage = new SharedPreferencesAppStorage(this, pkg, defaultSettingsStorage, true);

        boolean syncDismissUp = settingsStorage.getBoolean(AppSetting.DISMISS_UPRWADS);
        if (!syncDismissUp)
            return;

        for (int i = 0; i < sentNotifications.size(); i++)
        {
            ProcessedNotification notification = sentNotifications.valueAt(i);

            if (!notification.source.isListNotification() && notification.source.isSameNotification(androidId, pkg, tag))
            {
                Timber.tag("NC Upwards debug");
                Timber.d("	rem notifications check: %b %d", commBusy, sendingQueue.size());
                if (commBusy)
                {
                    notificationRemovalQueue.add(notification.id);
                    continue;
                }

                dismissOnPebble(notification.id, dontClose);
            }
        }

        //Remove now dismissed notification from queue so it does not spam Pebble later
        Iterator<ProcessedNotification> iterator = sendingQueue.iterator();
        while (iterator.hasNext())
        {
            ProcessedNotification notification = iterator.next();

            if (!notification.source.isListNotification() && notification.source.isSameNotification(androidId, pkg, tag))
            {
                iterator.remove();
            }
        }
        for (int i = 0; i < sentNotifications.size(); i++)
        {
            ProcessedNotification notification = sentNotifications.valueAt(i);

            if (!notification.source.isListNotification() && notification.source.isSameNotification(androidId, pkg, tag))
            {
                sentNotifications.removeAt(i);
                i--;
            }
        }
    }

    public void processDismissUpwards(String pkg, boolean dontClose)
    {
        Timber.d("got package dismiss: " + pkg );

        AppSettingStorage settingsStorage;
        if (pkg == null)
            settingsStorage = defaultSettingsStorage;
        else
            settingsStorage = new SharedPreferencesAppStorage(this, pkg, defaultSettingsStorage, true);

        boolean syncDismissUp = settingsStorage.getBoolean(AppSetting.DISMISS_UPRWADS);
        if (!syncDismissUp)
            return;

        for (int i = 0; i < sentNotifications.size(); i++)
        {
            ProcessedNotification notification = sentNotifications.valueAt(i);

            if (!notification.source.isListNotification() && notification.source.getPackage().equals(pkg))
            {
                Timber.tag("NC Upwards debug");
                Timber.d("	rem notifications check: %b %d", commBusy, sendingQueue.size());
                if (commBusy)
                {
                    notificationRemovalQueue.add(notification.id);
                    continue;
                }

                dismissOnPebble(notification.id, dontClose);
            }
        }

        //Remove now dismissed notification from queue so it does not spam Pebble later
        Iterator<ProcessedNotification> iterator = sendingQueue.iterator();
        while (iterator.hasNext())
        {
            ProcessedNotification notification = iterator.next();

            if (!notification.source.isListNotification() && notification.source.getPackage().equals(pkg))
            {
                iterator.remove();
            }
        }
        for (int i = 0; i < sentNotifications.size(); i++)
        {
            ProcessedNotification notification = sentNotifications.valueAt(i);

            if (!notification.source.isListNotification() && notification.source.getPackage().equals(pkg))
            {
                sentNotifications.removeAt(i);
                i--;
            }
        }
    }


    private void dismissOnPebbleSucceeded(PebbleDictionary data)
    {
        Timber.tag("NC Upwards debug");
        Timber.d("	dismiss success: %b %d", data.contains(2), notificationRemovalQueue.size());

        if (data.contains(2))
        {
            closeApp();
            return;
        }

        commWentIdle();
    }

    public void processNotification(PebbleNotification notificationSource)
    {
        Timber.d("notify internal");

        if (notificationSource.getSubtitle() == null)
        {
            //Attempt to figure out subtitle
            String subtitle = "";
            String text = notificationSource.getText();

            if (text.contains("\n"))
            {
                int firstLineBreak = text.indexOf('\n');
                if (firstLineBreak < 40 && firstLineBreak < text.length() * 0.8)
                {
                    subtitle = text.substring(0, firstLineBreak).trim();
                    text = text.substring(firstLineBreak).trim();
                }
            }

            notificationSource.setText(text);
            notificationSource.setSubtitle(subtitle);
        }

        if (notificationSource.getTitle().trim().equals(notificationSource.getSubtitle().trim()))
            notificationSource.setSubtitle("");

        notificationSource.setText(TextUtil.prepareString(notificationSource.getText(), TEXT_LIMIT));
        notificationSource.setTitle(TextUtil.prepareString(notificationSource.getTitle(), 30));
        notificationSource.setSubtitle(TextUtil.prepareString(notificationSource.getSubtitle(), 30));

        ProcessedNotification notification = new ProcessedNotification();
        notification.source = notificationSource;
        AppSettingStorage settingStorage = notificationSource.getSettingStorage(this);

        if (!notificationSource.isListNotification() &&
                !notificationSource.isHistoryDisabled() &&
                settingStorage.getBoolean(AppSetting.SAVE_TO_HISTORY) &&
                canDisplayWearGroupNotification(notification.source, settingStorage))
        {
            historyDb.storeNotification(System.currentTimeMillis(), notificationSource.getTitle(), notificationSource.getSubtitle(), notificationSource.getText());
        }

        if (!notificationSource.isListNotification())
        {

            if (notificationSource.getAndroidID() != null)
            {
                //Preventing spamming of equal notifications
                for (int i = 0; i < sentNotifications.size(); i++)
                {
                    ProcessedNotification comparing = sentNotifications.valueAt(i);
                    if (notificationSource.hasIdenticalContent(comparing.source))
                    {
                        Timber.d("notify failed - same notification exists");
                        return;
                    }
                }
            }

            if (settings.getBoolean(PebbleNotificationCenter.NOTIFICATIONS_DISABLED, false))
                return;

            if (settings.getBoolean("noNotificationsScreenOn", false))
            {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm.isScreenOn())
                {
                    Timber.d("notify failed - screen is on");
                    return;
                }
            }

            if (settings.getBoolean(PebbleNotificationCenter.NO_NOTIFY_VIBRATE, false))
            {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL)
                {
                    Timber.d("notify failed - ringer is silent");
                    return;
                }

            }

            if (settings.getBoolean("enableQuietTime", false) && !settingStorage.getBoolean(AppSetting.IGNORE_QUIET_HOURS))
            {
                int startHour = settings.getInt("quiteTimeStartHour", 0);
                int startMinute = settings.getInt("quiteTimeStartMinute", 0);
                int startTime = startHour * 60 + startMinute;

                int endHour = settings.getInt("quiteTimeEndHour", 23);
                int endMinute = settings.getInt("quiteTimeEndMinute", 59);
                int endTime = endHour * 60 + endMinute;

                Calendar calendar = Calendar.getInstance();
                int curHour = calendar.get(Calendar.HOUR_OF_DAY);
                int curMinute = calendar.get(Calendar.MINUTE);
                int curTime = curHour * 60 + curMinute;


                if ((endTime > startTime && curTime <= endTime && curTime >= startTime) || (endTime < startTime && (curTime <= endTime || curTime >= startTime)))
                {
                    Timber.d("notify failed - quiet time");
                    return;
                }
            }

            if (settings.getBoolean("noNotificationsNoPebble", false) && !isWatchConnected())
            {
                Timber.d("notify failed - watch not connected");
                return;
            }

            updateCurrentlyRunningApp();
            System.out.println("prev" + previousUUID);
            int pebbleAppMode = 0;
            if (previousUUID != null)
                pebbleAppMode = PreferencesUtil.getPebbleAppNotificationMode(settings, previousUUID);
            else
                pebbleAppMode = PreferencesUtil.getPebbleAppNotificationMode(settings, UNKNOWN_UUID);

            if (pebbleAppMode == 1) //Pebble native notification
            {
                String nativeTitle = notificationSource.getTitle();
                String nativeText = notificationSource.getSubtitle() + "\n\n" + notificationSource.getText();

                devConn.sendNotification(nativeTitle, nativeText);
                return;
            } else if (pebbleAppMode == 2) //No notification
            {
                Timber.d("notify failed - pebble app");
                return;
            }
        }

        Random rnd = new Random();
        do
        {
            notification.id = rnd.nextInt();
        }
        while (sentNotifications.get(notification.id) != null);

        if (!notification.source.isListNotification() && !canDisplayWearGroupNotification(notification.source, settingStorage))
        {
            sentNotifications.put(notification.id, notification);
            Timber.d("notify failed - group");
            return;
        }

        boolean oldBusy = commBusy;
        commBusy = true; //Prevent service from sending dismiss actions before app is even open
        processDismissUpwards(notificationSource.getAndroidID(), notificationSource.getPackage(), notificationSource.getTag(), true);
        commBusy = oldBusy;

        sentNotifications.put(notification.id, notification);

        String text = notificationSource.getText();

        while (text.length() > 0)
        {
            String chunk = TextUtil.trimString(text, 80, false);
            notification.textChunks.add(chunk);
            text = text.substring(chunk.length());
        }

        openApp();

        if (commBusy)
        {
            Timber.d("notify queued");
            sendingQueue.add(notification);
        } else
            send(notification);
    }

    private void updateCurrentlyRunningApp()
    {
        UUID currentApp = devConn.getCurrentRunningApp();

        if (currentApp != null && !(currentApp.getLeastSignificantBits() == 0 && currentApp.getMostSignificantBits() == 0) && (!currentApp.equals(DataReceiver.pebbleAppUUID) || previousUUID == null) && !currentApp.equals(UNKNOWN_UUID))
        {
            previousUUID = currentApp;
        }
    }

    private void openApp()
    {
        PebbleKit.startAppOnPebble(this, DataReceiver.pebbleAppUUID);
    }

    private void closeApp()
    {
        Timber.d("CloseApp " + previousUUID);
        commBusy = false;

        //startAppOnPebble seems to fail sometimes so I fallback to regular closing if it fails 2 times.
        if (settings.getBoolean(PebbleNotificationCenter.CLOSE_TO_LAST_APP, false) && previousUUID != null && !previousUUID.equals(DataReceiver.pebbleAppUUID) && !previousUUID.equals(MAIN_MENU_UUID) && closingAttempts < 3)
            PebbleKit.startAppOnPebble(this, previousUUID);
        else
            PebbleKit.closeAppOnPebble(this, DataReceiver.pebbleAppUUID);


        Editor editor = settings.edit();
        editor.putLong("lastClose", System.currentTimeMillis());
        editor.apply();

        closingAttempts++;
    }

    private void appOpened()
    {
        sendConfig(sendingQueue.size() > 0 || curSendingNotification != null);
    }

    private void configDelivered()
    {
        commWentIdle();
    }

    /**
     * Called when communication becomes idle and something else can be sent
     *
     * @return true if that function did anything, false if communication is still idle after calling.
     */
    public boolean commWentIdle()
    {
        Timber.i("Went idle");

        if (curSendingNotification != null)
        {
            send(curSendingNotification);
            return true;
        }

        ProcessedNotification next = sendingQueue.poll();
        if (next != null)
        {
            send(next);
            return true;
        }

        if (notificationRemovalQueue.size() > 0)
        {
            Integer nextRemovalNotifiaction = notificationRemovalQueue.poll();
            dismissOnPebble(nextRemovalNotifiaction, false);
            return true;
        }

        //Clean up excess history entries every day
        long lastDbCleanup = settings.getLong("lastCleanup", 0);
        if (System.currentTimeMillis() - lastDbCleanup > 1000 * 3600 * 24)
        {
            historyDb.cleanDatabase();
        }

        commBusy = false;

        return false;
    }

    /**
     * Starts timer that will mark communication as idle, if nothing happened in 10 seconds.
     */
    public void commStarted()
    {
        Timber.i("Not Idle");

        commBusy = true;
    }

    private void menuPicked(PebbleDictionary data)
    {
        int index = data.getUnsignedInteger(1).intValue();
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

    private void moreTextRequested(PebbleDictionary data)
    {
        Timber.d("More text requested...");

        int id = data.getInteger(1).intValue();

        ProcessedNotification notification = sentNotifications.get(id);
        if (notification == null)
        {
            Timber.d("Unknown ID!");

            notificationTransferCompleted();
            return;
        }

        int chunk = data.getUnsignedInteger(2).intValue();

        if (notification.textChunks.size() <= chunk)
        {
            Timber.d("Too much chunks!");

            notificationTransferCompleted();
            return;
        }

        data = new PebbleDictionary();

        data.addUint8(0, (byte) 1);
        data.addInt32(1, id);
        data.addUint8(2, (byte) chunk);
        data.addString(3, notification.textChunks.get(chunk));

        Timber.d("Sending more text...");

        PebbleKit.sendDataToPebble(this, DataReceiver.pebbleAppUUID, data);
        commStarted();
    }

    private void actionsTextRequested(PebbleDictionary data)
    {
        int id = data.getInteger(1).intValue();

        ProcessedNotification notification = sentNotifications.get(id);
        if (notification == null)
        {
            Timber.d("Unknown ID!");

            notificationTransferCompleted();
            return;
        }

        data = new PebbleDictionary();

        data.addUint8(0, (byte) 5);
        data.addInt32(1, id);

        List<NotificationAction> actions = notification.source.getActions();
        int size = Math.min(actions.size(), 5);

        byte[] textData = new byte[size * 19];

        for (int i = 0; i < size; i++)
        {
            String text = TextUtil.prepareString(actions.get(i).getActionText(), 18);
            System.arraycopy(text.getBytes(), 0, textData, i * 19, text.length());
            textData[19 * (i + 1) -1 ] = 0;
        }

        data.addBytes(3, textData);

        PebbleKit.sendDataToPebble(this, DataReceiver.pebbleAppUUID, data);
        commStarted();
    }


    private void notificationTransferCompleted()
    {
        Timber.d("Transfer completed...");

        curSendingNotification = null;

        Timber.d("csn null: " + (curSendingNotification == null));
        Timber.d("queue size: " + sendingQueue.size());

        if (commWentIdle())
            return;
    }

    private void pebbleSelectPressed(PebbleDictionary data)
    {
        int id = data.getInteger(1).intValue();
        boolean hold = data.contains(2);

        Timber.d("Select button pressed on Pebble, Hold: %b", hold);


        ProcessedNotification notification = sentNotifications.get(id);
        if (notification == null)
            return;

        if (notification.source.getActions() == null || notification.source.getActions().size() == 0)
        {
            dismissOnPhone(notification);
            return;
        }

        AppSetting relevantSetting = hold ? AppSetting.SELECT_HOLD_ACTION : AppSetting.SELECT_PRESS_ACTION;
        AppSettingStorage settingStorage = notification.source.getSettingStorage(this);

        int action = settingStorage.getInt(relevantSetting);

        if (notification.source.shouldForceActionMenu() || action == 2)
        {
            notification.activeActionList = new NotificationActionList(notification);
            notification.activeActionList.showList(this, notification);
        }
        else if (action == 0)
        {
            return;
        }
        else if (action == 1)
        {
            dismissOnPhone(notification);
        }
        else
        {
            action -= 3;
            if (notification.source.getActions().size() <= action)
                return;

            notification.source.getActions().get(action).executeAction(this, notification);
        }


    }

    public void dismissOnPhone(ProcessedNotification notification)
    {
        dismissOnPebble(notification.id, false);

        //Group messages can't be dismissed (they are not even displayed), so I should find relevat message in actual notification tray
        if (notification.source.getWearGroupType() == PebbleNotification.WEAR_GROUP_TYPE_GROUP_MESSAGE)
        {
            for (int i = 0; i < sentNotifications.size(); i++)
            {
                ProcessedNotification compare = sentNotifications.valueAt(i);

                if (notification.source.isInSameGroup(compare.source) && compare.source.getWearGroupType() == PebbleNotification.WEAR_GROUP_TYPE_GROUP_SUMMARY)
                {
                    notification = compare;
                    break;
                }
            }
        }

        if (!notification.source.isDismissable())
            return;

        JellybeanNotificationListener.dismissNotification(notification.source.getPackage(), notification.source.getTag(), notification.source.getAndroidID());

    }

    private void actionListPacket(int packetId, PebbleDictionary data)
    {
        int notificationId = data.getInteger(1).intValue();

        ProcessedNotification notification = sentNotifications.get(notificationId);
        if (notification != null)
        {
            ActionList list = notification.activeActionList;
            if (list != null)
            {
                list.handlePacket(this, packetId, data);
            }
        }
    }

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


        configBytes[7] = flags;

        configBytes[8] = (byte) (WatchappHandler.SUPPORTED_PROTOCOL >>> 0x08);
        configBytes[9] = (byte) WatchappHandler.SUPPORTED_PROTOCOL;
        configBytes[10] = (byte) Integer.parseInt(settings.getString(PebbleNotificationCenter.SHAKE_ACTION, "1"));
        configBytes[11] = (byte) (vibratingTimeout >>> 0x08);
        configBytes[12] = (byte) vibratingTimeout;

        data.addBytes(1, configBytes);

        Timber.d("Sending config...");

        PebbleKit.sendDataToPebble(this, DataReceiver.pebbleAppUUID, data);

        commBusy = false;
    }

    private void receivedConfigChange(PebbleDictionary data)
    {
        Editor editor = settings.edit();

        int id = data.getUnsignedInteger(1).intValue();
        switch (id)
        {
            case 0: //Notifications disabled
                boolean value = data.getUnsignedInteger(2) != 0;
                editor.putBoolean(PebbleNotificationCenter.NOTIFICATIONS_DISABLED, value);
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

        int id = data.getUnsignedInteger(0).intValue();

        Timber.d("Pebble packet %d", id);

        if (id != 7)
            closingAttempts = 0;

        switch (id)
		{
		case 0:
			appOpened();
			break;
		case 1:
			moreTextRequested(data);
			break;
		case 2:
			notificationTransferCompleted();
			break;
		case 4:
			if (listHandler != null) listHandler.gotRequest(data);
			break;
		case 5:
			if (listHandler != null) listHandler.entrySelected(data);
			break;
		case 6:
			menuPicked(data);
			break;
		case 7:
			closeApp();
			break;
		case 8:
			if (listHandler != null) listHandler.sendRelativeNotification(data);
			break;
		case 9:
			dismissOnPebbleSucceeded(data);
			break;
		case 10:
			configDelivered();
			break;
        case 11:
            receivedConfigChange(data);
            break;
        case 12:
            pebbleSelectPressed(data);
            break;
        case 13:
        case 3:
            actionListPacket(id, data);
            break;

        }
	}

    private boolean isWatchConnected()
    {
        try
        {
            return PebbleKit.isWatchConnected(this);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static boolean canDisplayWearGroupNotification(PebbleNotification notification, AppSettingStorage settingStorage)
    {
        boolean groupNotificationEnabled = settingStorage.getBoolean(AppSetting.USE_WEAR_GROUP_NOTIFICATIONS);
        if (notification.getWearGroupType() == PebbleNotification.WEAR_GROUP_TYPE_GROUP_SUMMARY && groupNotificationEnabled)
        {
            return false; //Don't send summary notifications, we will send group ones instead.
        }
        else if (notification.getWearGroupType() == PebbleNotification.WEAR_GROUP_TYPE_GROUP_MESSAGE && !groupNotificationEnabled)
        {
            return false; //Don't send group notifications, they are not enabled.
        }

        return true;
    }
}
