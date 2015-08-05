package com.matejdro.pebblenotificationcenter;

import android.util.SparseArray;

import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.appsetting.DefaultAppSettingsStorage;
import com.matejdro.pebblenotificationcenter.location.LocationLookup;
import com.matejdro.pebblenotificationcenter.pebble.NativeNotificationActionHandler;
import com.matejdro.pebblenotificationcenter.pebble.NotificationCenterDeveloperConnection;
import com.matejdro.pebblenotificationcenter.pebble.modules.ActionsModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.DismissUpwardsModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.ImageSendingModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.ListModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.SystemModule;

import java.net.URISyntaxException;

public class NCTalkerService extends PebbleTalkerService
{
    private DefaultAppSettingsStorage defaultSettingsStorage;
    private NotificationHistoryStorage historyDb;

    public SparseArray<ProcessedNotification> sentNotifications = new SparseArray<ProcessedNotification>();

    private LocationLookup locationLookup;

    @Override
    public void onCreate()
    {
        super.onCreate();

        locationLookup = new LocationLookup(this.getApplicationContext());
        locationLookup.lookup();

        defaultSettingsStorage = new DefaultAppSettingsStorage(getGlobalSettings(), getGlobalSettings().edit());
        historyDb = new NotificationHistoryStorage(this);
    }

    @Override
    public void onDestroy()
    {
        historyDb.close();
        locationLookup.close();

        super.onDestroy();
    }

    @Override
    public void registerModules()
    {
        addModule(new SystemModule(this), SystemModule.MODULE_SYSTEM);
        addModule(new NotificationSendingModule(this), NotificationSendingModule.MODULE_NOTIFICATION_SENDING);
        addModule(new ListModule(this), ListModule.MODULE_LIST);
        addModule(new DismissUpwardsModule(this), DismissUpwardsModule.MODULE_DISMISS_UPWARDS);
        addModule(new ActionsModule(this), ActionsModule.MODULE_ACTIONS);
        addModule(new ImageSendingModule(this), ImageSendingModule.MODULE_IMAGE_SENDING);
    }

    public LocationLookup getLocationLookup()
    {
        return locationLookup;
    }

    public NotificationHistoryStorage getHistoryDatabase()
    {
        return historyDb;
    }

    public DefaultAppSettingsStorage getDefaultSettingsStorage()
    {
        return defaultSettingsStorage;
    }

    public static NCTalkerService fromPebbleTalkerService(PebbleTalkerService service)
    {
        return (NCTalkerService) service;
    }


    @Override
    protected void initDeveloperConnection()
    {
        try
        {
            devConn = new NotificationCenterDeveloperConnection(this);

            devConn.connectBlocking();
            NativeNotificationActionHandler actionHandler = new NativeNotificationActionHandler(this);
            NotificationCenterDeveloperConnection.fromDevConn(devConn).registerActionHandler(actionHandler);

        } catch (InterruptedException e)
        {
        } catch (URISyntaxException e)
        {
        }
    }
}
