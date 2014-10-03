package com.matejdro.pebblenotificationcenter.lists;

import android.content.Intent;
import com.matejdro.pebblenotificationcenter.NotificationHistoryStorage;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;

public class NotificationHistoryAdapter extends NotificationListAdapter {
	private List<PebbleNotification> notifications;
	private NotificationHistoryStorage storage;
	
	public NotificationHistoryAdapter(PebbleTalkerService service, NotificationHistoryStorage storage) {
		super(service);
		this.storage = storage;
		
		loadNotifications();
	}
	
	public void loadNotifications()
	{
		Cursor cursor = storage.getReadableDatabase().rawQuery("SELECT PostTime, Title, Subtitle, Text FROM notifications ORDER BY PostTime DESC LIMIT 150", null);
		notifications = new ArrayList<PebbleNotification>(cursor.getCount());
		
		while (cursor.moveToNext())
		{
            long sendingDate = cursor.getLong(0);
            String title = cursor.getString(1);
            String text = cursor.getString(3) + "\n\nSent on " + getFormattedDate(sendingDate);

            PebbleNotification notification = new PebbleNotification(title, text, null);
			notification.setSubtitle(cursor.getString(2));
			notification.setPostTime(sendingDate);
            notification.setListNotification(true);

			notifications.add(notification);
		}
	}

	@Override
	public PebbleNotification getNotificationAt(int index) {
		return notifications.get(index);
	}

	@Override
	public int getNumOfNotifications() {
		return notifications.size();
	}

	@Override
	public void notificationPicked(int index) {
        PebbleNotification notification = notifications.get(index);

        Intent startIntent = new Intent(service, PebbleTalkerService.class);
        startIntent.putExtra("notification", notification);
        service.startService(startIntent);
	}
}
