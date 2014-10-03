package com.matejdro.pebblenotificationcenter.lists.actions;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.DataReceiver;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.util.TextUtil;

public abstract class ActionList
{
    public void handlePacket(PebbleTalkerService service, int id, PebbleDictionary dictionary)
    {
        if (id == 13)
        {
            if (!dictionary.contains(2))
            {
                service.commWentIdle();
                return;
            }

            int requestedIndex = dictionary.getInteger(2).intValue();
            sendItems(service, requestedIndex);

        }
        else if (id == 3)
        {
            int pickedIndex = dictionary.getUnsignedInteger(2).intValue();
            service.commWentIdle();
            itemPicked(service, pickedIndex);
        }
    }

    private void sendItems(PebbleTalkerService service, int start)
    {
        PebbleDictionary data = new PebbleDictionary();

        data.addUint8(0, (byte) 5);

        int totalSize = Math.min(getNumberOfItems(), 20);
        int segmentSize = Math.min(totalSize - start, 4);

        data.addUint8(1, (byte) totalSize);
        data.addUint8(2, (byte) start);

        byte[] textData = new byte[segmentSize * 19];

        for (int i = 0; i < segmentSize; i++)
        {
            String text = TextUtil.prepareString(getItem(i + start), 18);
            System.arraycopy(text.getBytes(), 0, textData, i * 19, text.getBytes().length);

            textData[19 * (i + 1) -1 ] = 0;
        }

        data.addBytes(3, textData);

        PebbleKit.sendDataToPebble(service, DataReceiver.pebbleAppUUID, data);
        service.commStarted();
    }

    public void showList(PebbleTalkerService service, ProcessedNotification notification)
    {
        sendItems(service, 0);
    }

    public abstract int getNumberOfItems();
    public abstract String getItem(int id);
    public abstract void itemPicked(PebbleTalkerService service, int id);

}
