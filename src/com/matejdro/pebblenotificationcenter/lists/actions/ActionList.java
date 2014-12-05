package com.matejdro.pebblenotificationcenter.lists.actions;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.DataReceiver;
import com.matejdro.pebblenotificationcenter.PebbleTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblenotificationcenter.util.TextUtil;

public abstract class ActionList
{
    public abstract int getNumberOfItems();
    public abstract String getItem(int id);

    /*
        @return true if action executed when item is picked definitely sent something towards Pebble
     */
    public abstract boolean itemPicked(PebbleTalkerService service, int id);

}
