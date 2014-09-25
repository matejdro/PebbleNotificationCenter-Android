package com.matejdro.pebblenotificationcenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.DefaultAppSettingsStorage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CustomNotificationCatcher extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
        DefaultAppSettingsStorage storage = PebbleNotificationCenter.getInMemorySettings().getDefaultSettingsStorage();
        if (storage.canAppSendNotifications(AppSetting.VIRTUAL_APP_THIRD_PARTY))
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
                boolean noHistory = false;
                if (text.endsWith("NOHISTORY"))
                {
                    text = text.substring(0, text.length() - 9);
                    noHistory = true;
                }

                PebbleNotification notification = new PebbleNotification(title, text, AppSetting.VIRTUAL_APP_THIRD_PARTY);
                notification.setNoHistory(noHistory);
                if (data.has("subtitle"))
                    notification.setSubtitle(data.getString("subtitle"));

                Intent startIntent = new Intent(context, PebbleTalkerService.class);
                startIntent.putExtra("notification", notification);
                context.startService(startIntent);
			}
			catch (JSONException e)
			{
				return;
			}
			
		}
	}

}
