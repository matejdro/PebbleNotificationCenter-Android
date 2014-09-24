package com.matejdro.pebblenotificationcenter;

import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import com.matejdro.pebblenotificationcenter.notifications.JellybeanNotificationListener;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.notifications.NotificationParser;

import com.matejdro.pebblenotificationcenter.notifications.actions.ActionParser;
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
			NotificationParser parser = new NotificationParser(service,sbn.getPackageName(),  notification);

			PebbleNotification pn = new PebbleNotification(NotificationHandler.getAppName(service, sbn.getPackageName()), parser.text, sbn.getPackageName());
            pn.setAndroidID(sbn.getId());
            pn.setDismissable(sbn.isClearable());
            pn.setSubtitle(parser.title);
            pn.setPostTime(sbn.getPostTime());
            pn.setTag(sbn.getTag());
            pn.setListNotification(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            {
                pn.setActions(ActionParser.getActions(notification));
            }
            if (notification.contentIntent != null)
                pn.setOpenAction(notification.contentIntent);

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
		
		PebbleTalkerService.notify(service, pn);
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
