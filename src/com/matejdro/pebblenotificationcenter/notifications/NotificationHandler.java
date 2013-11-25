package com.matejdro.pebblenotificationcenter.notifications;

import java.util.Iterator;
import java.util.regex.Pattern;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;

import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.util.ListSerialization;

public class NotificationHandler {
	public static boolean active = false;

	public static void newNotification(Context context, String pack, Notification notification, Integer id, String tag, boolean isDismissable)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		boolean enableOngoing = preferences.getBoolean("enableOngoing", false);

		boolean isOngoing = (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
		
		if (isOngoing && !enableOngoing)
			return;


		boolean includingMode = preferences.getBoolean("includingMode", false);
		boolean notificationExist = ListSerialization.listContains(preferences, "CheckedApps", pack);

		if (includingMode != notificationExist)
		{
			return;
		}

		final String title = getAppName(context, pack);

		NotificationParser parser = new NotificationParser(context, notification);

		String secondaryTitle = parser.title;
		String text = parser.text.trim();
		
		if (notification.tickerText != null && (text == null || text.trim().length() == 0))
		{
			text = notification.tickerText.toString();
		}
		
		if (!preferences.getBoolean("sendBlank", false))
		{
			if (text.length() == 0 && (secondaryTitle == null || secondaryTitle.length() == 0))
				return;
		}
		
		Iterator<String> blacklistRegexes = ListSerialization.getDirectIterator(preferences, "BlacklistRegexes");
		while (blacklistRegexes.hasNext())
		{
			String regex = blacklistRegexes.next();
			Pattern pattern = Pattern.compile(regex);
			
			if (pattern.matcher(title).find() || (secondaryTitle != null && pattern.matcher(secondaryTitle).find()) || pattern.matcher(text).find())
			{
				return;
			}
		}
		
		if (isDismissable)
			PebbleTalkerService.notify(context, id, pack, tag, title, secondaryTitle, text, !isOngoing);
		else
			PebbleTalkerService.notify(context, title, secondaryTitle, text);
	}
	
	public static void notificationDismissedOnPhone(Context context, String pkg, String tag, int id)
	{		
		PebbleTalkerService.dismissOnPebble(id, pkg, tag);
	}

	public static String getAppName(Context context, String packageName)
	{
		final PackageManager pm = context.getPackageManager();
		ApplicationInfo ai;
		try {
			ai = pm.getApplicationInfo( packageName, 0);
		} catch (final NameNotFoundException e) {
			ai = null;
		}
		final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "Notification");
		return applicationName;

	}
	
	public static boolean isNotificationListenerSupported()
	{
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
	}
}
