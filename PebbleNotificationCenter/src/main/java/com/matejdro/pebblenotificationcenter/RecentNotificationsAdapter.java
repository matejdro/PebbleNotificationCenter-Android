package com.matejdro.pebblenotificationcenter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

public class RecentNotificationsAdapter extends NotificationListAdapter {
	private List<NotificationMeta> notifications;
	private NotificationHistoryStorage storage;
	
	public RecentNotificationsAdapter(PebbleTalkerService service, NotificationHistoryStorage storage) {
		super(service);
		this.storage = storage;
		
		loadNotifications();
	}
	
	public void loadNotifications()
	{
		Cursor cursor = storage.getReadableDatabase().rawQuery("SELECT PostTime, Title, Subtitle, Text FROM notifications ORDER BY PostTime DESC LIMIT 150", null);
		notifications = new ArrayList<NotificationMeta>(cursor.getCount());
		
		while (cursor.moveToNext())
		{
			NotificationMeta notification = new NotificationMeta();
			notification.title = cursor.getString(1);
			notification.subtitle = cursor.getString(2);
			notification.date = cursor.getLong(0);
			notification.text = cursor.getString(3) + "\n\nSent on " + getFormattedDate(notification.date);
			notification.isOngoing = false;
			
			notifications.add(notification);
		}
	}

	@Override
	public NotificationMeta getNotificationAt(int index) {
		return notifications.get(index);
	}

	@Override
	public int getNumOfNotifications() {
		return notifications.size();
	}

	@Override
	public void notificationPicked(int index) {
		NotificationMeta notification = notifications.get(index);
		
		PebbleTalkerService.notify(service, notification.title, notification.subtitle, notification.text, true, true);
	}
}
