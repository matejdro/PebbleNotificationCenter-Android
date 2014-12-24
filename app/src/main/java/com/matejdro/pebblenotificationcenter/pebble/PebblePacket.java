package com.matejdro.pebblenotificationcenter.pebble;

import com.getpebble.android.kit.util.PebbleDictionary;

/**
 * Created by Matej on 21.11.2014.
 */
public class PebblePacket
{
    private PebbleDictionary dictionary;
    private DeliveryListener listener;

    public PebblePacket(PebbleDictionary dictionary)
    {
        this.dictionary = dictionary;
    }

    public PebblePacket(PebbleDictionary dictionary, DeliveryListener listener)
    {
        this.dictionary = dictionary;
        this.listener = listener;
    }

    /*
        @return Should I keep comm line free for sender of this packet to send something new
     */
    public boolean delivered()
    {
        if (listener != null)
            return listener.packetDelivered();

        return false;
    }

    public PebbleDictionary getPebbleDictionary()
    {
        return dictionary;
    }
}
