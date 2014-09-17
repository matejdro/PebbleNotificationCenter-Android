package com.matejdro.pebblenotificationcenter.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.appsetting.DefaultAppSettingsStorage;
import java.util.HashMap;
import java.util.Iterator;

public class SettingsMemoryStorage {
	private Context context;	
	
	private boolean dirty = true;
	
	private SharedPreferences preferences;
    private DefaultAppSettingsStorage appSettingsStorage;
	private HashMap<String, String> replacingStrings;
	
	public SettingsMemoryStorage(Context context)
	{
		this.context = context;
		this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
		this.replacingStrings = new HashMap<String, String>();
	}
	
	public void markDirty()
	{
		dirty = true;
	}
	
	private void loadSettings()
	{
		replacingStrings.clear();

		preferences = PreferenceManager.getDefaultSharedPreferences(context);
        appSettingsStorage = new DefaultAppSettingsStorage(preferences, preferences.edit());

		Iterator<String> replacingKeys = PreferencesUtil.getDirectIterator(preferences, PebbleNotificationCenter.REPLACING_KEYS_LIST);
		Iterator<String> replacingValues = PreferencesUtil.getDirectIterator(preferences, PebbleNotificationCenter.REPLACING_VALUES_LIST);
		while (replacingKeys.hasNext() && replacingValues.hasNext())
		{
			String keyString = replacingKeys.next();
			if (keyString.isEmpty())
				continue;
			
			String valueString = replacingValues.next();
			
			replacingStrings.put(keyString, valueString);
		}

		dirty = false;
	}
	
	public SharedPreferences getSharedPreferences()
	{
		if (dirty)
			loadSettings();
		
		return preferences;
	}

    public DefaultAppSettingsStorage getDefaultSettingsStorage()
    {
        if (dirty)
            loadSettings();

        return appSettingsStorage;
    }


	public HashMap<String, String> getReplacingStrings()
	{
		if (dirty)
			loadSettings();
		
		return replacingStrings;
	}
}
