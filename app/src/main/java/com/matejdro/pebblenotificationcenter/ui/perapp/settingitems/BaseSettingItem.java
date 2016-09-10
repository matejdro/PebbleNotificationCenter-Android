package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import android.support.annotation.Nullable;
import android.view.View;

import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

/**
 * Created by Matej on 19.10.2014.
 */
public abstract class BaseSettingItem
{
    protected AppSettingStorage settingsStorage;
    private AppSetting associatedSetting;

    protected BaseSettingItem(AppSettingStorage settingsStorage, AppSetting associatedSetting) {
        this.settingsStorage = settingsStorage;
        this.associatedSetting = associatedSetting;
    }

    public @Nullable AppSetting getAssociatedSetting() {
        return associatedSetting;
    }

    public abstract View getView(PerAppActivity activity);
    public abstract boolean onClose();
    public abstract void setEnabled(boolean enabled);
}
