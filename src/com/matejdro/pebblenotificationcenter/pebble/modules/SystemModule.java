package com.matejdro.pebblenotificationcenter.pebble.modules;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import java.util.concurrent.Callable;

/**
 * Created by Matej on 29.11.2014.
 */
public class SystemModule extends CommModule
{
    private Callable<Boolean> runOnNext;

    public SystemModule(PebbleTalkerService service)
    {
        super(service);
        runOnNext = null;
    }

    @Override
    public boolean sendNextMessage()
    {
        if (runOnNext == null)
            return false;

        try
        {
            return runOnNext.call();
        } catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void gotMessageFromPebble(PebbleDictionary message)
    {
        long id = message.getUnsignedIntegerAsLong(1);

        switch (id)
        {
            case 0: //Pebble opened
                break;
            case 1: //Menu entry picked
                break;
            case 2: //Config change
                break;
            case 3: //Close me
                break;


        }
    }
}
