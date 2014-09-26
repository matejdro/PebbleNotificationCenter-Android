package com.matejdro.pebblenotificationcenter.lists;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.notifications.JellybeanNotificationListener;
import com.matejdro.pebblenotificationcenter.notifications.NotificationHandler;
import com.matejdro.pebblenotificationcenter.notifications.NotificationParser;
import com.matejdro.pebblenotificationcenter.notifications.actions.ActionParser;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import java.util.ArrayList;
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

            AppSettingStorage settingStorage = pn.getSettingStorage(service);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && settingStorage.getBoolean(AppSetting.ACTIONS_SHOW_MENU))
            {
                ArrayList<NotificationAction> actions = new ArrayList<NotificationAction>();

                ActionParser.parseNativeActions(notification, actions);
                if (settingStorage.getBoolean(AppSetting.LOAD_WEAR_ACTIONS))
                    ActionParser.parseWearActions(notification, actions);

                pn.setActions(actions);
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
