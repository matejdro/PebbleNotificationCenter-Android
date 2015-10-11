package com.matejdro.pebblenotificationcenter.appsetting;

import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Created by Matej on 16.9.2014.
 */
public interface AppSettingStorage
{
    public @Nullable
    String getStringByKey(String key);
    public String getString(AppSetting setting);
    public boolean getBoolean(AppSetting setting);
    public int getInt(AppSetting setting);
    public List<String> getStringList(AppSetting setting);
    public <T extends Enum<?>> T getEnum(AppSetting setting);

    public void setString(AppSetting setting, String val);
    public void setBoolean(AppSetting setting, boolean val);
    public void setInt(AppSetting setting, int val);
    public void setStringList(AppSetting setting, Collection<String> val);
    public void setEnum(AppSetting setting, Enum<?> val);

    /*
        Returns true if app is Checked (either excluded or included depending on option)
     */
    public boolean isAppChecked();
    public void setAppChecked(boolean checked);
    public boolean canAppSendNotifications();
}
