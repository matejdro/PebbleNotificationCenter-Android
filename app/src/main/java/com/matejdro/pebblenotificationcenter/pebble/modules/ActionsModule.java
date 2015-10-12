package com.matejdro.pebblenotificationcenter.pebble.modules;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblecommons.pebble.CommModule;
import com.matejdro.pebblecommons.pebble.PebbleCommunication;
import com.matejdro.pebblecommons.pebble.PebbleTalkerService;
import com.matejdro.pebblecommons.util.TextUtil;
import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.appsetting.AppSetting;
import com.matejdro.pebblenotificationcenter.appsetting.AppSettingStorage;
import com.matejdro.pebblenotificationcenter.notifications.actions.DismissOnPhoneAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.NotificationAction;
import com.matejdro.pebblenotificationcenter.notifications.actions.lists.ActionList;
import com.matejdro.pebblenotificationcenter.notifications.actions.lists.NotificationActionList;
import com.matejdro.pebblenotificationcenter.notifications.actions.lists.WritingPhrasesList;

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
        Timber.d("Sending action list items");
        PebbleDictionary data = new PebbleDictionary();

        byte[] bytes = new byte[3];

        data.addUint8(0, (byte) 4);
        data.addUint8(1, (byte) 0);

        int segmentSize = Math.min(listSize - nextListItemToSend, 4);

        bytes[0] = (byte) nextListItemToSend;
        bytes[1] = (byte) listSize;
        bytes[2] = (byte) (list.isTertiaryTextList() ? 1 : 0);

        byte[] textData = new byte[segmentSize * 19];

        for (int i = 0; i < segmentSize; i++)
        {
            String text = TextUtil.prepareString(list.getItem(i + nextListItemToSend), 18);
            System.arraycopy(text.getBytes(), 0, textData, i * 19, text.getBytes().length);

            textData[19 * (i + 1) -1 ] = 0;
        }

        data.addBytes(2, bytes);
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
        int type = data.getUnsignedIntegerAsLong(3).intValue();

        Timber.d("Button action from Pebble, Type: %d", type);

        ProcessedNotification notification = NCTalkerService.fromPebbleTalkerService(getService()).sentNotifications.get(id);
        if (notification == null)
        {
            Timber.d("Invalid notification %d", id);
            SystemModule.get(getService()).hideHourglass();
            return;
        }

        if (notification.source.getActions() == null || notification.source.getActions().size() == 0)
        {
            DismissOnPhoneAction.dismissOnPhone(notification, NCTalkerService.fromPebbleTalkerService(getService()));
            return;
        }

        AppSetting relevantSetting;
        if (type == 0)
            relevantSetting = AppSetting.SELECT_PRESS_ACTION;
        else if (type == 1)
            relevantSetting = AppSetting.SELECT_HOLD_ACTION;
        else
            relevantSetting = AppSetting.SHAKE_ACTION;

        AppSettingStorage settingStorage = notification.source.getSettingStorage(getService());

        int action = settingStorage.getInt(relevantSetting);

        if (notification.source.shouldForceActionMenu() || action == 2)
        {
            this.notification = notification;
            showList(new NotificationActionList(notification));
        }
        else if (action == 0 || action == 60 || action == 61) //Do nothing on disabled and stop periodic vibration actions
        {
            Timber.d("Action was set to none");
            SystemModule.get(getService()).hideHourglass();
            return;
        }
        else if (action == 1)
        {
            DismissOnPhoneAction.dismissOnPhone(notification, NCTalkerService.fromPebbleTalkerService(getService()));
        }
        else if (action == 62)
        {
            DismissUpwardsModule.dismissPebbleID(getService(), notification.id);
        }
        else
        {
            action -= 3;
            if (notification.source.getActions().size() <= action)
                return;

            SystemModule.get(getService()).hideHourglass();
            notification.source.getActions().get(action).executeAction(NCTalkerService.fromPebbleTalkerService(getService()), notification);
        }
    }

    public void gotMessageActionItemPicked(PebbleDictionary message)
    {
        ProcessedNotification notification = NCTalkerService.fromPebbleTalkerService(getService()).sentNotifications.get(this.notification.id);
        if (notification == null)
        {
            Timber.d("Invalid notification %d", notification.id);
            SystemModule.get(getService()).hideHourglass();
            return;
        }

        int action = message.getUnsignedIntegerAsLong(2).intValue();
        Timber.d("Action picked message %d", action);

        if (list == null || action >= list.getNumberOfItems())
        {
            Timber.w("Action is higher than list has items!");
            SystemModule.get(getService()).hideHourglass();
            return;
        }



        if (!list.itemPicked(NCTalkerService.fromPebbleTalkerService(getService()), action))
            SystemModule.get(getService()).hideHourglass();
    }

    private void gotMessageDismissNotification(PebbleDictionary data)
    {
        int id = data.getInteger(2).intValue();

        Timber.d("Got dismiss request from Pebble");

        ProcessedNotification notification = NCTalkerService.fromPebbleTalkerService(getService()).sentNotifications.get(id);
        if (notification == null)
        {
            Timber.d("Invalid notification!");
            SystemModule.get(getService()).hideHourglass();
            return;
        }

        DismissOnPhoneAction.dismissOnPhone(notification, NCTalkerService.fromPebbleTalkerService(getService()));
    }

    private void gotMessageReplyText(PebbleDictionary data)
    {
        if (list == null || !list.isTertiaryTextList())
            return;

        String text = data.getString(2);
        ((WritingPhrasesList) list).reply(text);

        SystemModule.get(getService()).hideHourglass();
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
            case 3:
                gotMessageReplyText(message);
                break;
        }
    }

    public static ActionsModule get(PebbleTalkerService service)
    {
        return (ActionsModule) service.getModule(MODULE_ACTIONS);
    }
}
