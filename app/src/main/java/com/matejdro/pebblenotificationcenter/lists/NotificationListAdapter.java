package com.matejdro.pebblenotificationcenter.lists;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.pebble.modules.ListModule;
import com.matejdro.pebblecommons.util.TextUtil;
import java.text.DateFormat;
import java.util.Date;
import timber.log.Timber;

public interface NotificationListAdapter {
	public PebbleNotification getNotificationAt(int index);
	public int getNumOfNotifications();
	public void forceRefresh();
}
