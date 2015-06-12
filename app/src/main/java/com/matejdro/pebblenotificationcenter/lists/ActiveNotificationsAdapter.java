package com.matejdro.pebblenotificationcenter.lists;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.notifications.JellybeanNotificationListener;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;
import java.util.Arrays;
import java.util.Comparator;

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ActiveNotificationsAdapter implements NotificationListAdapter {
	private PebbleNotification[] notifications;
	
	public ActiveNotificationsAdapter(Context context) {
		loadNotifications(context);
	}
	
	private void loadNotifications(Context context)
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

            PebbleNotification pn = NotificationHandler.getPebbleNotificationFromAndroidNotification(context, NotificationHandler.getKeyFromSbn(sbn), notification, sbn.isClearable());
            pn.setListNotification(true);
            pn.setPostTime(sbn.getPostTime());

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
