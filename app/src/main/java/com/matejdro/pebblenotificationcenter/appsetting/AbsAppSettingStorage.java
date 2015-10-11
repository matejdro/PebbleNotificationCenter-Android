package com.matejdro.pebblenotificationcenter.appsetting;

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
        setString(setting, val.name());
    }
}
