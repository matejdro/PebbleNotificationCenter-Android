package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;

/**
 * Created by Matej on 19.10.2014.
 */
public class AppEnabledCheckboxItem extends CheckBoxItem
{
    public AppEnabledCheckboxItem(AppSettingStorage settingsStorage, int textResource, int descriptionResource)
    {
        super(settingsStorage, null, textResource, descriptionResource);
    }

    @Override
    protected void settingChanged(boolean change)
    {
        settingsStorage.setAppChecked(change);

    }

    @Override
    protected boolean getSavedValue()
    {
       return settingsStorage.isAppChecked();
    }
}
