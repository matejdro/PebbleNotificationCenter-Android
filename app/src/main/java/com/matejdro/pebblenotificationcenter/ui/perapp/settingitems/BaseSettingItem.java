package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.view.View;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

/**
 * Created by Matej on 19.10.2014.
 */
public abstract class BaseSettingItem
{
    protected AppSettingStorage settingsStorage;

    protected BaseSettingItem(AppSettingStorage settingsStorage)
    {
        this.settingsStorage = settingsStorage;
    }

    public abstract View getView(PerAppActivity activity);
    public abstract boolean onClose();
    public abstract void setEnabled(boolean enabled);
}
