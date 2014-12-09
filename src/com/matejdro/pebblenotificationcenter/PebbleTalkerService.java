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
import com.matejdro.pebblenotificationcenter.pebble.modules.ActionsModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.CommModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.DismissUpwardsModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.ListModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.SystemModule;
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
    public static final String INTENT_PEBBLE_PACKET = "PebblePacket";
    public static final String INTENT_PEBBLE_ACK = "PebbleAck";
    public static final String INTENT_PEBBLE_NACK = "PebbleNack";

    private SharedPreferences settings;
    private DefaultAppSettingsStorage defaultSettingsStorage;
    private NotificationHistoryStorage historyDb;

    private PebbleDeveloperConnection devConn;

    public SparseArray<ProcessedNotification> sentNotifications = new SparseArray<ProcessedNotification>();

    private PebbleCommunication pebbleCommunication;

    private LocationLookup locationLookup;

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

       initDeveloperConnection();

        locationLookup = new LocationLookup(this.getApplicationContext());
        locationLookup.lookup();
        pebbleCommunication = new PebbleCommunication(this);

        addModule(new SystemModule(this), SystemModule.MODULE_SYSTEM);
        addModule(new NotificationSendingModule(this), NotificationSendingModule.MODULE_NOTIFICATION_SENDING);
        addModule(new ListModule(this), ListModule.MODULE_LIST);
        addModule(new DismissUpwardsModule(this), DismissUpwardsModule.MODULE_DISMISS_UPWARDS);
        addModule(new ActionsModule(this), ActionsModule.MODULE_ACTIONS);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
       if (intent != null && intent.getAction() != null)
       {
           if (intent.getAction().equals(INTENT_PEBBLE_PACKET))
           {
               String json = intent.getStringExtra("packet");
               receivedPacketFromPebble(json);
           }
           else if (intent.getAction().equals(INTENT_PEBBLE_ACK))
           {
               int transactionId = intent.getIntExtra("transactionId", -1);
               getPebbleCommunication().receivedAck(transactionId);
           }
           else if (intent.getAction().equals(INTENT_PEBBLE_NACK))
           {
               int transactionId = intent.getIntExtra("transactionId", -1);
               getPebbleCommunication().receivedNack(transactionId);
           }
           else
           {
               CommModule receivingModule = registeredIntents.get(intent.getAction());
               if (receivingModule != null)
                   receivingModule.gotIntent(intent);
           }
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

    public SharedPreferences getGlobalSettings()
    {
        return settings;
    }

    public DefaultAppSettingsStorage getDefaultSettingsStorage()
    {
        return defaultSettingsStorage;
    }

    public PebbleCommunication getPebbleCommunication()
    {
        return pebbleCommunication;
    }

    public LocationLookup getLocationLookup()
    {
        return locationLookup;
    }

    private void initDeveloperConnection()
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


    public PebbleDeveloperConnection getDeveloperConnection()
    {
        if (!devConn.isOpen())
            initDeveloperConnection();

        return devConn;
    }

    public NotificationHistoryStorage getHistoryDatabase()
    {
        return historyDb;
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


        int destination = data.getUnsignedIntegerAsLong(0).intValue();
        Timber.d("Pebble packet for " + destination);

        CommModule module = modules.get(destination);
        if (module == null)
        {
            Crashlytics.logException(new NullPointerException("Destination module does not exist: " + destination));
            return;
        }

        module.gotMessageFromPebble(data);

	}
}
