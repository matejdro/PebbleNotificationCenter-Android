package com.matejdro.pebblenotificationcenter.notifications;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import com.matejdro.pebblenotificationcenter.notifications.actions.ActionParser;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.TaskerAction;
import com.matejdro.pebblenotificationcenter.util.SettingsMemoryStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import timber.log.Timber;

public class NotificationHandler {
	public static boolean active = false;

	public static void newNotification(Context context, String pack, Notification notification, Integer id, String tag, boolean isDismissible)
	{
		Timber.i("Processing notification from package %s", pack);

		SettingsMemoryStorage settings = PebbleNotificationCenter.getInMemorySettings();
		SharedPreferences preferences = settings.getSharedPreferences();
        AppSettingStorage settingStorage = new SharedPreferencesAppStorage(context, pack, settings.getDefaultSettingsStorage(), true);

		boolean enableOngoing = settingStorage.getBoolean(AppSetting.SEND_ONGOING_NOTIFICATIONS);
		boolean isOngoing = (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
		
		if (isOngoing && !enableOngoing) {
			Timber.d("Discarding notification from %s because FLAG_ONGOING_EVENT is set.", pack);
			return;
		}

		if (!settingStorage.canAppSendNotifications()) {
			Timber.d("Discarding notification from %s because package is not selected", pack);
			return;
		}

		final String title = getAppName(context, pack);

		NotificationParser parser = new NotificationParser(context, pack, notification);

		String secondaryTitle = parser.title;
		String text = parser.text.trim();
		
		if (notification.tickerText != null && (text == null || text.trim().length() == 0)) {
			text = notification.tickerText.toString();
		}
		
		if (!settingStorage.getBoolean(AppSetting.SEND_BLANK_NOTIFICATIONS)) {
			if (text.length() == 0 && (secondaryTitle == null || secondaryTitle.length() == 0)) {
				Timber.d("Discarding notification from %s because it is empty", pack);
				return;
			}
		}

        String combinedText = title + " " + secondaryTitle + " " + text;

        List<String> regexList = settingStorage.getStringList(AppSetting.INCLUDED_REGEX);
        if (regexList.size() > 0 && !containsRegexes(combinedText, regexList))
            return;

        regexList = settingStorage.getStringList(AppSetting.EXCLUDED_REGEX);
        if (containsRegexes(combinedText, regexList))
            return;

        PebbleNotification pebbleNotification = new PebbleNotification(title, text, pack);
        pebbleNotification.setSubtitle(secondaryTitle);
        if (isDismissible)
        {
            pebbleNotification.setDismissable(true);
            pebbleNotification.setAndroidID(id);
            pebbleNotification.setTag(tag);
        }

        ArrayList<NotificationAction> actions = new ArrayList<NotificationAction>();
        TaskerAction.addTaskerTasks(settingStorage, actions);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            if (settingStorage.getBoolean(AppSetting.LOAD_WEAR_ACTIONS))
                ActionParser.parseWearActions(notification, actions);
            ActionParser.parseNativeActions(notification, actions);

            parseWearGroupData(notification, pebbleNotification);
        }
        pebbleNotification.setActions(actions);


        if (notification.contentIntent != null)
                pebbleNotification.setOpenAction(notification.contentIntent);

        Intent startIntent = new Intent(context, PebbleTalkerService.class);
        startIntent.putExtra("notification", pebbleNotification);
        context.startService(startIntent);

    }

    public static void parseWearGroupData(Notification notification, PebbleNotification pebbleNotification)
    {
        Bundle extras = NotificationParser.getExtras(notification);
        if (extras == null)
            return;

        if (!extras.containsKey("android.support.groupKey"))
            return;

        pebbleNotification.setWearGroupKey(extras.getString("android.support.groupKey"));

        if (extras.getBoolean("android.support.isGroupSummary", false))
            pebbleNotification.setWearGroupType(PebbleNotification.WEAR_GROUP_TYPE_GROUP_SUMMARY);
        else
            pebbleNotification.setWearGroupType(PebbleNotification.WEAR_GROUP_TYPE_GROUP_MESSAGE);

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

    public static boolean containsRegexes(String text, List<String> regexes)
    {
        for (String regex : regexes)
        {
            try
            {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(text);
                if (matcher.find())
                    return true;
            }
            catch (PatternSyntaxException e)
            {
            }
        }

        return false;
    }


	public static boolean isNotificationListenerSupported()
	{
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
	}
}
