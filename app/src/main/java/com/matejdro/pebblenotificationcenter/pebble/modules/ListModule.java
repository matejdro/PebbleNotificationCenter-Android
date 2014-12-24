package com.matejdro.pebblenotificationcenter.pebble.modules;

import android.content.Context;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.lists.ActiveNotificationsAdapter;
import com.matejdro.pebblenotificationcenter.lists.NotificationHistoryAdapter;
import com.matejdro.pebblenotificationcenter.lists.NotificationListAdapter;
import com.matejdro.pebblenotificationcenter.pebble.PebbleCommunication;
import com.matejdro.pebblenotificationcenter.util.TextUtil;
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
        data.addString(7, getFormattedDate(getService(), notification.getPostTime()));
        if (openListWindow)
            data.addUint8(999, (byte) 1);

        Timber.i("Sending list entry " + index + " " + data.getString(5));

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
                listAdapter = new NotificationHistoryAdapter(getService(), getService().getHistoryDatabase());
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
