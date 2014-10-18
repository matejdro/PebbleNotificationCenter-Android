package com.matejdro.pebblenotificationcenter.notifications;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import com.matejdro.pebblenotificationcenter.NotificationKey;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import timber.log.Timber;

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class JellybeanNotificationListener extends NotificationListenerService {
	private Handler handler;
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

		handler = new Handler();
		instance = this;

		NotificationHandler.active = true;

        Timber.d("Finished creating Notification Listener...");



        super.onCreate();
	}

	@Override
	public void onNotificationPosted(final StatusBarNotification sbn) {
        handler.post(new Runnable() {

			@Override
			public void run() {
				NotificationHandler.newNotification(JellybeanNotificationListener.this, NotificationHandler.getKeyFromSbn(sbn), sbn.getNotification(), true);
			}
		});
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
        NotificationKey key = NotificationHandler.getKeyFromSbn(sbn);

        Intent intent = new Intent(this, PebbleTalkerService.class);
        intent.putExtra("dismissUpwardsKey", key);
        startService(intent);
	}

	public static void dismissNotification(String pkg, String tag, int id)
	{
        Timber.d("dismissing " + pkg + " " + tag + " " + id + " " + (instance != null));

        if (instance != null)
		    instance.cancelNotification(pkg, tag, id);
	}

    @TargetApi(value = Build.VERSION_CODES.LOLLIPOP)
    public static void dismissNotification(NotificationKey key)
    {
        Timber.d("dismissing " + key + " " + (instance != null));

        if (instance == null)
            return;

        System.out.println(key.getLolipopKey());

        if (key.getLolipopKey() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            instance.cancelNotification(key.getLolipopKey());
        else
            instance.cancelNotification(key.getPackage(), key.getTag(), key.getAndroidId());
    }

	public static StatusBarNotification[] getCurrentNotifications()
	{
		if (instance == null)
			return new StatusBarNotification[0];
		
		return instance.getActiveNotifications();
	}
}
