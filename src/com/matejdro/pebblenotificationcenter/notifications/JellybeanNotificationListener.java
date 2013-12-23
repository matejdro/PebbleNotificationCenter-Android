package com.matejdro.pebblenotificationcenter.notifications;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.matejdro.pebblenotificationcenter.util.SettingsMemoryStorage;

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class JellybeanNotificationListener extends NotificationListenerService {
	private Handler handler;
	public static JellybeanNotificationListener instance;
	
	@Override
	public void onDestroy() {
		NotificationHandler.active = false;
		
		instance = null;
	}

	@Override
	public void onCreate() {
		handler = new Handler();
		instance = this;
		
		NotificationHandler.active = true;
		
		super.onCreate();
	}

	@Override
	public void onNotificationPosted(final StatusBarNotification sbn) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				NotificationHandler.newNotification(JellybeanNotificationListener.this, sbn.getPackageName(), sbn.getNotification(), sbn.getId(), sbn.getTag(), true);
			}
		});
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		NotificationHandler.notificationDismissedOnPhone(this, sbn.getPackageName(), sbn.getTag(), sbn.getId());
	}

	public static void dismissNotification(String pkg, String tag, int id)
	{
		Log.d("PebbleNotifier", "dismissing");

		instance.cancelNotification(pkg, tag, id);
	}

	public static StatusBarNotification[] getCurrentNotifications()
	{
		if (instance == null)
			return new StatusBarNotification[0];
		
		return instance.getActiveNotifications();
	}
}
