package com.matejdro.pebblenotificationcenter.lists;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.DataReceiver;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.util.TextUtil;
import java.text.DateFormat;
import java.util.Date;
import timber.log.Timber;

public abstract class NotificationListAdapter {
	protected PebbleTalkerService service;
	private int lastNotification = -1;
	
	public NotificationListAdapter(PebbleTalkerService service)
	{
		this.service = service;
	}
	
	public abstract PebbleNotification getNotificationAt(int index);
	public abstract int getNumOfNotifications();
	public abstract void notificationPicked(int index);
	
	public void gotRequest(PebbleDictionary data)
	{
		int id = data.getUnsignedInteger(1).intValue();
		sendNotification(id);
	}
	
	public void entrySelected(PebbleDictionary data)
	{
		int id = data.getInteger(1).intValue();
		if (id >= getNumOfNotifications())
			return;
		
		lastNotification = id;
		notificationPicked(id);
	}
	
	public void sendRelativeNotification(PebbleDictionary data)
	{
		if (lastNotification < 0)
			return;
		
		int change = data.getInteger(1).intValue();
		int newNotification = lastNotification + change;
		if (newNotification < 0 || newNotification >= getNumOfNotifications())
			return;
		
		lastNotification = newNotification;
		notificationPicked(newNotification);
	}
	
	public void sendNotification(int index)
	{
		PebbleDictionary data = new PebbleDictionary();

		if (index >= getNumOfNotifications())
		{
			data.addUint8(0, (byte) 2);
			data.addUint16(1, (short) 0);
			data.addUint16(2, (short) 1);
			data.addUint8(3, (byte) 1);
			data.addString(4, "No notifications");
			data.addString(5, "");
			data.addString(6, "");
			
			PebbleKit.sendDataToPebble(service, DataReceiver.pebbleAppUUID, data);

			return;
		}
		
		PebbleNotification notification = getNotificationAt(index);

		data.addUint8(0, (byte) 2);
		data.addUint16(1, (short) index);
		data.addUint16(2, (short) getNumOfNotifications());
		data.addUint8(3, (byte) (notification.isDismissable() ? 0 : 1));
		data.addString(4, TextUtil.prepareString(notification.getTitle()));
		data.addString(5, TextUtil.prepareString(notification.getSubtitle()));
		data.addString(6, getFormattedDate(notification.getPostTime()));

		Timber.i("Sending notification " + index + " " + data.getString(4));
		
		PebbleKit.sendDataToPebble(service, DataReceiver.pebbleAppUUID, data);
	}
	
	public String getFormattedDate(long date)
	{
		DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(service);
		DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(service);
		Date dateO = new Date(date);

		String dateS = dateFormat.format(dateO) + " " + timeFormat.format(dateO);

		return TextUtil.trimString(dateS);
	}
}
