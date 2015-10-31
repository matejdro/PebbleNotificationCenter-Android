package com.matejdro.pebblenotificationcenter.tasker;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.matejdro.pebblenotificationcenter.appsetting.AbsAppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Matej on 18.9.2014.
 */
public class BundleAppSettingsStorage extends AbsAppSettingStorage
{
    private AbsAppSettingStorage original;
    private Bundle storage;

    public BundleAppSettingsStorage(AbsAppSettingStorage original, Bundle storage)
    {
        this.original = original;
        this.storage = storage;
    }

    @Nullable
    @Override
    public String getStringByKey(String key)
    {
        if (storage.containsKey("setting_".concat(key)))
            return storage.getString("setting_".concat(key));

        return original.getStringByKey(key);
    }

    @Override
    public void setStringByKey(String key, String value)
    {
        storage.putString(key, value);
    }


    @Override
    public String getString(AppSetting setting)
    {
        if (storage.containsKey("setting_".concat(setting.getKey())))
            return storage.getString("setting_".concat(setting.getKey()));

        return original.getString(setting);
    }

    @Override
    public boolean getBoolean(AppSetting setting)
    {
        if (storage.containsKey("setting_".concat(setting.getKey())))
            return storage.getBoolean("setting_".concat(setting.getKey()));

        return original.getBoolean(setting);
    }

    @Override
    public int getInt(AppSetting setting)
    {
        if (storage.containsKey("setting_".concat(setting.getKey())))
            return storage.getInt("setting_".concat(setting.getKey()));

        return original.getInt(setting);
    }

    @Override
    public List<String> getStringList(AppSetting setting)
    {
        if (storage.containsKey("setting_".concat(setting.getKey())))
            return storage.getStringArrayList("setting_".concat(setting.getKey()));

        return original.getStringList(setting);
    }

    @Override
    public void setString(AppSetting setting, String val)
    {
        if (original.getString(setting).equals(val))
        {
            deleteSetting(setting);
            return;
        }

        storage.putString("setting_".concat(setting.getKey()), val);
    }

    @Override
    public void setBoolean(AppSetting setting, boolean val)
    {
        if (original.getBoolean(setting) == val)
        {
            deleteSetting(setting);
            return;
        }

        storage.putBoolean("setting_".concat(setting.getKey()), val);
    }

    @Override
    public void setInt(AppSetting setting, int val)
    {
        if (original.getInt(setting) == val)
        {
            deleteSetting(setting);
            return;
        }

        storage.putInt("setting_".concat(setting.getKey()), val);
    }

    @Override
    public void setStringList(AppSetting setting, Collection<String> val)
    {
        if (original.getStringList(setting).equals(val))
        {
            deleteSetting(setting);
            return;
        }

        storage.putStringArrayList("setting_".concat(setting.getKey()), new ArrayList<String>(val));
    }

    @Override
    public void deleteSetting(AppSetting setting)
    {
        storage.remove("setting_".concat(setting.getKey()));
    }

    @Override
    public boolean isAppChecked()
    {
        if (storage.containsKey("special_appchecked"))
            return storage.getBoolean("special_appchecked");

        return original.isAppChecked();
    }

    @Override
    public void setAppChecked(boolean checked)
    {
        storage.putBoolean("special_appchecked", checked);
    }

    @Override
    public boolean canAppSendNotifications()
    {
        throw new UnsupportedOperationException(); //This class is never supposed to be used with actual notifications
    }
}
