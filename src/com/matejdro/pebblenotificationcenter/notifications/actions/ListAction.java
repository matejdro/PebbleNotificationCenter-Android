package com.matejdro.pebblenotificationcenter.notifications.actions;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.DataReceiver;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.util.TextUtil;

/**
 * Action that displays list of strings when activated (used for canned responses)
 */
public abstract class ListAction extends NotificationAction
{
    public ListAction(String actionText)
    {
        super(actionText);
    }

    public void handlePacket(PebbleTalkerService service, int id, PebbleDictionary dictionary)
    {
        if (id == 13)
        {
            int notificationId = dictionary.getInteger(1).intValue();
            int requestedIndex = dictionary.getUnsignedInteger(2).intValue();

            sendItems(service, notificationId, requestedIndex);

        }
        else if (id == 14)
        {
            int pickedIndex = dictionary.getUnsignedInteger(2).intValue();
            service.commWentIdle();
            itemPicked(service, pickedIndex);
        }
    }

    private void sendItems(PebbleTalkerService service, int notificationId, int start)
    {
        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 6);
        data.addInt32(1, notificationId);

        int totalSize = Math.min(getNumberOfItems(), 20);
        int segmentSize = Math.min(totalSize - start, 4);

        data.addUint8(2, (byte) totalSize);
        data.addUint8(3, (byte) start);

        byte[] textData = new byte[segmentSize * 19];

        for (int i = 0; i < segmentSize; i++)
        {
            String text = TextUtil.prepareString(getItem(i + start), 18);
            System.arraycopy(text.getBytes(), 0, textData, i * 19, text.length());
            textData[19 * (i + 1) -1 ] = 0;
        }

        data.addBytes(4, textData);

        PebbleKit.sendDataToPebble(service, DataReceiver.pebbleAppUUID, data);
        service.commStarted();
    }

    @Override
    public void executeAction(PebbleTalkerService service, ProcessedNotification notification)
    {
        sendItems(service, notification.id, 0);
    }

    public abstract int getNumberOfItems();
    public abstract String getItem(int id);
    public abstract void itemPicked(PebbleTalkerService service, int id);

}
