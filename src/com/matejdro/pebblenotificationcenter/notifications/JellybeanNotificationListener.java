package com.matejdro.pebblenotificationcenter.notifications;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import com.matejdro.pebblenotificationcenter.NotificationKey;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.pebble.modules.DismissUpwardsModule;
import timber.log.Timber;

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class JellybeanNotificationListener extends NotificationListenerService {
	public static JellybeanNotificationListener instance;
	
	@Override
	public void onDestroy() {
		NotificationHandler.active = false;

        Timber.d("Notification Listener stopped...");

        instance = null;
	}

	@Override
	public void onCreate() {
        Timber.d("Creating Notification Listener...");

		instance = this;

		NotificationHandler.active = true;

        Timber.d("Finished creating Notification Listener...");



        super.onCreate();
	}

	@Override
	public void onNotificationPosted(final StatusBarNotification sbn) {
        Timber.d("Got new jellybean notification");
        NotificationHandler.newNotification(JellybeanNotificationListener.this, NotificationHandler.getKeyFromSbn(sbn), sbn.getNotification(), true);
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
        NotificationKey key = NotificationHandler.getKeyFromSbn(sbn);
        Timber.d("Got jellybean dismiss " + key);


        DismissUpwardsModule.dismissNotification(this, key);
	}

    @TargetApi(value = Build.VERSION_CODES.LOLLIPOP)
    public static boolean isNotificationFilteredByDoNotInterrupt(NotificationKey key)
    {
        if (instance == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || key.getLolipopKey() == null)
            return false;

        RankingMap rankingMap = instance.getCurrentRanking();
        Ranking ranking = new Ranking();
        if (!rankingMap.getRanking(key.getLolipopKey(), ranking))
            return false;

        return !ranking.matchesInterruptionFilter();
    }


    @TargetApi(value = Build.VERSION_CODES.LOLLIPOP)
    public static void dismissNotification(NotificationKey key)
    {
        Timber.d("dismissing from phone " + key + " " + (instance != null));

        if (instance == null)
            return;

        if (key.getLolipopKey() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            instance.cancelNotification(key.getLolipopKey());
        else
            //noinspection deprecation
            instance.cancelNotification(key.getPackage(), key.getTag(), key.getAndroidId());
    }

	public static StatusBarNotification[] getCurrentNotifications()
	{
		if (instance == null)
			return new StatusBarNotification[0];
		
		return instance.getActiveNotifications();
	}
}
