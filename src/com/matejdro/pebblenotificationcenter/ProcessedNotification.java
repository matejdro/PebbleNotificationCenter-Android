package com.matejdro.pebblenotificationcenter;

import com.matejdro.pebblenotificationcenter.lists.actions.ActionList;
import java.util.ArrayList;
import java.util.List;

public class ProcessedNotification
{
	public int id;
	public List<String> textChunks = new ArrayList<String>(13);
    public ActionList activeActionList;
    public boolean vibrated = false;
    public boolean sent = false;

    public PebbleNotification source;
}
