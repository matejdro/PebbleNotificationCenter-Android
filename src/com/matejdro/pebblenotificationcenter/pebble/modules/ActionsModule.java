package com.matejdro.pebblenotificationcenter.pebble.modules;

import android.content.Context;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.PebbleNotification;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.lists.ActiveNotificationsAdapter;
import com.matejdro.pebblenotificationcenter.lists.NotificationHistoryAdapter;
import com.matejdro.pebblenotificationcenter.lists.NotificationListAdapter;
import com.matejdro.pebblenotificationcenter.lists.actions.ActionList;
import com.matejdro.pebblenotificationcenter.lists.actions.NotificationActionList;
import com.matejdro.pebblenotificationcenter.notifications.actions.ActionParser;
import com.matejdro.pebblenotificationcenter.notifications.actions.DismissOnPhoneAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import com.matejdro.pebblenotificationcenter.pebble.PebbleCommunication;
import com.matejdro.pebblenotificationcenter.util.TextUtil;
import java.text.DateFormat;
import java.util.Date;
import timber.log.Timber;

/**
 * Created by Matej on 3.12.2014.
 */
public class ActionsModule extends CommModule
{
    public static final int MODULE_ACTIONS = 4;

    private ActionList list;
    private ProcessedNotification notification;
    int listSize = -1;
    private int nextListItemToSend = -1;

    public ActionsModule(PebbleTalkerService service)
    {
        super(service);
    }

    private void sendNextListItems()
    {
        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 4);
        data.addUint8(1, (byte) 0);

        int segmentSize = Math.min(listSize - nextListItemToSend, 4);

        data.addUint8(2, (byte) nextListItemToSend);

        byte[] textData = new byte[segmentSize * 19];

        for (int i = 0; i < segmentSize; i++)
        {
            String text = TextUtil.prepareString(list.getItem(i + nextListItemToSend), 18);
            System.arraycopy(text.getBytes(), 0, textData, i * 19, text.getBytes().length);

            textData[19 * (i + 1) -1 ] = 0;
        }

        data.addBytes(3, textData);

        getService().getPebbleCommunication().sendToPebble(data);

        nextListItemToSend += 4;
        if (nextListItemToSend >= listSize)
            nextListItemToSend = -1;
    }

    @Override
    public boolean sendNextMessage()
    {
        if (list != null && nextListItemToSend >= 0 )
        {
            sendNextListItems();
            return true;
        }

        return false;
    }

    public void showList(ActionList list)
    {
        this.list = list;
        listSize = Math.min(list.getNumberOfItems(), NotificationAction.MAX_NUMBER_OF_ACTIONS);
        nextListItemToSend = 0;

        PebbleCommunication communication = getService().getPebbleCommunication();
        communication.queueModulePriority(this);
        communication.sendNext();
    }

    private void gotMessageSelectPressed(PebbleDictionary data)
    {
        int id = data.getInteger(2).intValue();
        boolean hold = data.contains(3);

        Timber.d("Select button pressed on Pebble, Hold: " + hold);

        ProcessedNotification notification = getService().sentNotifications.get(id);
        if (notification == null)
        {
            Timber.d("Invalid notification!");
            SystemModule.get(getService()).hideHourglass();
            return;
        }

        if (notification.source.getActions() == null || notification.source.getActions().size() == 0)
        {
            DismissOnPhoneAction.dismissOnPhone(notification, getService());
            return;
        }

        AppSetting relevantSetting = hold ? AppSetting.SELECT_HOLD_ACTION : AppSetting.SELECT_PRESS_ACTION;
        AppSettingStorage settingStorage = notification.source.getSettingStorage(getService());

        int action = settingStorage.getInt(relevantSetting);

        if (notification.source.shouldForceActionMenu() || action == 2)
        {
            this.notification = notification;
            showList(new NotificationActionList(notification));
        }
        else if (action == 0)
        {
            Timber.d("Action was set to none");
            SystemModule.get(getService()).hideHourglass();
            return;
        }
        else if (action == 1)
        {
            DismissOnPhoneAction.dismissOnPhone(notification, getService());
        }
        else
        {
            action -= 3;
            if (notification.source.getActions().size() <= action)
                return;

            SystemModule.get(getService()).hideHourglass();
            notification.source.getActions().get(action).executeAction(getService(), notification);
        }
    }

    public void gotMessageActionItemPicked(PebbleDictionary message)
    {
        int action = message.getUnsignedIntegerAsLong(2).intValue();
        Timber.d("Action picked message " + action);

        if (action >= list.getNumberOfItems())
        {
            Timber.w("Action is higher than list has items!");
            SystemModule.get(getService()).hideHourglass();
            return;
        }

        if (!list.itemPicked(getService(), action));
            SystemModule.get(getService()).hideHourglass();
    }

    private void gotMessageDismissNotification(PebbleDictionary data)
    {
        int id = data.getInteger(2).intValue();

        Timber.d("Got dismiss request from Pebble");

        ProcessedNotification notification = getService().sentNotifications.get(id);
        if (notification == null)
        {
            Timber.d("Invalid notification!");
            SystemModule.get(getService()).hideHourglass();
            return;
        }

        DismissOnPhoneAction.dismissOnPhone(notification, getService());
    }

    @Override
    public void gotMessageFromPebble(PebbleDictionary message)
    {
        int id = message.getUnsignedIntegerAsLong(1).intValue();
        switch (id)
        {
            case 0:
                gotMessageSelectPressed(message);
                break;
            case 1:
                gotMessageDismissNotification(message);
                break;
            case 2:
                gotMessageActionItemPicked(message);
                break;
        }
    }

    public static ActionsModule get(PebbleTalkerService service)
    {
        return (ActionsModule) service.getModule(MODULE_ACTIONS);
    }
}
