package com.matejdro.pebblenotificationcenter.notifications;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityEvent;
import com.matejdro.pebblenotificationcenter.NotificationKey;
import timber.log.Timber;

public class AccesibilityNotificationListener extends AccessibilityService {
	public static AccesibilityNotificationListener instance;
	
	@Override
	public void onCreate() {
		instance = this;
		NotificationHandler.active = true;
		
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		NotificationHandler.active = false;
		
		super.onDestroy();
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		Parcelable parcelable = event.getParcelableData();
		if (!(parcelable instanceof Notification))
			return;
		
		Notification notification = (Notification) parcelable;

        Timber.d("Got new accessibility notification");
        NotificationHandler.newNotification(this, new NotificationKey(event.getPackageName().toString(), null, null), notification, false);
	}

	@Override
	public void onInterrupt() {
	}
}
