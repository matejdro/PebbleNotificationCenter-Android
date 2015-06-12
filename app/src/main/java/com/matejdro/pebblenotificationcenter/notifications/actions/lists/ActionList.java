package com.matejdro.pebblenotificationcenter.notifications.actions.lists;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.NCTalkerService;
import com.matejdro.pebblenotificationcenter.ProcessedNotification;
import com.matejdro.pebblecommons.util.TextUtil;

public abstract class ActionList
{
    public abstract int getNumberOfItems();
    public abstract String getItem(int id);

    /*
        @return true if action executed when item is picked definitely sent something towards Pebble
     */
    public abstract boolean itemPicked(NCTalkerService service, int id);


    public boolean isTertiaryTextList()
    {
        return false;
    }
}
