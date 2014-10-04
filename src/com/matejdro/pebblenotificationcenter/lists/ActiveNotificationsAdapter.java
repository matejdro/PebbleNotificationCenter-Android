package com.matejdro.pebblenotificationcenter.lists;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.notifications.JellybeanNotificationListener;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import java.util.Arrays;
import java.util.Comparator;

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ActiveNotificationsAdapter extends NotificationListAdapter {
	private PebbleNotification[] notifications;
	
	public ActiveNotificationsAdapter(PebbleTalkerService service) {
		super(service);
		
		loadNotifications();
	}
	
	private void loadNotifications()
	{
		StatusBarNotification[] sbns = JellybeanNotificationListener.getCurrentNotifications();
        if (sbns == null)
        {
            notifications = new PebbleNotification[0];
            return;
        }
		notifications = new PebbleNotification[sbns.length];
		
		for (int i = 0; i < sbns.length; i++)
		{
			StatusBarNotification sbn = sbns[i];
			Notification notification = sbn.getNotification();

            PebbleNotification pn = NotificationHandler.getPebbleNotificationFromAndroidNotification(service, sbn.getPackageName(), notification, sbn.getId(), sbn.getTag(), sbn.isClearable());
            pn.setListNotification(true);

            notifications[i] = pn;
		}
		
		Arrays.sort(notifications, new NotificationComparable());
	}


	@Override
	public PebbleNotification getNotificationAt(int index) {
		return notifications[index];
	}

	@Override
	public int getNumOfNotifications() {
		return notifications.length;
	}

	@Override
	public void notificationPicked(int index) {
		PebbleNotification pn = notifications[index];

        Intent startIntent = new Intent(service, PebbleTalkerService.class);
        startIntent.putExtra("notification", pn);
        service.startService(startIntent);
	}
	
	private static class NotificationComparable implements Comparator<PebbleNotification>
	{

		@Override
		public int compare(PebbleNotification lhs, PebbleNotification rhs) {
			//First sort by normal/onging, then by date.
			
			if (lhs.isDismissable() != rhs.isDismissable())
			{
				if (lhs.isDismissable())
					return -1;
				else
					return 11;
			}
			
			return (int) (rhs.getPostTime() - lhs.getPostTime());
		}
		
	}
}
