package com.matejdro.pebblenotificationcenter.tasker;

import android.content.Intent;
import android.os.Bundle;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.ui.perapp.PerAppActivity;

/**
 * Created by Matej on 18.9.2014.
 */
public class TaskerAppSettingsActivity extends PerAppActivity
{
    private Bundle storage;

    @Override
    public AppSettingStorage initAppSettingStorage()
    {
        storage = new Bundle();
        loadIntent();

        AppSettingStorage original = super.initAppSettingStorage();
        return new BundleAppSettingsStorage(original, storage);
    }

    protected void loadIntent() {
        Intent intent = getIntent();

        if (intent == null)
            return;

        Bundle bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
        if (bundle == null)
            return;

        int action = bundle.getInt("action");
        if (action != 2)
            return;

        storage.putAll(bundle);
        for (String key : bundle.keySet())
        {
            if (!key.startsWith("setting_") && !key.startsWith("special_"))
            {
                storage.remove(key);
            }
        }
    }

    @Override
    public void save()
    {
        super.save();

        Intent intent = new Intent();

        Bundle bundle = new Bundle();

        bundle.putInt("action", 2);
        String description = getString(R.string.taskerDescriptionPerAppSetting, appName);

        bundle.putAll(storage);
        bundle.putString("appName", appName);
        bundle.putString("appPackage", appPackage);

        intent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", description);
        intent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", bundle);

        setResult(RESULT_OK, intent);
    }

}