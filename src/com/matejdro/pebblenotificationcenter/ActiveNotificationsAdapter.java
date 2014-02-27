package com.matejdro.pebblenotificationcenter;

import java.util.Arrays;
import java.util.Comparator;

import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import com.matejdro.pebblenotificationcenter.notifications.JellybeanNotificationListener;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.notifications.NotificationParser;

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ActiveNotificationsAdapter extends NotificationListAdapter {
	private PendingNotification[] notifications;
	
	public ActiveNotificationsAdapter(PebbleTalkerService service) {
		super(service);
		
		loadNotifications();
	}
	
	private void loadNotifications()
	{
		StatusBarNotification[] sbns = JellybeanNotificationListener.getCurrentNotifications();
		notifications = new PendingNotification[sbns.length];
		
		for (int i = 0; i < sbns.length; i++)
		{
			StatusBarNotification sbn = sbns[i];
			Notification notification = sbn.getNotification();
			NotificationParser parser = new NotificationParser(service, notification);
			PendingNotification pn = new PendingNotification();
			pn.androidID = sbn.getId();
			pn.dismissable = sbn.isClearable();
			pn.pkg = sbn.getPackageName();
			pn.title = NotificationHandler.getAppName(service, pn.pkg);
			pn.subtitle = parser.title;
			pn.text = parser.text;
			pn.postTime = sbn.getPostTime();
			pn.tag = sbn.getTag();
			
			notifications[i] = pn;
		}
		
		Arrays.sort(notifications, new PendingComparable());
	}
	
	@Override
	public NotificationMeta getNotificationAt(int index) {
		PendingNotification notification = notifications[index];
		
		NotificationMeta meta = new NotificationMeta();
		meta.title = notification.title;
		meta.subtitle = notification.subtitle;
		meta.date = notification.postTime;
		meta.isOngoing = !notification.dismissable;
		
		return meta;
	}

	@Override
	public int getNumOfNotifications() {
		return notifications.length;
	}

	@Override
	public void notificationPicked(int index) {
		PendingNotification pn = notifications[index];
		
		PebbleTalkerService.notify(service, pn.androidID, pn.pkg, pn.tag, pn.title, pn.subtitle, pn.text, pn.dismissable, true, true);
	}
	
	private static class PendingComparable implements Comparator<PendingNotification>
	{

		@Override
		public int compare(PendingNotification lhs, PendingNotification rhs) {
			//First sort by normal/onging, then by date.
			
			if (lhs.dismissable != rhs.dismissable)
			{
				if (lhs.dismissable)
					return -1;
				else
					return 11;
			}
			
			return (int) (rhs.postTime - lhs.postTime);
		}
		
	}
}
