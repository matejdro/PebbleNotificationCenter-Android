package com.matejdro.pebblenotificationcenter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class CustomNotificationCatcher extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		if (settings.getBoolean("enableCustom", true))
		{
			String notificationData = intent.getStringExtra("notificationData");
			if (notificationData == null)
				return;
			try
			{
				JSONArray array = new JSONArray(notificationData);
				JSONObject data = array.getJSONObject(0);
				
				String title = data.getString("title");
				String text = data.getString("body");
				
				if (data.has("subtitle"))
				{
					String subtitle = data.getString("subtitle");
					
					if (text.endsWith("NOHISTORY"))
					{
						text = text.substring(0, text.length() - 9);
						PebbleTalkerService.notify(context, title, subtitle, text, true, false);
					}
					else
					{
						PebbleTalkerService.notify(context, title, subtitle, text);
					}
				}
				else
				{
					if (text.endsWith("NOHISTORY"))
					{
						text = text.substring(0, text.length() - 9);
						PebbleTalkerService.notify(context, title, text, true);
					}
					else
					{
						PebbleTalkerService.notify(context, title, text);
					}
				}
				
			}
			catch (JSONException e)
			{
				return;
			}
			
		}
	}

}
