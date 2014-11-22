package com.matejdro.pebblenotificationcenter.pebble;

import android.content.Context;
import android.util.SparseArray;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.DataReceiver;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Matej on 21.11.2014.
 */
public class PebbleCommunication
{
    private Context context;
    private static int nextPacketId = 0;

    private boolean commBusy = false;
    private Queue<PebblePacket> packetQueue = new LinkedList<PebblePacket>();
    private SparseArray<PebblePacket> sentPackets = new SparseArray<PebblePacket>();

    public PebbleCommunication(Context context)
    {
        this.context = context;
    }

    private void sendToPebble(PebblePacket packet)
    {
        commBusy = true;
        System.out.println("sent " + nextPacketId);

        sentPackets.put(nextPacketId, packet);
        PebbleKit.sendDataToPebbleWithTransactionId(context, DataReceiver.pebbleAppUUID, packet.getPebbleDictionary(), nextPacketId);


        nextPacketId = (nextPacketId + 1) % 255;
    }

    public void sendPacket(PebblePacket packet)
    {
        System.out.println("trysend " + nextPacketId + " (busy: " + commBusy + ")");
        if (commBusy)
            packetQueue.add(packet);
        else
            sendToPebble(packet);

    }

    public void sendPacket(PebbleDictionary dictionary)
    {
        sendPacket(new PebblePacket(dictionary));
    }

    public void receivedAck(int transactionId)
    {
        System.out.println("ACK! " + transactionId);
        boolean sendNext = true;

        commBusy = false;

        PebblePacket associatedPacket = sentPackets.get(transactionId);
        if (associatedPacket != null)
            sendNext = !associatedPacket.delivered();

        if (sendNext)
        {
            PebblePacket nextPacket = packetQueue.poll();
            if (nextPacket != null)
                sendToPebble(nextPacket);
        }

    }
}
