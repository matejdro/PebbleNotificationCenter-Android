package com.matejdro.pebblenotificationcenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CustomNotificationCatcher extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
        AppSettingStorage storage = new SharedPreferencesAppStorage(context, AppSetting.VIRTUAL_APP_THIRD_PARTY, PebbleNotificationCenter.getInMemorySettings().getDefaultSettingsStorage(), true);
		if (storage.canAppSendNotifications())
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
						PebbleTalkerService.notify(context, AppSetting.VIRTUAL_APP_THIRD_PARTY, title, subtitle, text, true, false);
					}
					else
					{
						PebbleTalkerService.notify(context, AppSetting.VIRTUAL_APP_THIRD_PARTY, title, subtitle, text);
					}
				}
				else
				{
					if (text.endsWith("NOHISTORY"))
					{
						text = text.substring(0, text.length() - 9);
						PebbleTalkerService.notify(context, AppSetting.VIRTUAL_APP_THIRD_PARTY, title, text, true);
					}
					else
					{
						PebbleTalkerService.notify(context, AppSetting.VIRTUAL_APP_THIRD_PARTY, title, text);
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
