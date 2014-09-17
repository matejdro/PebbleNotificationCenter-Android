package com.matejdro.pebblenotificationcenter.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.R;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.DefaultAppSettingsStorage;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class TaskerReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null)
			return;

        DefaultAppSettingsStorage storage = PebbleNotificationCenter.getInMemorySettings().getDefaultSettingsStorage();
        if (!storage.canAppSendNotifications(AppSetting.VIRTUAL_APP_TASKER_RECEIVER))
            return;

        Bundle bundle = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
		if (bundle == null)
			return;
		
		int action = bundle.getInt("action");
				
		
		if (action == 0)
		{
			String title = bundle.getString("title");
			String subtitle = bundle.getString("subtitle");
			String body = bundle.getString("body");
			
			boolean storeInHistory = bundle.getBoolean("storeInHistory");
						
			PebbleTalkerService.notify(context, AppSetting.VIRTUAL_APP_TASKER_RECEIVER, title, subtitle, body, !storeInHistory, false);
		}
		else if (action == 1)
		{			
			Object value = bundle.get("value");
			String key = bundle.getString("key");
			
			int settingType = getSettingType(context, key);
			if (settingType < 0)
				return;
			
			Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

			
			if (settingType == 0)
			{
				if (value instanceof Boolean)
					editor.putBoolean(key, (Boolean) value);
				else
					editor.putBoolean(key, value.equals("1"));
			}
			else
			{
				editor.putString(key, (String) value);
			}
			
			editor.apply();
		}
	}
	
	private int getSettingType(Context context, String key)
	{
		XmlResourceParser parser = context.getResources().getXml(R.xml.settings);
		try
		{
			while (true)
			{
				int element = parser.next();
				if (element == XmlPullParser.END_DOCUMENT)
					break;
				
				if (element != XmlPullParser.START_TAG)
					continue;
						
				int type = -1;
				
				if (parser.getName().equals("CheckBoxPreference"))
					type = 0;
				else if (parser.getName().equals("ListPreference"))
					type = 1;
				else if (parser.getName().equals("EditTextPreference"))
					type = 1;
				else 
					continue;
				
				for (int i = 0; i < parser.getAttributeCount(); i++)
				{
					if (parser.getAttributeName(i).equals("key") && parser.getAttributeValue(i).equals(key))
					{
						return type;
					}
				}
			}

		}
		catch (XmlPullParserException e)
		{
		} 
		catch (IOException e) {
		}

		return -1;
	}

}
