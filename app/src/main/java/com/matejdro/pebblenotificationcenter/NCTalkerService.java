package com.matejdro.pebblenotificationcenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.SparseArray;

import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblecommons.pebble.PebbleDeveloperConnection;
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
import com.matejdro.pebblenotificationcenter.ui.XposedSettingsActivity;

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

        //noinspection ConstantConditions
        if (PebbleNotificationCenter.isXposedModuleRunning())
        {
            @SuppressLint("WorldReadableFiles")
            SharedPreferences xposedPreferences = getSharedPreferences(XposedSettingsActivity.SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
            setEnableDeveloperConnectionRefreshing(!xposedPreferences.getBoolean(XposedSettingsActivity.SETTING_FIX_DEVELOPER_CONNECTION, false));
        }
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
    protected PebbleDeveloperConnection createDeveloperConnection() throws URISyntaxException {
        NotificationCenterDeveloperConnection developerConnection = new NotificationCenterDeveloperConnection(this);

        NativeNotificationActionHandler actionHandler = new NativeNotificationActionHandler(this);
        developerConnection.registerActionHandler(actionHandler);

        return developerConnection;
    }
}
