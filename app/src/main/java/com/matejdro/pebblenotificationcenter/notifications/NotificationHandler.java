package com.matejdro.pebblenotificationcenter.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.matejdro.pebblecommons.notification.NotificationCenterExtender;
import com.matejdro.pebblenotificationcenter.NotificationKey;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import com.matejdro.pebblenotificationcenter.notifications.actions.ActionParser;
import com.matejdro.pebblenotificationcenter.pebble.NativeNotificationIcon;
import com.matejdro.pebblenotificationcenter.pebble.modules.ImageSendingModule;
import com.matejdro.pebblenotificationcenter.pebble.modules.NotificationSendingModule;
import com.matejdro.pebblenotificationcenter.util.SettingsMemoryStorage;

import timber.log.Timber;

public class NotificationHandler {
	public static boolean active = false;

	public static void newNotification(Context context, NotificationKey key, Notification notification, boolean isDismissible)
	{
		Timber.i("Processing notification %s", key);

		SettingsMemoryStorage settings = PebbleNotificationCenter.getInMemorySettings();
        AppSettingStorage settingStorage = new SharedPreferencesAppStorage(context, key.getPackage(), settings.getDefaultSettingsStorage());

		boolean enableOngoing = settingStorage.getBoolean(AppSetting.SEND_ONGOING_NOTIFICATIONS);
		boolean isOngoing = (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
		
		if (isOngoing && !enableOngoing) {
			Timber.d("Discarding notification because FLAG_ONGOING_EVENT is set.");
			return;
		}

		if (!settingStorage.canAppSendNotifications()) {
			Timber.d("Discarding notification because package is not selected");
			return;
		}

        //Respect LocalOnly on NC notifications regardless of the settings
        boolean localNotification = NotificationCompat.getLocalOnly(notification);
        if (localNotification &&
           (PebbleNotificationCenter.PACKAGE.equals(key.getPackage()) || settingStorage.getBoolean(AppSetting.DISABLE_LOCAL_ONLY_NOTIFICATIONS)))
        {
            Timber.d("Discarding notification because it is local only");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && settingStorage.getInt(AppSetting.MINIMUM_NOTIFICATION_PRIORITY) > notification.priority) {
            Timber.d("Discarding notification because its priority is too low!");
            return;
        }

        PebbleNotification pebbleNotification = getPebbleNotificationFromAndroidNotification(context, key, notification, isDismissible);
        if (pebbleNotification == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            parseWearGroupData(notification, pebbleNotification);
        }

        NotificationSendingModule.notify(pebbleNotification, context);
    }

    public static @Nullable PebbleNotification getPebbleNotificationFromAndroidNotification(Context context, NotificationKey key, Notification notification, boolean isDismissible)
    {
        NotificationCenterExtender notificationCenterExtender = new NotificationCenterExtender(notification);
        if (notificationCenterExtender.isNCNotificationDisabled())
        {
            Timber.d("Discarding notification because extender requested it");
            return null;
        }

        final String title = getAppName(context, key.getPackage());

        PebbleNotification pebbleNotification = new PebbleNotification(title, null, key);
        AppSettingStorage settingStorage = pebbleNotification.getSettingStorage(context);

        NotificationParser parser = new NotificationParser(context, pebbleNotification, notification);

        String secondaryTitle = parser.title;
        String text = parser.text.trim();

        if (notification.tickerText != null && text.length() == 0 && (secondaryTitle == null  || secondaryTitle.trim().length() == 0)) {
            text = notification.tickerText.toString();
        }

        pebbleNotification.setText(text);
        pebbleNotification.setSubtitle(secondaryTitle);
        pebbleNotification.setDismissable(isDismissible);
        pebbleNotification.setColor(getColor(notification, key.getPackage(), context));

        if (key.getPackage() != null)
            pebbleNotification.setNativeNotificationIcon(NativeNotificationIcon.getIconForApplication(key.getPackage(), title));

        if (settingStorage.getBoolean(AppSetting.SHOW_IMAGE))
            pebbleNotification.setPebbleImage(ImageSendingModule.prepareImage(getImage(notification)));

        if (settingStorage.getBoolean(AppSetting.USE_PROVIDED_VIBRATION))
            pebbleNotification.setForcedVibrationPattern(notification.vibrate);

        ActionParser.loadActions(notification, pebbleNotification, context);

        return pebbleNotification;
    }

    @TargetApi(value = Build.VERSION_CODES.LOLLIPOP)
    public static NotificationKey getKeyFromSbn(StatusBarNotification notification)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return new NotificationKey(notification.getKey());
        else
            return new NotificationKey(notification.getPackageName(), notification.getId(), notification.getTag());
    }

    public static void parseWearGroupData(Notification notification, PebbleNotification pebbleNotification)
    {
        Bundle extras = NotificationParser.getExtras(notification);
        if (extras == null)
            return;

        String groupKey = NotificationCompat.getGroup(notification);
        boolean summary = NotificationCompat.isGroupSummary(notification);
        boolean hasPages = hasPages(extras);

        Timber.d("wear group: %b %b %s", summary, hasPages, groupKey);

        if (groupKey == null)
            return;

        if (summary && hasPages)
            return;

        pebbleNotification.setWearGroupKey(groupKey);

        if (summary)
        {
            pebbleNotification.setWearGroupType(PebbleNotification.WEAR_GROUP_TYPE_GROUP_SUMMARY);
        }
        else
            pebbleNotification.setWearGroupType(PebbleNotification.WEAR_GROUP_TYPE_GROUP_MESSAGE);
    }

    public static boolean hasPages(Bundle extras)
    {
        if (extras == null || !extras.containsKey("android.wearable.EXTENSIONS"))
            return false;

        Bundle wearables = extras.getBundle("android.wearable.EXTENSIONS");

        if (!wearables.containsKey("pages"))
            return false;

        Parcelable[] pages = wearables.getParcelableArray("pages");
        if (pages.length < 1)
            return false;

        return true;
    }

    public static int getColor(Notification notification, String appPackage, Context context)
    {
        //Try getting color from Notification#color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            int color = notification.color;
            if (color != Notification.COLOR_DEFAULT)
                return color;
        }

        //Try getting color from Wearable and Car extensions
        Bundle extras = NotificationParser.getExtras(notification);
        if (extras != null)
        {
            if (extras.containsKey("android.wearable.EXTENSIONS"))
            {
                Bundle wearableExtension = extras.getBundle("android.wearable.EXTENSIONS");
                if (wearableExtension.containsKey("app_color"))
                    return wearableExtension.getInt("app_color");
            }

            if (extras.containsKey("android.car.EXTENSIONS"))
            {
                Bundle carExtension = extras.getBundle("android.car.EXTENSIONS");
                if (carExtension.containsKey("app_color"))
                    return carExtension.getInt("app_color");
            }
        }

        //Try getting color from app theme (material design primary color)
        if (appPackage != null)
        {
            PackageManager packageManager = context.getPackageManager();
            try
            {
                Resources otherAppResources = packageManager.getResourcesForApplication(appPackage);
                Resources.Theme theme = otherAppResources.newTheme();

                int themeResId = packageManager.getApplicationInfo(appPackage, 0).theme;
                if (themeResId == 0)
                {
                    Intent launchIntent =  packageManager.getLaunchIntentForPackage(appPackage);
                    themeResId = launchIntent != null ? packageManager.getActivityInfo(launchIntent.getComponent(), 0).theme : 0;
                }

                if (themeResId != 0)
                {
                    theme.applyStyle(themeResId, false);

                    //AppCompat theme color
                    TypedArray typedArray = theme.obtainStyledAttributes(new int[] {otherAppResources.getIdentifier("colorPrimary", "attr", appPackage)});
                    int color = typedArray.getColor(0, Color.TRANSPARENT);
                    typedArray.recycle();

                    if (color == Color.TRANSPARENT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    {
                        //Native Lollipop theme color
                        typedArray = theme.obtainStyledAttributes(new int[] {android.R.attr.colorPrimary});
                        color = typedArray.getColor(0, Color.TRANSPARENT);
                        typedArray.recycle();
                    }

                    if (color != Color.TRANSPARENT)
                        return color;
                }
            }
            catch (NameNotFoundException ignored)
            {
            }
        }

        //Try getting color from notification LED color
        int ledColor = notification.ledARGB;
        if (ledColor != Notification.COLOR_DEFAULT)
            return ledColor;

        return Color.TRANSPARENT;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Bitmap getImage(Notification notification)
    {
        Bundle extras = NotificationParser.getExtras(notification);
        if (extras != null)
        {
            //Extract image from BigPictureStyle notification style
            Bitmap bitmap = extras.getParcelable(Notification.EXTRA_PICTURE);
            if (bitmap != null)
                return bitmap;

            //Extract image from Wearable extender background
            if (extras.containsKey("android.wearable.EXTENSIONS"))
            {
                Bundle wearableExtension = extras.getBundle("android.wearable.EXTENSIONS");
                bitmap = wearableExtension.getParcelable("background");
                if (bitmap != null)
                    return bitmap;
            }

            //Extract image from Car extender large icon
            if (extras.containsKey("android.car.EXTENSIONS"))
            {
                Bundle carExtensions = extras.getBundle("android.car.EXTENSIONS");
                bitmap = carExtensions.getParcelable("large_icon");
                if (bitmap != null)
                    return bitmap;
            }

            //Extract image from large icon on android notification
            bitmap = extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG);
            if (bitmap != null)
                return bitmap;

            bitmap = extras.getParcelable(Notification.EXTRA_LARGE_ICON);
            if (bitmap != null)
                return bitmap;
        }

        return  null;
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
