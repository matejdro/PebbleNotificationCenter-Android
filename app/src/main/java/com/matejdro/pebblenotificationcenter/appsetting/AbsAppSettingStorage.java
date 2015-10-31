package com.matejdro.pebblenotificationcenter.appsetting;

import android.support.annotation.Nullable;

import com.crashlytics.android.core.CrashlyticsCore;

public abstract class AbsAppSettingStorage implements AppSettingStorage
{
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<?>> T getEnum(AppSetting setting)
    {
        Enum defEnum = (Enum) setting.getDefault();

        String name = getStringByKey(setting.getKey());

        if (name == null)
            return (T) defEnum;

        try
        {
            return (T) Enum.valueOf(defEnum.getClass(), name);
        }
        catch (IllegalArgumentException e)
        {
            CrashlyticsCore.getInstance().logException(e);
            return (T) defEnum;
        }
    }

    @Override
    public void setEnum(AppSetting setting, Enum<?> val)
    {
        setStringByKey(setting.getKey(), val.name());
    }

    public abstract @Nullable String getStringByKey(String key);
    public abstract void setStringByKey(String key, String value);
}
