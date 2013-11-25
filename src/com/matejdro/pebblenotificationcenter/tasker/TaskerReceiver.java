package com.matejdro.pebblenotificationcenter.tasker;

import java.io.IOException;

import net.dinglisch.android.tasker.TaskerPlugin;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.R;

public class TaskerReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null)
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
			
			PebbleTalkerService.notify(context, title, subtitle, body, !storeInHistory, false);
		}
		else if (action == 1)
		{			
			boolean enable = bundle.getBoolean("value");
			String key = bundle.getString("key");
			
			if (isSettingCheckBox(context, key))
			{
				Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
				editor.putBoolean(key, enable);
				editor.apply();
			}

		}
	}
	
	private boolean isSettingCheckBox(Context context, String key)
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
									
				if (!parser.getName().equals("CheckBoxPreference"))
					continue;
				
				for (int i = 0; i < parser.getAttributeCount(); i++)
				{
					if (parser.getAttributeName(i).equals("key") && parser.getAttributeValue(i).equals(key))
					{
						return true;
					}
				}
			}

		}
		catch (XmlPullParserException e)
		{
		} 
		catch (IOException e) {
		}

		return false;
	}

}
