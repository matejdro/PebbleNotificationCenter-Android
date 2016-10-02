package com.matejdro.pebblenotificationcenter.lists;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.notifications.JellybeanNotificationListener;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ActiveNotificationsAdapter implements NotificationListAdapter {
	private List<PebbleNotification> pebbleNotifications;
	private Context context;
	
	public ActiveNotificationsAdapter(Context context) {
		this.context = context;

		loadNotifications(context);
	}
	
	private void loadNotifications(Context context)
	{
		pebbleNotifications = new ArrayList<>();

		StatusBarNotification[] sbns = JellybeanNotificationListener.getCurrentNotifications();
        if (sbns == null)
            return;

		for (StatusBarNotification sbn : sbns)
		{
			Notification notification = sbn.getNotification();

			PebbleNotification pn = NotificationHandler.getPebbleNotificationFromAndroidNotification(context, NotificationHandler.getKeyFromSbn(sbn), notification, sbn.isClearable());
			if (pn == null)
				continue;

			pn.setListNotification(true);
			pn.setPostTime(sbn.getPostTime());

			pebbleNotifications.add(pn);
		}
		
		Collections.sort(pebbleNotifications, new NotificationComparable());
	}


	@Override
	public PebbleNotification getNotificationAt(int index) {
		return pebbleNotifications.get(index);
	}

	@Override
	public int getNumOfNotifications() {
		return pebbleNotifications.size();
	}

	@Override
	public void forceRefresh() {
		loadNotifications(context);
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
