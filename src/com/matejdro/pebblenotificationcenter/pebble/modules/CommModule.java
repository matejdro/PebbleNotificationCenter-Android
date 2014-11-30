package com.matejdro.pebblenotificationcenter.pebble.modules;

import android.content.Intent;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;

/**
 * Created by Matej on 28.11.2014.
 */
public abstract class CommModule
{
    private PebbleTalkerService service;

    public CommModule(PebbleTalkerService service)
    {
        this.service = service;
    }

    public PebbleTalkerService getService()
    {
        return service;
    }

    /*
        @return true if module sent something, false if module is done with sending
     */
    public abstract boolean sendNextMessage();

    /*
        Activated when received new message from Pebble marked with this module
     */
    public abstract void gotMessageFromPebble(PebbleDictionary message);

    /*
        Intent associated with this module got delivered to service
     */
    public void gotIntent(Intent intent)
    {

    }

    /*
        Called when Pebble app opened. This means Pebble app has lost all its data (for example there is no need to dismiss any left notifications from Pebble anymore).
     */
    public void pebbleAppOpened()
    {

    }
}
