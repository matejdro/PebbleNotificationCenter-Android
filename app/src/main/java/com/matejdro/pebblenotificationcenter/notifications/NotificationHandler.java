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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Target;

import com.matejdro.pebblecommons.notification.NotificationCenterExtender;
import com.matejdro.pebblecommons.util.BitmapUtils;
import com.matejdro.pebblenotificationcenter.NotificationKey;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleNotificationCenter;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.appsetting.SharedPreferencesAppStorage;
import com.matejdro.pebblenotificationcenter.notifications.actions.ActionParser;
import com.matejdro.pebblenotificationcenter.pebble.NativeNotificationIcon;
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

        NotificationTextParser parser = new NotificationTextParser(context, pebbleNotification, notification);

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
            pebbleNotification.setBigNotificationImage(getImage(context, notification));

        if (settingStorage.getBoolean(AppSetting.USE_PROVIDED_VIBRATION))
            pebbleNotification.setForcedVibrationPattern(notification.vibrate);

        if (settingStorage.getBoolean(AppSetting.WATCHAPP_NOTIFICATION_ICON))
            pebbleNotification.setNotificationIcon(getNotificationIcon(key.getPackage(), notification, context));

        ActionParser.loadActions(notification, pebbleNotification, context);

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        pebbleNotification.setScrollToEnd(wearableExtender.getStartScrollBottom());

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
        Bundle extras = NotificationTextParser.getExtras(notification);
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
        try {
            //Try getting color from Wearable and Car extensions
            Bundle extras = NotificationTextParser.getExtras(notification);
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
        } catch (RuntimeException e) {
            // Unpacking the bundle may cause crashes on some devices.
        }

        //Try getting color from notification icon color
        try {
            Drawable appIcon = context.getPackageManager().getApplicationIcon(appPackage);
            Bitmap iconBitmap = BitmapUtils.getBitmap(appIcon);
            if (iconBitmap != null)
            {
                Palette palette = Palette.from(iconBitmap).addTarget(Target.VIBRANT).generate();
                return palette.getColorForTarget(Target.VIBRANT, Color.TRANSPARENT);
            }
        } catch (NameNotFoundException ignored) {
        }


        return Color.TRANSPARENT;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Bitmap getImage(Context context, Notification notification)
    {
        Bundle extras = NotificationTextParser.getExtras(notification);
        if (extras != null)
        {
            //Extract image from BigPictureStyle notification style
            Bitmap bitmap = BitmapUtils.getBitmap(context, extras.getParcelable(Notification.EXTRA_PICTURE));
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
                bitmap = BitmapUtils.getBitmap(context, carExtensions.getParcelable("large_icon"));
                if (bitmap != null)
                    return bitmap;
            }

            //Extract image from large icon on android notification
            bitmap = BitmapUtils.getBitmap(context, extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG));
            if (bitmap != null)
                return bitmap;

            bitmap = BitmapUtils.getBitmap(context, extras.getParcelable(Notification.EXTRA_LARGE_ICON));
            if (bitmap != null)
                return bitmap;
        }

        return  null;
    }

    @Nullable
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.M)
    public static Bitmap getNotificationIcon(String packageName, Notification notification, Context context)
    {
        Drawable iconDrawable = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            Icon icon = notification.getSmallIcon();
            if (icon == null)
                return null;

            iconDrawable = icon.loadDrawable(context);
        }
        else
        {
            if (packageName == null)
                return null;

            int iconId = notification.icon;
            try
            {
                Resources sourceAppResources = context.getPackageManager().getResourcesForApplication(packageName);
                iconDrawable = sourceAppResources.getDrawable(iconId);
            }
            catch (NameNotFoundException e)
            {
                return null;
            }
            catch (Resources.NotFoundException e)
            {
                return null;
            }
        }

        if (iconDrawable != null)
            return BitmapUtils.getBitmap(iconDrawable);

        return null;
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
