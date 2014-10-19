package com.matejdro.pebblenotificationcenter.ui.perapp.settingitems;

import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import java.util.List;

/**
 * Created by Matej on 19.10.2014.
 */
public class UseDefaultCheckBoxItem extends CheckBoxItem
{
    public UseDefaultCheckBoxItem(AppSettingStorage settingsStorage, int textResource, int descriptionResource)
    {
        super(settingsStorage, null, textResource, descriptionResource);
    }

    @Override
    protected void settingChanged(boolean change)
    {
        settingsStorage.setAppUseDefaultSettings(change);
        updateOtherItems(!change);

    }

    private void updateOtherItems(boolean enabled)
    {
        for (int i = 1; i < activity.settings.size(); i++)
        {
            List<BaseSettingItem> items = activity.settings.get(i).settings;
            for (BaseSettingItem item : items)
                item.setEnabled(enabled);
        }
    }

    @Override
    protected boolean getSavedValue()
    {
        boolean val = settingsStorage.shouldAppUseDefaultSettings();

        updateOtherItems(!val);
        return val;
    }
}
