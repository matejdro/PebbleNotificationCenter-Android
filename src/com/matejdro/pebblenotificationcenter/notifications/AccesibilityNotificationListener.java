package com.matejdro.pebblenotificationcenter.notifications;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityEvent;

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
		
		NotificationHandler.newNotification(this, event.getPackageName().toString(), notification, null, null, false);
	}

	@Override
	public void onInterrupt() {
	}
}
