package com.matejdro.pebblenotificationcenter.appsetting;

import android.content.Context;
import android.content.SharedPreferences;
import com.matejdro.pebblenotificationcenter.util.PreferencesUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Matej on 16.9.2014.
 */
public class SharedPreferencesAppStorage implements AppSettingStorage
{
    private DefaultAppSettingsStorage defaultConfig;
    private SharedPreferences appConfig;
    private SharedPreferences.Editor editor;
    private String appPackage;
    private boolean obeyDefaultSettingsOption;

    public SharedPreferencesAppStorage(Context context, String appPackage, DefaultAppSettingsStorage defaultConfig, boolean obeyDefaultSettingsOption)
    {
        this.defaultConfig = defaultConfig;
        appConfig = context.getSharedPreferences("app_".concat(filterAppName(appPackage)), Context.MODE_PRIVATE);
        editor = appConfig.edit();
        this.appPackage = appPackage;
        this.obeyDefaultSettingsOption = obeyDefaultSettingsOption;
    }

    @Override
    public String getString(AppSetting setting)
    {
        if (!appConfig.contains(setting.getKey()) || (shouldAppUseDefaultSettings() && obeyDefaultSettingsOption))
            return defaultConfig.getString(setting);

        return appConfig.getString(setting.getKey(), null);
    }

    @Override
    public boolean getBoolean(AppSetting setting)
    {
        if (!appConfig.contains(setting.getKey()) || (shouldAppUseDefaultSettings() && obeyDefaultSettingsOption))
            return defaultConfig.getBoolean(setting);

        return appConfig.getBoolean(setting.getKey(), false);
    }

    @Override
    public int getInt(AppSetting setting)
    {
        if (!appConfig.contains(setting.getKey()) || (shouldAppUseDefaultSettings() && obeyDefaultSettingsOption))
            return defaultConfig.getInt(setting);

        return appConfig.getInt(setting.getKey(), 0);
    }

    @Override
    public List<String> getStringList(AppSetting setting)
    {
        if (!appConfig.contains(setting.getKey()) || (shouldAppUseDefaultSettings() && obeyDefaultSettingsOption))
            return defaultConfig.getStringList(setting);

        List<String> list = new ArrayList<String>();
        PreferencesUtil.loadCollection(appConfig, list, setting.getKey());

        return list;
    }

    @Override
    public void setString(AppSetting setting, String val)
    {
        editor.putString(setting.getKey(), val);
        editor.apply();
    }

    @Override
    public void setBoolean(AppSetting setting, boolean val)
    {
        editor.putBoolean(setting.getKey(), val);
        editor.apply();
    }

    @Override
    public void setInt(AppSetting setting, int val)
    {
        editor.putInt(setting.getKey(), val);
        editor.apply();
    }

    @Override
    public void setStringList(AppSetting setting, Collection<String> val)
    {
        PreferencesUtil.saveCollection(editor, val, setting.getKey());
    }

    @Override
    public boolean isAppChecked()
    {
        return defaultConfig.isAppChecked(appPackage);
    }

    @Override
    public void setAppChecked(boolean checked)
    {
        defaultConfig.setAppChecked(appPackage, checked);
    }

    @Override
    public boolean canAppSendNotifications()
    {
        return defaultConfig.canAppSendNotifications(appPackage);
    }

    @Override
    public boolean shouldAppUseDefaultSettings()
    {
        return appConfig.getBoolean("useDefaultSettings", true);
    }

    @Override
    public void setAppUseDefaultSettings(boolean val)
    {
        editor.putBoolean("useDefaultSettings", val);
    }

    public static String filterAppName(String name)
    {
        return name.replaceAll("[^0-9a-zA-Z ]", "_");
    }
}
