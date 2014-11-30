package com.matejdro.pebblenotificationcenter.pebble;

import android.content.Context;
import android.util.SparseArray;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.DataReceiver;
import com.matejdro.pebblenotificationcenter.pebble.modules.CommModule;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import timber.log.Timber;

/**
 * Created by Matej on 21.11.2014.
 */
public class PebbleCommunication
{
    private Context context;

    private Deque<CommModule> queuedModules;
    private int lastSentPacket = 0;
    private boolean commBusy;

    public PebbleCommunication(Context context)
    {
        this.context = context;
        queuedModules = new LinkedList<CommModule>();
        commBusy = true;
    }

    public void sendToPebble(PebbleDictionary packet)
    {
        Timber.d("SENT " + lastSentPacket);
        PebbleKit.sendDataToPebbleWithTransactionId(context, DataReceiver.pebbleAppUUID, packet, lastSentPacket);

        lastSentPacket = (lastSentPacket + 1) % 255;
        commBusy = true;
    }

    public void sendNext()
    {
        if (!commBusy)
            return;

        while (queuedModules.size() > 0)
        {
            CommModule nextModule = queuedModules.peek();

            if (nextModule.sendNextMessage())
                break;

            queuedModules.removeFirst();
        }
    }

    public void receivedAck(int transactionId)
    {
        System.out.println("ACK " + transactionId);

        if (transactionId != lastSentPacket)
        {
            Timber.w("Got invalid ACK");
            return;
        }

        commBusy = false;
        sendNext();
    }

    public void receivedNack(int transactionId)
    {
        System.out.println("NACK " + transactionId);

        commBusy = false;
        //TODO better nack handling. At the moment NACK usually just means that app on Pebble closed which means I should stop spamming it with messages.
    }

    public void queueModule(CommModule module)
    {
        queuedModules.add(module);
    }

    /*
        Some modules need priority, for example if user pressed SELECT before I finished sending notification, I should prioritize this request over sending rest of the message.
     */
    public void queueModulePriority(CommModule module)
    {
        queuedModules.addFirst(module);
    }
}
