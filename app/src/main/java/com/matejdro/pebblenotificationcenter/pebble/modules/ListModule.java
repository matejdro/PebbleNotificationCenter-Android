package com.matejdro.pebblenotificationcenter.pebble.modules;

import android.content.Context;
import android.graphics.Bitmap;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblecommons.pebble.CommModule;
import com.matejdro.pebblecommons.pebble.PebbleCapabilities;
import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebblecommons.pebble.PebbleUtil;
import com.matejdro.pebblecommons.util.TextUtil;
import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.lists.ActiveNotificationsAdapter;
import com.matejdro.pebblenotificationcenter.lists.NotificationHistoryAdapter;
import com.matejdro.pebblenotificationcenter.lists.NotificationListAdapter;

import java.text.DateFormat;
import java.util.Date;

import timber.log.Timber;

/**
 * Created by Matej on 3.12.2014.
 */
public class ListModule extends CommModule
{
    public static final int MODULE_LIST = 2;

    private NotificationListAdapter listAdapter;

    private int sendNotification = -1;
    private int lastSentNotification = -1;

    private int nextListItemToSend = 0;
    private boolean openListWindow = false;

    public ListModule(PebbleTalkerService service)
    {
        super(service);
    }

    @Override
    public boolean sendNextMessage()
    {
        if (listAdapter == null)
            return false;

        if (sendNotification > -1 )
        {
            int localSendNotification = sendNotification;
            sendNotification = -1;

            if (localSendNotification < listAdapter.getNumOfNotifications())
            {
                lastSentNotification = localSendNotification;
                NotificationSendingModule.notify(listAdapter.getNotificationAt(localSendNotification), getService());
                return true;
            }
        }

        if (nextListItemToSend < 0)
            return false;

        sendListItem(nextListItemToSend);

        nextListItemToSend = -1;
        openListWindow = false;

        return true;
    }

    public void gotMessageListItemRequest(PebbleDictionary data)
    {
        int id = data.getUnsignedIntegerAsLong(2).intValue();
        nextListItemToSend = id;

        boolean forceListRefresh = data.getUnsignedIntegerAsLong(3).intValue() == 1;
        if (forceListRefresh && listAdapter != null)
            listAdapter.forceRefresh();

        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.sendNext();
    }

    public void gotMessageNotificationRequest(PebbleDictionary data)
    {
        int id = data.getUnsignedIntegerAsLong(2).intValue();
        if (id >= listAdapter.getNumOfNotifications())
            return;

        sendNotification = id;

        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.sendNext();
    }

    public void gotMessageSendRelativeNotification(PebbleDictionary data)
    {
        if (lastSentNotification < 0)
        {
            SystemModule.get(getService()).hideHourglass();
            return;
        }

        int change = data.getInteger(2).intValue();
        int newNotification = lastSentNotification + change;
        if (newNotification < 0 || newNotification >= listAdapter.getNumOfNotifications())
        {
            SystemModule.get(getService()).hideHourglass();
            return;
        }

        sendNotification = newNotification;

        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.sendNext();
    }

    public void sendListItem(int index)
    {
        PebbleDictionary data = new PebbleDictionary();

        if (index >= listAdapter.getNumOfNotifications())
        {
            data.addUint8(0, (byte) 2);
            data.addUint8(1, (byte) 0);
            data.addUint16(2, (short) 0);
            data.addUint16(3, (short) 1);
            data.addUint8(4, (byte) 1);
            data.addString(5, "No notifications");
            data.addString(6, "");
            data.addString(7, "");
            data.addUint16(8, (short) 0);

            if (openListWindow)
                data.addUint8(999, (byte) 1);

            getService().getPebbleCommunication().sendToPebble(data);

            return;
        }

        PebbleNotification notification = listAdapter.getNotificationAt(index);

        data.addUint8(0, (byte) 2);
        data.addUint8(1, (byte) 0);
        data.addUint16(2, (short) index);
        data.addUint16(3, (short) listAdapter.getNumOfNotifications());
        data.addUint8(4, (byte) (notification.isDismissable() ? 0 : 1));
        data.addString(5, TextUtil.prepareString(notification.getTitle()));
        data.addString(6, TextUtil.prepareString(notification.getSubtitle()));
        data.addString(7, getFormattedDate(getService(), notification.getRawPostTime()));
        data.addUint16(8, (short) 0); // Placeholder

        if (openListWindow)
            data.addUint8(999, (byte) 1);

        Bitmap icon = notification.getNotificationIcon();
        if (icon != null)
        {
            PebbleCapabilities connectedWatchCapabilities = getService().getPebbleCommunication().getConnectedWatchCapabilities();
            byte[] iconData = ImageSendingModule.prepareIcon(icon, getService(), connectedWatchCapabilities);

            // This feature requires lots of memory on the watch to contain lots of icons for every list item
            // To weed out low memory devices, a device must be able to afford to receive at least 2048 bytes of the appmessage.
            int minAppmessageBufferSize = Math.max(2048, iconData.length);

            if (PebbleUtil.getBytesLeft(data, connectedWatchCapabilities) >= minAppmessageBufferSize)
            {
                data.addUint16(8, (short) iconData.length); // Placeholder
                data.addBytes(9, iconData);
            }
        }

        Timber.i("Sending list entry %d %s", index, data.getString(5));

        getService().getPebbleCommunication().sendToPebble(data);
    }

    public static String getFormattedDate(Context context, long date)
    {
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        Date dateO = new Date(date);

        String dateS = dateFormat.format(dateO) + " " + timeFormat.format(dateO);

        return TextUtil.trimString(dateS);
    }


    @Override
    public void gotMessageFromPebble(PebbleDictionary message)
    {
        if (listAdapter == null)
            return;

        int id = message.getUnsignedIntegerAsLong(1).intValue();
        switch (id)
        {
            case 0:
                gotMessageListItemRequest(message);
                break;
            case 1:
                gotMessageNotificationRequest(message);
                break;
            case 2:
                gotMessageSendRelativeNotification(message);
                break;
        }
    }

    public void showList(int id)
    {
        switch(id)
        {
            case 0:
                listAdapter = new ActiveNotificationsAdapter(getService());
                break;
            case 1:
                listAdapter = new NotificationHistoryAdapter(getService(), NCTalkerService.fromPebbleTalkerService(getService()).getHistoryDatabase());
                break;
            default:
                return;
        }

        nextListItemToSend = 0;
        openListWindow = true;

        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.sendNext();
    }

    public static ListModule get(PebbleTalkerService service)
    {
        return (ListModule) service.getModule(MODULE_LIST);
    }
}
