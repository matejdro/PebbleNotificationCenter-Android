package com.matejdro.pebblenotificationcenter.pebble;

/**
 * Created by Matej on 21.11.2014.
 */
public interface DeliveryListener
{
    /*
        @return Should I keep comm line free for sender of this packet to send something new
     */
    public boolean packetDelivered();
}
